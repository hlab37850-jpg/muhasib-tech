package com.muhasibtech.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "item_groups", indices = [Index("categoryId")])
data class ItemGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long = 0,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "items",
    indices = [Index("barcode"), Index("groupId"), Index("baseUnitId"), Index("categoryId")]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val groupId: Long = 0,
    val baseUnitId: Long = 0,
    val categoryId: Long = 0,
    val openingQty: Double = 0.0,
    val openingCost: Double = 0.0,
    val expirationDate: String = "",
    val minStock: Double = 0.0,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "item_units",
    indices = [Index(value = ["itemId", "unitId"], unique = true)]
)
data class ItemUnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val unitId: Long,
    val conversionFactor: Double = 1.0,
    val isBase: Boolean = false,
    val barcode: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "item_prices",
    indices = [Index("itemId"), Index("currencyId"), Index("effectiveDate")]
)
data class ItemPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val currencyId: Long,
    val price: Double,
    val effectiveDate: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_balances",
    indices = [Index(value = ["itemId", "warehouseId"], unique = true)]
)
data class StockBalanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val warehouseId: Long,
    val qty: Double = 0.0,
    val averageCost: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "documents",
    indices = [
        Index("docType"),
        Index("date"),
        Index("partyAccountId"),
        Index("warehouseId"),
        Index("currencyId"),
        Index("cashboxId"),
        Index("isClosed")
    ]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val docType: String,
    val docNumber: Long = 0,
    val date: String,
    val reference: String = "",
    val partyAccountId: Long = 0,
    val warehouseId: Long = 0,
    val targetWarehouseId: Long = 0,
    val currencyId: Long = 0,
    val cashboxId: Long = 0,
    val currencyRate: Double = 1.0,
    val isCredit: Boolean = false,
    val settlementType: String = "",
    val notes: String = "",
    val discountType: String = "AMOUNT",
    val discountValue: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val feesAmount: Double = 0.0,
    val feesNote: String = "",
    val paidAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val costTotal: Double = 0.0,
    val isClosed: Boolean = false,
    val createdBy: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "document_lines",
    indices = [Index("documentId"), Index("itemId"), Index("accountId")]
)
data class DocumentLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val itemId: Long = 0,
    val accountId: Long = 0,
    val qty: Double = 0.0,
    val price: Double = 0.0,
    val total: Double = 0.0,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val notes: String = "",
    val sortOrder: Int = 0,
    val cost: Double = 0.0,
    val previousQty: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_lines",
    indices = [
        Index("documentId"),
        Index("accountId"),
        Index("currencyId"),
        Index("date")
    ]
)
data class JournalLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long = 0,
    val date: String,
    val accountId: Long,
    val currencyId: Long,
    val rate: Double = 1.0,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val details: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
