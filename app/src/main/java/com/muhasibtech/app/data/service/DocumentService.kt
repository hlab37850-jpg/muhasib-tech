package com.muhasibtech.app.data.service

import androidx.room.withTransaction
import com.muhasibtech.app.data.local.AppDatabase
import com.muhasibtech.app.data.local.entity.DocumentEntity
import com.muhasibtech.app.data.local.entity.DocumentLineEntity
import com.muhasibtech.app.data.local.entity.JournalLineEntity
import com.muhasibtech.app.data.local.entity.StockBalanceEntity
import com.muhasibtech.app.data.model.AdjustmentType
import com.muhasibtech.app.data.model.DocType
import com.muhasibtech.app.data.model.DiscountType
import com.muhasibtech.app.data.model.DocumentRequest
import com.muhasibtech.app.data.model.SettingKeys
import com.muhasibtech.app.data.repository.AppRepository
import com.muhasibtech.app.util.AppDate
import kotlin.math.abs

class DocumentService(
    private val db: AppDatabase,
    private val repo: AppRepository
) {

    suspend fun saveDocument(request: DocumentRequest): Long {
        DocumentRequestValidator.validate(request)

        return db.withTransaction {
            val date = if (request.date.isBlank()) AppDate.today() else request.date
            val warehouseId = if (request.warehouseId == 0L) {
                repo.settings.getCurrentWarehouseId()
            } else {
                request.warehouseId
            }

            val targetWarehouseId = request.targetWarehouseId
            val currencyId = if (request.currencyId == 0L) {
                repo.settings.getCurrentCurrencyId()
            } else {
                request.currencyId
            }

            val cashboxId = if (request.cashboxId == 0L) {
                repo.cashboxes.getDefault()?.id ?: 0L
            } else {
                request.cashboxId
            }

            val existing = if (request.documentId > 0) {
                repo.documents.getById(request.documentId)
            } else {
                null
            }

            if (existing != null && existing.isClosed) {
                throw IllegalStateException("المستند مقفل ولا يمكن تعديله")
            }

            if (existing != null) {
                val oldLines = repo.documentLines.getByDocument(existing.id)
                reverseInventory(existing, oldLines)
                repo.documentLines.deleteByDocument(existing.id)
                repo.journalLines.deleteByDocument(existing.id)
            }

            val docNumber = existing?.docNumber ?: (repo.documents.getLastDocNumber(request.docType) + 1)

            val lineEntities = mutableListOf<DocumentLineEntity>()
            var totalAmount = 0.0

            request.lines.forEachIndexed { index, line ->
                val total = if (line.total != 0.0) line.total else line.qty * line.price

                if (isItemDocument(request.docType)) {
                    totalAmount += total
                }

                lineEntities.add(
                    DocumentLineEntity(
                        documentId = 0,
                        itemId = line.itemId,
                        accountId = line.accountId,
                        qty = line.qty,
                        price = line.price,
                        total = total,
                        debit = line.debit,
                        credit = line.credit,
                        notes = line.notes,
                        sortOrder = index
                    )
                )
            }

            if (request.docType == DocType.RECEIPT) {
                totalAmount = lineEntities.sumOf { if (it.credit != 0.0) it.credit else it.total }
            }

            if (request.docType == DocType.PAYMENT) {
                totalAmount = lineEntities.sumOf { if (it.debit != 0.0) it.debit else it.total }
            }

            if (request.docType == DocType.JOURNAL_DAILY || request.docType == DocType.JOURNAL_OPENING) {
                val debit = lineEntities.sumOf { it.debit }
                val credit = lineEntities.sumOf { it.credit }
                if (abs(debit - credit) > 0.001) {
                    throw IllegalStateException("القيد غير متوازن")
                }
                totalAmount = debit
            }

            val discountAmount = if (isItemDocument(request.docType)) {
                if (request.discountType == DiscountType.PERCENT) {
                    totalAmount * request.discountValue / 100.0
                } else {
                    request.discountValue
                }
            } else {
                0.0
            }

            if (isItemDocument(request.docType) && discountAmount - totalAmount > 0.001) {
                throw IllegalStateException("لا يمكن أن يتجاوز الخصم إجمالي المستند")
            }

            val taxAmount = if (isItemDocument(request.docType) && request.taxRate > 0.0) {
                (totalAmount - discountAmount) * request.taxRate / 100.0
            } else {
                0.0
            }

            val netAmount = if (isItemDocument(request.docType)) {
                totalAmount - discountAmount + taxAmount + request.feesAmount
            } else {
                totalAmount
            }

            val isCash = request.docType == DocType.SALE_CASH ||
                request.docType == DocType.PURCHASE_CASH ||
                request.docType == DocType.RECEIPT ||
                request.docType == DocType.PAYMENT

            val paidAmount = if (isCash && request.paidAmount == 0.0) {
                netAmount
            } else {
                request.paidAmount
            }

            if ((request.docType == DocType.SALE_CASH ||
                    request.docType == DocType.PURCHASE_CASH) &&
                paidAmount - netAmount > 0.001
            ) {
                throw IllegalStateException("المبلغ المدفوع نقدًا لا يجوز أن يتجاوز صافي المستند")
            }
            if ((request.docType == DocType.SALE_CASH ||
                    request.docType == DocType.PURCHASE_CASH) &&
                kotlin.math.abs(paidAmount - netAmount) > 0.001
            ) {
                throw IllegalStateException("المستند النقدي يجب أن يُسدد بالكامل؛ استخدم نوع الآجل للدفعات الجزئية")
            }

            val isCreditDocument = request.docType == DocType.SALE_CREDIT ||
                request.docType == DocType.PURCHASE_CREDIT
            if (isCreditDocument && (paidAmount < 0.0 || paidAmount - netAmount > 0.001)) {
                throw IllegalStateException("المبلغ المدفوع لا يجوز أن يتجاوز صافي المستند")
            }
            val remainingAmount = if (isCreditDocument) {
                (netAmount - paidAmount).coerceAtLeast(0.0)
            } else {
                0.0
            }

            val signedInventoryValue = applyInventory(request, warehouseId, targetWarehouseId, lineEntities)

            val document = DocumentEntity(
                id = existing?.id ?: 0,
                docType = request.docType,
                docNumber = docNumber,
                date = date,
                reference = request.reference,
                partyAccountId = request.partyAccountId,
                warehouseId = warehouseId,
                targetWarehouseId = targetWarehouseId,
                currencyId = currencyId,
                cashboxId = cashboxId,
                currencyRate = request.currencyRate,
                isCredit = request.isCredit,
                settlementType = request.settlementType,
                notes = request.notes,
                discountType = request.discountType,
                discountValue = request.discountValue,
                discountAmount = discountAmount,
                taxRate = request.taxRate,
                taxAmount = taxAmount,
                feesAmount = request.feesAmount,
                feesNote = request.feesNote,
                paidAmount = paidAmount,
                totalAmount = totalAmount,
                netAmount = netAmount,
                remainingAmount = remainingAmount,
                costTotal = signedInventoryValue,
                isClosed = existing?.isClosed ?: false,
                createdBy = existing?.createdBy ?: 0,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val docId = if (existing == null) {
                repo.documents.insert(document)
            } else {
                repo.documents.update(document)
                document.id
            }

            val savedDocument = document.copy(id = docId)
            val linesToInsert = lineEntities.map { it.copy(documentId = docId) }

            repo.documentLines.insertAll(linesToInsert)
            postJournal(savedDocument, linesToInsert)

            docId
        }
    }

    suspend fun deleteDocument(documentId: Long) {
        db.withTransaction {
            val document = repo.documents.getById(documentId)
                ?: throw IllegalStateException("المستند غير موجود")

            if (document.isClosed) {
                throw IllegalStateException("المستند مقفل ولا يمكن حذفه")
            }

            val oldLines = repo.documentLines.getByDocument(document.id)

            reverseInventory(document, oldLines)

            repo.documentLines.deleteByDocument(document.id)
            repo.journalLines.deleteByDocument(document.id)
            repo.documents.delete(document)
        }
    }

    private fun isItemDocument(docType: String): Boolean {
        return docType == DocType.SALE_CASH ||
            docType == DocType.SALE_CREDIT ||
            docType == DocType.PURCHASE_CASH ||
            docType == DocType.PURCHASE_CREDIT ||
            docType == DocType.QUOTATION ||
            docType == DocType.PURCHASE_REQUEST
    }

    private fun isStockDocument(docType: String): Boolean {
        return docType == DocType.SALE_CASH ||
            docType == DocType.SALE_CREDIT ||
            docType == DocType.PURCHASE_CASH ||
            docType == DocType.PURCHASE_CREDIT ||
            docType == DocType.STOCK_OUT ||
            docType == DocType.STOCK_IN ||
            docType == DocType.STOCK_TRANSFER ||
            docType == DocType.STOCK_ADJUSTMENT ||
            docType == DocType.STOCKTAKE
    }

    private suspend fun applyInventory(
        request: DocumentRequest,
        warehouseId: Long,
        targetWarehouseId: Long,
        lines: MutableList<DocumentLineEntity>
    ): Double {
        if (!isStockDocument(request.docType)) return 0.0

        var signedValue = 0.0

        for (i in lines.indices) {
            val line = lines[i]

            if (line.itemId == 0L) continue

            when (request.docType) {

                DocType.SALE_CASH, DocType.SALE_CREDIT -> {
                    if (line.qty == 0.0 || warehouseId == 0L) continue

                    val balance = repo.stockBalances.getOnce(line.itemId, warehouseId)
                    val oldQty = balance?.qty ?: 0.0
                    val cost = if ((balance?.averageCost ?: 0.0) > 0.0) {
                        balance!!.averageCost
                    } else {
                        line.price
                    }

                    lines[i] = line.copy(cost = cost, previousQty = oldQty)
                    changeStock(line.itemId, warehouseId, -abs(line.qty), cost, false)
                    signedValue -= cost * abs(line.qty)
                }

                DocType.PURCHASE_CASH, DocType.PURCHASE_CREDIT -> {
                    if (line.qty == 0.0 || warehouseId == 0L) continue

                    val balance = repo.stockBalances.getOnce(line.itemId, warehouseId)
                    val oldQty = balance?.qty ?: 0.0
                    val cost = if (line.price > 0.0) line.price else (balance?.averageCost ?: 0.0)

                    lines[i] = line.copy(cost = cost, previousQty = oldQty)
                    changeStock(line.itemId, warehouseId, abs(line.qty), cost, true)
                    signedValue += cost * abs(line.qty)
                }

                DocType.STOCK_OUT -> {
                    if (line.qty == 0.0 || warehouseId == 0L) continue

                    val balance = repo.stockBalances.getOnce(line.itemId, warehouseId)
                    val oldQty = balance?.qty ?: 0.0
                    val cost = if ((balance?.averageCost ?: 0.0) > 0.0) {
                        balance!!.averageCost
                    } else {
                        line.price
                    }

                    lines[i] = line.copy(cost = cost, previousQty = oldQty)
                    changeStock(line.itemId, warehouseId, -abs(line.qty), cost, false)
                    signedValue -= cost * abs(line.qty)
                }

                DocType.STOCK_IN -> {
                    if (line.qty == 0.0 || warehouseId == 0L) continue

                    val balance = repo.stockBalances.getOnce(line.itemId, warehouseId)
                    val oldQty = balance?.qty ?: 0.0
                    val cost = if (line.price > 0.0) line.price else (balance?.averageCost ?: 0.0)

                    lines[i] = line.copy(cost = cost, previousQty = oldQty)
                    changeStock(line.itemId, warehouseId, abs(line.qty), cost, true)
                    signedValue += cost * abs(line.qty)
                }

                DocType.STOCK_TRANSFER -> {
                    if (line.qty == 0.0 || warehouseId == 0L || targetWarehouseId == 0L) continue

                    val sourceBalance = repo.stockBalances.getOnce(line.itemId, warehouseId)
                    val oldQty = sourceBalance?.qty ?: 0.0
                    val cost = if ((sourceBalance?.averageCost ?: 0.0) > 0.0) {
                        sourceBalance!!.averageCost
                    } else {
                        line.price
                    }

                    lines[i] = line.copy(cost = cost, previousQty = oldQty)

                    changeStock(line.itemId, warehouseId, -abs(line.qty), cost, false)
                    changeStock(line.itemId, targetWarehouseId, abs(line.qty), cost, true)
                }

                DocType.STOCK_ADJUSTMENT -> {
                    if (warehouseId == 0L) continue

                    val balance = repo.stockBalances.getOnce(line.itemId, warehouseId)
                    val oldQty = balance?.qty ?: 0.0
                    val cost = if (line.price > 0.0) line.price else (balance?.averageCost ?: 0.0)

                    lines[i] = line.copy(cost = cost, previousQty = oldQty)

                    when (request.settlementType) {

                        AdjustmentType.OPENING -> {
                            val delta = line.qty - oldQty
                            if (delta != 0.0) {
                                changeStock(line.itemId, warehouseId, delta, cost, true)
                                signedValue += delta * cost
                            }
                        }

                        AdjustmentType.INCREASE -> {
                            val qty = abs(line.qty)
                            if (qty != 0.0) {
                                changeStock(line.itemId, warehouseId, qty, cost, true)
                                signedValue += qty * cost
                            }
                        }

                        AdjustmentType.DAMAGED, AdjustmentType.SHORTAGE -> {
                            val qty = abs(line.qty)
                            if (qty != 0.0) {
                                changeStock(line.itemId, warehouseId, -qty, cost, false)
                                signedValue -= qty * cost
                            }
                        }

                        else -> {
                            val delta = line.qty
                            if (delta != 0.0) {
                                changeStock(line.itemId, warehouseId, delta, cost, delta > 0)
                                signedValue += delta * cost
                            }
                        }
                    }
                }

                DocType.STOCKTAKE -> {
                    if (warehouseId == 0L) continue

                    val balance = repo.stockBalances.getOnce(line.itemId, warehouseId)
                    val oldQty = balance?.qty ?: 0.0
                    val cost = if (line.price > 0.0) line.price else (balance?.averageCost ?: 0.0)
                    val delta = line.qty - oldQty

                    lines[i] = line.copy(cost = cost, previousQty = oldQty)

                    if (delta != 0.0) {
                        changeStock(line.itemId, warehouseId, delta, cost, delta > 0)
                        signedValue += delta * cost
                    }
                }
            }
        }

        return signedValue
    }

    private suspend fun reverseInventory(
        document: DocumentEntity,
        lines: List<DocumentLineEntity>
    ) {
        for (line in lines) {
            if (line.itemId == 0L) continue

            when (document.docType) {

                DocType.SALE_CASH, DocType.SALE_CREDIT -> {
                    val cost = if (line.cost != 0.0) line.cost else line.price
                    changeStock(line.itemId, document.warehouseId, abs(line.qty), cost, true)
                }

                DocType.PURCHASE_CASH, DocType.PURCHASE_CREDIT -> {
                    reversePurchaseStock(
                        itemId = line.itemId,
                        warehouseId = document.warehouseId,
                        qty = abs(line.qty),
                        purchasePrice = line.price
                    )
                }

                DocType.STOCK_OUT -> {
                    val cost = if (line.cost != 0.0) line.cost else line.price
                    changeStock(line.itemId, document.warehouseId, abs(line.qty), cost, false)
                }

                DocType.STOCK_IN -> {
                    val cost = if (line.cost != 0.0) line.cost else line.price
                    changeStock(line.itemId, document.warehouseId, -abs(line.qty), cost, false)
                }

                DocType.STOCK_TRANSFER -> {
                    val cost = if (line.cost != 0.0) line.cost else line.price

                    if (document.targetWarehouseId != 0L) {
                        changeStock(line.itemId, document.targetWarehouseId, -abs(line.qty), cost, false)
                    }

                    if (document.warehouseId != 0L) {
                        changeStock(line.itemId, document.warehouseId, abs(line.qty), cost, true)
                    }
                }

                DocType.STOCK_ADJUSTMENT, DocType.STOCKTAKE -> {
                    setQty(line.itemId, document.warehouseId, line.previousQty)
                }
            }
        }
    }

    private suspend fun changeStock(
        itemId: Long,
        warehouseId: Long,
        deltaQty: Double,
        unitCost: Double,
        updateAverage: Boolean
    ) {
        if (itemId == 0L || warehouseId == 0L || deltaQty == 0.0) return

        val balance = repo.stockBalances.getOnce(itemId, warehouseId)
        val oldQty = balance?.qty ?: 0.0
        val oldCost = balance?.averageCost ?: 0.0
        val newQty = oldQty + deltaQty

        var newCost = oldCost

        if (updateAverage && newQty > 0.0) {
            if (deltaQty > 0.0) {
                newCost = ((oldQty * oldCost) + (deltaQty * unitCost)) / newQty
            }
        } else if (balance == null && newQty > 0.0) {
            newCost = if (unitCost > 0.0) unitCost else 0.0
        }

        if (balance == null) {
            repo.stockBalances.insert(
                StockBalanceEntity(
                    itemId = itemId,
                    warehouseId = warehouseId,
                    qty = newQty,
                    averageCost = newCost
                )
            )
        } else {
            repo.stockBalances.updateBalance(
                itemId = itemId,
                warehouseId = warehouseId,
                qty = newQty,
                averageCost = newCost,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    private suspend fun reversePurchaseStock(
        itemId: Long,
        warehouseId: Long,
        qty: Double,
        purchasePrice: Double
    ) {
        if (itemId == 0L || warehouseId == 0L || qty == 0.0) return

        val balance = repo.stockBalances.getOnce(itemId, warehouseId) ?: return

        val oldQty = balance.qty
        val oldCost = balance.averageCost
        val newQty = oldQty - qty

        var newCost = 0.0

        if (newQty > 0.0) {
            newCost = ((oldQty * oldCost) - (qty * purchasePrice)) / newQty
            if (newCost < 0.0) newCost = 0.0
        }

        repo.stockBalances.updateBalance(
            itemId = itemId,
            warehouseId = warehouseId,
            qty = newQty,
            averageCost = newCost,
            updatedAt = System.currentTimeMillis()
        )
    }

    private suspend fun setQty(
        itemId: Long,
        warehouseId: Long,
        qty: Double
    ) {
        if (itemId == 0L || warehouseId == 0L) return

        val balance = repo.stockBalances.getOnce(itemId, warehouseId)

        if (balance == null) {
            repo.stockBalances.insert(
                StockBalanceEntity(
                    itemId = itemId,
                    warehouseId = warehouseId,
                    qty = qty,
                    averageCost = 0.0
                )
            )
        } else {
            repo.stockBalances.updateBalance(
                itemId = itemId,
                warehouseId = warehouseId,
                qty = qty,
                averageCost = balance.averageCost,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    private suspend fun postJournal(
        doc: DocumentEntity,
        lines: List<DocumentLineEntity>
    ) {
        val journalLines = mutableListOf<JournalLineEntity>()

        fun add(accountId: Long, debit: Double, credit: Double, details: String) {
            if (accountId == 0L) return
            if (abs(debit) <= 0.001 && abs(credit) <= 0.001) return

            journalLines.add(
                JournalLineEntity(
                    documentId = doc.id,
                    date = doc.date,
                    accountId = accountId,
                    currencyId = doc.currencyId,
                    rate = doc.currencyRate,
                    debit = debit,
                    credit = credit,
                    details = details
                )
            )
        }

        val cashbox = if (doc.cashboxId > 0) {
            repo.cashboxes.getById(doc.cashboxId)
        } else {
            repo.cashboxes.getDefault()
        }

        val cashAccountId = cashbox?.accountId ?: repo.settings.getLong(SettingKeys.ACCOUNT_CASH_ID, 0L)

        when (doc.docType) {

            DocType.SALE_CASH, DocType.SALE_CREDIT -> {
                val salesAccount = repo.settings.getLong(SettingKeys.ACCOUNT_SALES_ID, 0L)
                val discountAccount = repo.settings.getLong(SettingKeys.ACCOUNT_DISCOUNT_ID, 0L)
                val taxAccount = repo.settings.getLong(SettingKeys.ACCOUNT_TAX_ID, 0L)
                val feesAccount = repo.settings.getLong(SettingKeys.ACCOUNT_FEES_ID, 0L)
                val cogsAccount = repo.settings.getLong(SettingKeys.ACCOUNT_COGS_ID, 0L)
                val inventoryAccount = repo.settings.getLong(SettingKeys.ACCOUNT_INVENTORY_ID, 0L)
                val customersAccount = repo.settings.getLong(SettingKeys.ACCOUNT_CUSTOMERS_ID, 0L)

                if (doc.docType == DocType.SALE_CASH) {
                    add(cashAccountId, doc.netAmount, 0.0, "مبيعات نقدية")
                } else {
                    val customerAccount = if (doc.partyAccountId > 0) doc.partyAccountId else customersAccount
                    add(customerAccount, doc.remainingAmount, 0.0, "رصيد مستحق على العميل")
                    add(cashAccountId, doc.paidAmount, 0.0, "دفعة مقدمة على المبيعات")
                }
                add(discountAccount, doc.discountAmount, 0.0, "الخصم")
                add(salesAccount, 0.0, doc.totalAmount, "المبيعات")
                add(taxAccount, 0.0, doc.taxAmount, "الضريبة")
                add(feesAccount, 0.0, doc.feesAmount, "الرسوم")

                if (doc.costTotal < 0.0) {
                    val cost = abs(doc.costTotal)
                    add(cogsAccount, cost, 0.0, "تكلفة البضاعة المباعة")
                    add(inventoryAccount, 0.0, cost, "إخراج المخزون")
                }
            }

            DocType.PURCHASE_CASH, DocType.PURCHASE_CREDIT -> {
                val inventoryAccount = repo.settings.getLong(SettingKeys.ACCOUNT_INVENTORY_ID, 0L)
                val suppliersAccount = repo.settings.getLong(SettingKeys.ACCOUNT_SUPPLIERS_ID, 0L)

                add(inventoryAccount, doc.netAmount, 0.0, "شراء مخزون")
                if (doc.docType == DocType.PURCHASE_CASH) {
                    add(cashAccountId, 0.0, doc.netAmount, "مشتريات نقدية")
                } else {
                    val supplierAccount = if (doc.partyAccountId > 0) doc.partyAccountId else suppliersAccount
                    add(supplierAccount, 0.0, doc.remainingAmount, "رصيد مستحق للمورد")
                    add(cashAccountId, 0.0, doc.paidAmount, "دفعة مقدمة للمورد")
                }
            }

            DocType.RECEIPT -> {
                add(cashAccountId, doc.totalAmount, 0.0, "قبض نقدي")

                lines.forEach { line ->
                    val amount = if (line.credit != 0.0) line.credit else line.total
                    add(line.accountId, 0.0, amount, line.notes)
                }
            }

            DocType.PAYMENT -> {
                lines.forEach { line ->
                    val amount = if (line.debit != 0.0) line.debit else line.total
                    add(line.accountId, amount, 0.0, line.notes)
                }

                add(cashAccountId, 0.0, doc.totalAmount, "صرف نقدي")
            }

            DocType.JOURNAL_DAILY, DocType.JOURNAL_OPENING -> {
                lines.forEach { line ->
                    add(line.accountId, line.debit, line.credit, line.notes)
                }
            }

            DocType.STOCK_OUT -> {
                val stockLossAccount = repo.settings.getLong(SettingKeys.ACCOUNT_STOCK_LOSS_ID, 0L)
                val inventoryAccount = repo.settings.getLong(SettingKeys.ACCOUNT_INVENTORY_ID, 0L)

                if (doc.costTotal < 0.0) {
                    val amount = abs(doc.costTotal)
                    add(stockLossAccount, amount, 0.0, "صرف مخزني")
                    add(inventoryAccount, 0.0, amount, "إخراج المخزون")
                }
            }

            DocType.STOCK_IN, DocType.STOCK_ADJUSTMENT, DocType.STOCKTAKE -> {
                val inventoryAccount = repo.settings.getLong(SettingKeys.ACCOUNT_INVENTORY_ID, 0L)
                val openingAccount = repo.settings.getLong(SettingKeys.ACCOUNT_OPENING_ID, 0L)
                val otherRevenueAccount = repo.settings.getLong(SettingKeys.ACCOUNT_OTHER_REVENUE_ID, 0L)
                val stockLossAccount = repo.settings.getLong(SettingKeys.ACCOUNT_STOCK_LOSS_ID, 0L)

                if (doc.costTotal > 0.0) {
                    val amount = doc.costTotal
                    val creditAccount = if (doc.settlementType == AdjustmentType.OPENING) {
                        openingAccount
                    } else {
                        otherRevenueAccount
                    }

                    add(inventoryAccount, amount, 0.0, "زيادة مخزون")
                    add(creditAccount, 0.0, amount, "مصدر الزيادة")
                } else if (doc.costTotal < 0.0) {
                    val amount = abs(doc.costTotal)
                    add(stockLossAccount, amount, 0.0, "نقص مخزون")
                    add(inventoryAccount, 0.0, amount, "إخراج المخزون")
                }
            }

            else -> {
                // عروض الأسعار وطلبات الشراء لا تولد قيودًا محاسبية.
            }
        }

        if (journalLines.isNotEmpty()) {
            JournalBalanceValidator.validate(
                totalDebit = journalLines.sumOf { it.debit },
                totalCredit = journalLines.sumOf { it.credit }
            )
            repo.journalLines.insertAll(journalLines)
        }
    }
}
