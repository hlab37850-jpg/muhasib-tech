package com.muhasibtech.app.ui.document

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.muhasibtech.app.MuhasibApp
import com.muhasibtech.app.R
import com.muhasibtech.app.data.local.entity.AccountEntity
import com.muhasibtech.app.data.local.entity.CashboxEntity
import com.muhasibtech.app.data.local.entity.CurrencyEntity
import com.muhasibtech.app.data.local.entity.ItemEntity
import com.muhasibtech.app.data.local.entity.WarehouseEntity
import com.muhasibtech.app.data.model.AdjustmentType
import com.muhasibtech.app.data.model.DocType
import com.muhasibtech.app.data.model.DiscountType
import com.muhasibtech.app.data.model.DocumentRequest
import com.muhasibtech.app.data.repository.AppRepository
import com.muhasibtech.app.databinding.ActivityDocumentEditBinding
import com.muhasibtech.app.util.AppDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class DocumentEditActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_DOC_TYPE = "extra_doc_type"
        private const val EXTRA_DOCUMENT_ID = "extra_document_id"

        fun startAdd(context: Context, docType: String) {
            val intent = Intent(context, DocumentEditActivity::class.java)
            intent.putExtra(EXTRA_DOC_TYPE, docType)
            context.startActivity(intent)
        }

        fun startEdit(context: Context, documentId: Long) {
            val intent = Intent(context, DocumentEditActivity::class.java)
            intent.putExtra(EXTRA_DOCUMENT_ID, documentId)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityDocumentEditBinding
    private lateinit var repo: AppRepository
    private lateinit var lineAdapter: DocumentLineAdapter

    private var docType: String = DocType.JOURNAL_DAILY
    private var documentId: Long = 0

    private var partyAccountId: Long = 0
    private var warehouseId: Long = 0
    private var targetWarehouseId: Long = 0
    private var currencyId: Long = 0
    private var cashboxId: Long = 0

    private var settlementType: String = AdjustmentType.OPENING

    private var discountType: String = DiscountType.AMOUNT
    private var discountValue: Double = 0.0
    private var taxRate: Double = 0.0
    private var feesAmount: Double = 0.0
    private var feesNote: String = ""
    private var paidAmount: Double = 0.0

    private val lines = mutableListOf<LineUi>()

    private var warehouses: List<WarehouseEntity> = emptyList()
    private var currencies: List<CurrencyEntity> = emptyList()
    private var cashboxes: List<CashboxEntity> = emptyList()
    private var accounts: List<AccountEntity> = emptyList()
    private var items: List<ItemEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDocumentEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = (application as MuhasibApp).repository

        docType = intent.getStringExtra(EXTRA_DOC_TYPE) ?: DocType.JOURNAL_DAILY
        documentId = intent.getLongExtra(EXTRA_DOCUMENT_ID, 0)

        binding.toolbar.title = getTitleForDocType(docType)

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_save) {
                saveDocument()
                true
            } else {
                false
            }
        }

        binding.btnAddLine.setOnClickListener {
            addLine()
        }

        binding.tvSummary.setOnClickListener {
            if (isDiscountSupported()) {
                showDiscountDialog()
            }
        }

        setupSelectors()
        loadMasterData()
    }

    private fun setupSelectors() {
        binding.btnParty.setOnClickListener { showPartyDialog() }
        binding.btnWarehouse.setOnClickListener { showWarehouseDialog() }
        binding.btnTargetWarehouse.setOnClickListener { showTargetWarehouseDialog() }
        binding.btnCashbox.setOnClickListener { showCashboxDialog() }
        binding.btnCurrency.setOnClickListener { showCurrencyDialog() }
        binding.btnSettlement.setOnClickListener { showSettlementDialog() }
    }

    private fun loadMasterData() {
        lifecycleScope.launch {
            warehouses = repo.warehouses.observeActive().first()
            currencies = repo.currencies.observeActive().first()
            cashboxes = repo.cashboxes.observeActive().first()
            accounts = repo.accounts.observeActive().first()
            items = repo.items.observeActive().first()

            if (documentId > 0) {
                val loaded = loadDocument()
                if (!loaded) initDefaults()
            } else {
                initDefaults()
            }

            setupLinesAdapter()
            updateVisibility()
            updateSelectors()
            updateTotals()
        }
    }

    private suspend fun initDefaults() {
        warehouseId = repo.settings.getCurrentWarehouseId()
        currencyId = repo.settings.getCurrentCurrencyId()
        cashboxId = repo.cashboxes.getDefault()?.id ?: 0

        binding.etDate.setText(AppDate.today())
    }

    private suspend fun loadDocument(): Boolean {
        val doc = repo.documents.getById(documentId) ?: return false

        docType = doc.docType
        binding.toolbar.title = getTitleForDocType(docType)

        partyAccountId = doc.partyAccountId
        warehouseId = doc.warehouseId
        targetWarehouseId = doc.targetWarehouseId
        currencyId = doc.currencyId
        cashboxId = doc.cashboxId
        settlementType = if (doc.settlementType.isBlank()) AdjustmentType.OPENING else doc.settlementType

        discountType = doc.discountType
        discountValue = doc.discountValue
        taxRate = doc.taxRate
        feesAmount = doc.feesAmount
        feesNote = doc.feesNote
        paidAmount = doc.paidAmount

        binding.etDate.setText(doc.date)
        binding.etReference.setText(doc.reference)
        binding.etNotes.setText(doc.notes)

        val docLines = repo.documentLines.getByDocument(documentId)

        lines.clear()

        docLines.forEach { line ->
            if (isAccountDocument()) {
                val accountName = accounts.firstOrNull { it.id == line.accountId }?.name ?: ""
                lines.add(
                    LineUi(
                        accountId = line.accountId,
                        accountName = accountName,
                        debit = line.debit,
                        credit = line.credit,
                        notes = line.notes
                    )
                )
            } else {
                val itemName = items.firstOrNull { it.id == line.itemId }?.name ?: ""
                lines.add(
                    LineUi(
                        itemId = line.itemId,
                        itemName = itemName,
                        qty = line.qty,
                        price = line.price,
                        total = line.total,
                        notes = line.notes
                    )
                )
            }
        }

        return true
    }

    private fun setupLinesAdapter() {
        lineAdapter = DocumentLineAdapter(
            isAccounting = isAccountDocument(),
            onClick = { index -> editLine(index) },
            onDelete = { index -> deleteLine(index) }
        )

        binding.rvLines.layoutManager = LinearLayoutManager(this)
        binding.rvLines.isNestedScrollingEnabled = false
        binding.rvLines.adapter = lineAdapter

        lineAdapter.submit(lines)
    }

    private fun getTitleForDocType(type: String): String {
        return getString(
            when (type) {
                DocType.SALE_CASH -> R.string.sales
                DocType.SALE_CREDIT -> R.string.sales
                DocType.PURCHASE_CASH -> R.string.purchases
                DocType.PURCHASE_CREDIT -> R.string.purchases
                DocType.RECEIPT -> R.string.receipt_payment
                DocType.PAYMENT -> R.string.receipt_payment
                DocType.JOURNAL_DAILY -> R.string.child_journal_daily
                DocType.JOURNAL_OPENING -> R.string.child_journal_opening
                DocType.STOCK_OUT -> R.string.child_stock_out
                DocType.STOCK_IN -> R.string.child_stock_in
                DocType.STOCK_TRANSFER -> R.string.child_stock_transfer
                DocType.STOCK_ADJUSTMENT -> R.string.child_stock_adjustment
                DocType.STOCKTAKE -> R.string.child_stocktake
                DocType.QUOTATION -> R.string.child_quotation
                DocType.PURCHASE_REQUEST -> R.string.child_purchase_request
                else -> R.string.app_name
            }
        )
    }

    private fun isAccountDocument(): Boolean {
        return docType == DocType.RECEIPT ||
            docType == DocType.PAYMENT ||
            docType == DocType.JOURNAL_DAILY ||
            docType == DocType.JOURNAL_OPENING
    }

    private fun isDiscountSupported(): Boolean {
        return docType == DocType.SALE_CASH ||
            docType == DocType.SALE_CREDIT ||
            docType == DocType.PURCHASE_CASH ||
            docType == DocType.PURCHASE_CREDIT
    }

    private fun updateVisibility() {
        val needsParty = docType == DocType.SALE_CASH ||
            docType == DocType.SALE_CREDIT ||
            docType == DocType.PURCHASE_CASH ||
            docType == DocType.PURCHASE_CREDIT

        val needsWarehouse = docType == DocType.SALE_CASH ||
            docType == DocType.SALE_CREDIT ||
            docType == DocType.PURCHASE_CASH ||
            docType == DocType.PURCHASE_CREDIT ||
            docType == DocType.STOCK_OUT ||
            docType == DocType.STOCK_IN ||
            docType == DocType.STOCK_TRANSFER ||
            docType == DocType.STOCK_ADJUSTMENT ||
            docType == DocType.STOCKTAKE

        val needsTargetWarehouse = docType == DocType.STOCK_TRANSFER

        val needsCashbox = docType == DocType.SALE_CASH ||
            docType == DocType.PURCHASE_CASH ||
            docType == DocType.RECEIPT ||
            docType == DocType.PAYMENT

        val needsSettlement = docType == DocType.STOCK_ADJUSTMENT

        binding.btnParty.visibility = if (needsParty) View.VISIBLE else View.GONE
        binding.btnWarehouse.visibility = if (needsWarehouse) View.VISIBLE else View.GONE
        binding.btnTargetWarehouse.visibility = if (needsTargetWarehouse) View.VISIBLE else View.GONE
        binding.btnCashbox.visibility = if (needsCashbox) View.VISIBLE else View.GONE
        binding.btnSettlement.visibility = if (needsSettlement) View.VISIBLE else View.GONE
    }

    private fun updateSelectors() {
        binding.btnParty.text = getString(R.string.party) + ": " + getPartyName()
        binding.btnWarehouse.text = getString(R.string.warehouse) + ": " + getWarehouseName(warehouseId)
        binding.btnTargetWarehouse.text = getString(R.string.target_warehouse) + ": " + getWarehouseName(targetWarehouseId)
        binding.btnCashbox.text = getString(R.string.cashbox) + ": " + getCashboxName()
        binding.btnCurrency.text = getString(R.string.currency) + ": " + getCurrencyName()
        binding.btnSettlement.text = getString(R.string.settlement_type) + ": " + getSettlementName()
    }

    private fun getPartyName(): String {
        if (partyAccountId == 0L) return getString(R.string.none)
        return accounts.firstOrNull { it.id == partyAccountId }?.name ?: getString(R.string.none)
    }

    private fun getWarehouseName(id: Long): String {
        if (id == 0L) return getString(R.string.none)
        return warehouses.firstOrNull { it.id == id }?.name ?: getString(R.string.none)
    }

    private fun getCashboxName(): String {
        if (cashboxId == 0L) return getString(R.string.none)
        return cashboxes.firstOrNull { it.id == cashboxId }?.name ?: getString(R.string.none)
    }

    private fun getCurrencyName(): String {
        if (currencyId == 0L) return getString(R.string.none)
        return currencies.firstOrNull { it.id == currencyId }?.name ?: getString(R.string.none)
    }

    private fun getSettlementName(): String {
        return getString(
            when (settlementType) {
                AdjustmentType.DAMAGED -> R.string.damaged
                AdjustmentType.INCREASE -> R.string.increase
                AdjustmentType.SHORTAGE -> R.string.shortage
                else -> R.string.opening
            }
        )
    }

    private fun showPartyDialog() {
        if (accounts.isEmpty()) {
            Toast.makeText(this, R.string.no_results, Toast.LENGTH_SHORT).show()
            return
        }

        val names = mutableListOf(getString(R.string.none))
        names.addAll(accounts.map { it.name })

        var selected = 0
        if (partyAccountId != 0L) {
            val index = accounts.indexOfFirst { it.id == partyAccountId }
            if (index >= 0) selected = index + 1
        }

        showSingleChoiceDialog(getString(R.string.select_party), names.toTypedArray(), selected) { which ->
            partyAccountId = if (which == 0) 0 else accounts[which - 1].id
            updateSelectors()
        }
    }

    private fun showWarehouseDialog() {
        if (warehouses.isEmpty()) {
            Toast.makeText(this, R.string.no_results, Toast.LENGTH_SHORT).show()
            return
        }

        val names = warehouses.map { it.name }.toTypedArray()
        var selected = warehouses.indexOfFirst { it.id == warehouseId }
        if (selected < 0) selected = 0

        showSingleChoiceDialog(getString(R.string.select_warehouse), names, selected) { which ->
            warehouseId = warehouses[which].id
            updateSelectors()
        }
    }

    private fun showTargetWarehouseDialog() {
        if (warehouses.isEmpty()) {
            Toast.makeText(this, R.string.no_results, Toast.LENGTH_SHORT).show()
            return
        }

        val names = warehouses.map { it.name }.toTypedArray()
        var selected = warehouses.indexOfFirst { it.id == targetWarehouseId }
        if (selected < 0) selected = 0

        showSingleChoiceDialog(getString(R.string.select_target_warehouse), names, selected) { which ->
            targetWarehouseId = warehouses[which].id
            updateSelectors()
        }
    }

    private fun showCashboxDialog() {
        if (cashboxes.isEmpty()) {
            Toast.makeText(this, R.string.no_results, Toast.LENGTH_SHORT).show()
            return
        }

        val names = cashboxes.map { it.name }.toTypedArray()
        var selected = cashboxes.indexOfFirst { it.id == cashboxId }
        if (selected < 0) selected = 0

        showSingleChoiceDialog(getString(R.string.select_cashbox), names, selected) { which ->
            cashboxId = cashboxes[which].id
            updateSelectors()
        }
    }

    private fun showCurrencyDialog() {
        if (currencies.isEmpty()) {
            Toast.makeText(this, R.string.no_results, Toast.LENGTH_SHORT).show()
            return
        }

        val names = currencies.map { it.name }.toTypedArray()
        var selected = currencies.indexOfFirst { it.id == currencyId }
        if (selected < 0) selected = 0

        showSingleChoiceDialog(getString(R.string.select_currency), names, selected) { which ->
            currencyId = currencies[which].id
            updateSelectors()
        }
    }

    private fun showSettlementDialog() {
        val names = arrayOf(
            getString(R.string.opening),
            getString(R.string.damaged),
            getString(R.string.increase),
            getString(R.string.shortage)
        )

        val values = arrayOf(
            AdjustmentType.OPENING,
            AdjustmentType.DAMAGED,
            AdjustmentType.INCREASE,
            AdjustmentType.SHORTAGE
        )

        var selected = values.indexOf(settlementType)
        if (selected < 0) selected = 0

        showSingleChoiceDialog(getString(R.string.settlement_type), names, selected) { which ->
            settlementType = values[which]
            updateSelectors()
        }
    }

    private fun showSingleChoiceDialog(
        title: String,
        items: Array<String>,
        selected: Int,
        onSelected: (Int) -> Unit
    ) {
        var current = selected

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setSingleChoiceItems(items, current) { _, which ->
                current = which
            }
            .setPositiveButton(R.string.save) { _, _ ->
                onSelected(current)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun addLine() {
        if (isAccountDocument()) {
            openAccountLineDialog(null)
        } else {
            openItemLineDialog(null)
        }
    }

    private fun editLine(index: Int) {
        if (index !in lines.indices) return

        if (isAccountDocument()) {
            openAccountLineDialog(index)
        } else {
            openItemLineDialog(index)
        }
    }

    private fun deleteLine(index: Int) {
        if (index !in lines.indices) return

        lines.removeAt(index)
        lineAdapter.submit(lines.toList())
        updateTotals()
    }

    private fun openItemLineDialog(index: Int?) {
        if (items.isEmpty()) {
            Toast.makeText(this, R.string.no_results, Toast.LENGTH_SHORT).show()
            return
        }

        val names = items.map { it.name }.toTypedArray()

        var selected = 0
        if (index != null) {
            val currentItemId = lines[index].itemId
            val found = items.indexOfFirst { it.id == currentItemId }
            if (found >= 0) selected = found
        }

        showSingleChoiceDialog(getString(R.string.select_item), names, selected) { which ->
            val item = items[which]
            showQtyPriceDialog(item, index)
        }
    }

    private fun showQtyPriceDialog(item: ItemEntity, index: Int?) {
        val container = createVerticalContainer()

        val etQty = createEditText(getString(R.string.qty))
        val etPrice = createEditText(getString(R.string.price))

        if (index != null) {
            etQty.setText(lines[index].qty.toString())
            etPrice.setText(lines[index].price.toString())
        }

        container.addView(etQty)
        container.addView(etPrice)

        MaterialAlertDialogBuilder(this)
            .setTitle(item.name)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val qty = etQty.text.toString().toDoubleOrNull() ?: 0.0
                val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                val total = qty * price

                if (index == null) {
                    lines.add(
                        LineUi(
                            itemId = item.id,
                            itemName = item.name,
                            qty = qty,
                            price = price,
                            total = total
                        )
                    )
                } else {
                    lines[index] = lines[index].copy(
                        itemId = item.id,
                        itemName = item.name,
                        qty = qty,
                        price = price,
                        total = total
                    )
                }

                lineAdapter.submit(lines.toList())
                updateTotals()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openAccountLineDialog(index: Int?) {
        if (accounts.isEmpty()) {
            Toast.makeText(this, R.string.no_results, Toast.LENGTH_SHORT).show()
            return
        }

        val names = accounts.map { it.name }.toTypedArray()

        var selected = 0
        if (index != null) {
            val currentAccountId = lines[index].accountId
            val found = accounts.indexOfFirst { it.id == currentAccountId }
            if (found >= 0) selected = found
        }

        showSingleChoiceDialog(getString(R.string.select_account), names, selected) { which ->
            val account = accounts[which]
            showAmountDialog(account, index)
        }
    }

    private fun showAmountDialog(account: AccountEntity, index: Int?) {
        val container = createVerticalContainer()

        if (docType == DocType.RECEIPT) {
            val etAmount = createEditText(getString(R.string.amount))

            if (index != null) {
                etAmount.setText(lines[index].credit.toString())
            }

            container.addView(etAmount)

            MaterialAlertDialogBuilder(this)
                .setTitle(account.name)
                .setView(container)
                .setPositiveButton(R.string.save) { _, _ ->
                    val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0

                    if (index == null) {
                        lines.add(
                            LineUi(
                                accountId = account.id,
                                accountName = account.name,
                                credit = amount
                            )
                        )
                    } else {
                        lines[index] = lines[index].copy(
                            accountId = account.id,
                            accountName = account.name,
                            credit = amount
                        )
                    }

                    lineAdapter.submit(lines.toList())
                    updateTotals()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()

        } else if (docType == DocType.PAYMENT) {
            val etAmount = createEditText(getString(R.string.amount))

            if (index != null) {
                etAmount.setText(lines[index].debit.toString())
            }

            container.addView(etAmount)

            MaterialAlertDialogBuilder(this)
                .setTitle(account.name)
                .setView(container)
                .setPositiveButton(R.string.save) { _, _ ->
                    val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0

                    if (index == null) {
                        lines.add(
                            LineUi(
                                accountId = account.id,
                                accountName = account.name,
                                debit = amount
                            )
                        )
                    } else {
                        lines[index] = lines[index].copy(
                            accountId = account.id,
                            accountName = account.name,
                            debit = amount
                        )
                    }

                    lineAdapter.submit(lines.toList())
                    updateTotals()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()

        } else {
            val etDebit = createEditText(getString(R.string.debit))
            val etCredit = createEditText(getString(R.string.credit))

            if (index != null) {
                etDebit.setText(lines[index].debit.toString())
                etCredit.setText(lines[index].credit.toString())
            }

            container.addView(etDebit)
            container.addView(etCredit)

            MaterialAlertDialogBuilder(this)
                .setTitle(account.name)
                .setView(container)
                .setPositiveButton(R.string.save) { _, _ ->
                    val debit = etDebit.text.toString().toDoubleOrNull() ?: 0.0
                    val credit = etCredit.text.toString().toDoubleOrNull() ?: 0.0

                    if (index == null) {
                        lines.add(
                            LineUi(
                                accountId = account.id,
                                accountName = account.name,
                                debit = debit,
                                credit = credit
                            )
                        )
                    } else {
                        lines[index] = lines[index].copy(
                            accountId = account.id,
                            accountName = account.name,
                            debit = debit,
                            credit = credit
                        )
                    }

                    lineAdapter.submit(lines.toList())
                    updateTotals()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun createVerticalContainer(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
    }

    private fun createEditText(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun updateTotals() {
        if (isAccountDocument()) {
            val debit = lines.sumOf { it.debit }
            val credit = lines.sumOf { it.credit }

            binding.tvSummary.text = String.format(
                Locale.US,
                "مدين: %.2f - دائن: %.2f",
                debit,
                credit
            )
            return
        }

        val total = lines.sumOf { it.qty * it.price }

        val discountAmount = if (discountType == DiscountType.PERCENT) {
            total * discountValue / 100.0
        } else {
            discountValue
        }

        val taxAmount = (total - discountAmount) * taxRate / 100.0
        val net = total - discountAmount + taxAmount + feesAmount

        val remaining = if (docType == DocType.SALE_CREDIT || docType == DocType.PURCHASE_CREDIT) {
            net - paidAmount
        } else {
            0.0
        }

        binding.tvSummary.text = String.format(
            Locale.US,
            "المجموع: %.2f | الخصم: %.2f | الضريبة: %.2f | الصافي: %.2f | المتبقي: %.2f",
            total,
            discountAmount,
            taxAmount,
            net,
            remaining
        )
    }

    private fun showDiscountDialog() {
        val container = createVerticalContainer()

        val radioGroup = RadioGroup(this)
        val amountId = 1001
        val percentId = 1002

        val rbAmount = RadioButton(this).apply {
            id = amountId
            text = getString(R.string.discount_amount)
        }

        val rbPercent = RadioButton(this).apply {
            id = percentId
            text = getString(R.string.discount_percent)
        }

        radioGroup.addView(rbAmount)
        radioGroup.addView(rbPercent)

        if (discountType == DiscountType.PERCENT) {
            radioGroup.check(percentId)
        } else {
            radioGroup.check(amountId)
        }

        val etDiscount = createEditText(getString(R.string.discount))
        val etTax = createEditText(getString(R.string.tax_rate))
        val etFees = createEditText(getString(R.string.fees))
        val etFeesNote = createEditText(getString(R.string.fees_note))
        val etPaid = createEditText(getString(R.string.paid_amount))

        etDiscount.setText(discountValue.toString())
        etTax.setText(taxRate.toString())
        etFees.setText(feesAmount.toString())
        etFeesNote.setText(feesNote)
        etPaid.setText(paidAmount.toString())

        container.addView(radioGroup)
        container.addView(etDiscount)
        container.addView(etTax)
        container.addView(etFees)
        container.addView(etFeesNote)
        container.addView(etPaid)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.discount)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                discountType = if (radioGroup.checkedRadioButtonId == percentId) {
                    DiscountType.PERCENT
                } else {
                    DiscountType.AMOUNT
                }

                discountValue = etDiscount.text.toString().toDoubleOrNull() ?: 0.0
                taxRate = etTax.text.toString().toDoubleOrNull() ?: 0.0
                feesAmount = etFees.text.toString().toDoubleOrNull() ?: 0.0
                feesNote = etFeesNote.text.toString().trim()
                paidAmount = etPaid.text.toString().toDoubleOrNull() ?: 0.0

                updateTotals()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveDocument() {
        if (lines.isEmpty()) {
            Toast.makeText(this, R.string.no_results, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val requestLines = lines.mapIndexed { index, line ->
                    DocumentRequest.Line(
                        itemId = line.itemId,
                        accountId = line.accountId,
                        qty = line.qty,
                        price = line.price,
                        total = if (isAccountDocument()) 0.0 else line.total,
                        debit = line.debit,
                        credit = line.credit,
                        notes = line.notes,
                        sortOrder = index
                    )
                }

                val request = DocumentRequest(
                    documentId = documentId,
                    docType = docType,
                    date = binding.etDate.text.toString().trim(),
                    reference = binding.etReference.text.toString().trim(),
                    partyAccountId = partyAccountId,
                    warehouseId = warehouseId,
                    targetWarehouseId = targetWarehouseId,
                    currencyId = currencyId,
                    cashboxId = cashboxId,
                    settlementType = settlementType,
                    notes = binding.etNotes.text.toString().trim(),
                    discountType = discountType,
                    discountValue = discountValue,
                    taxRate = taxRate,
                    feesAmount = feesAmount,
                    feesNote = feesNote,
                    paidAmount = paidAmount,
                    lines = requestLines
                )

                repo.documentService.saveDocument(request)

                Toast.makeText(this@DocumentEditActivity, R.string.saved_success, Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@DocumentEditActivity,
                    e.message ?: getString(R.string.save_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
