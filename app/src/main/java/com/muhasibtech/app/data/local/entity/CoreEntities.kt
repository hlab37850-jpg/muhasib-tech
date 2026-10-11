package com.muhasibtech.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts", indices = [Index("parentId"), Index("nature")])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String = "",
    val name: String,
    val parentId: Long = 0,
    val nature: String = "ASSET",
    val isSystem: Boolean = false,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories", indices = [Index("type")])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String = "GENERAL",
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "currencies", indices = [Index(value = ["code"], unique = true)])
data class CurrencyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val symbol: String = "",
    val decimalPlaces: Int = 2,
    val isBase: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exchange_rates", indices = [Index("currencyId"), Index("rateDate")])
data class ExchangeRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val currencyId: Long,
    val rateDate: String,
    val rate: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "warehouses")
data class WarehouseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cashboxes", indices = [Index("accountId"), Index("warehouseId")])
data class CashboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val accountId: Long,
    val warehouseId: Long = 0,
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "account_limits",
    indices = [Index(value = ["accountId", "currencyId"], unique = true)]
)
data class AccountLimitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val currencyId: Long,
    val debitLimit: Double = 0.0,
    val creditLimit: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val fullName: String,
    val passwordHash: String = "",
    val isAdmin: Boolean = false,
    val warehouseId: Long = 0,
    val cashboxId: Long = 0,
    val permissionsJson: String = "{}",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "alerts", indices = [Index("alertAt"), Index("isDone")])
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String = "",
    val alertAt: Long,
    val isDone: Boolean = false,
    val relatedType: String = "",
    val relatedId: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "yearly_closings",
    indices = [Index(value = ["closingDate", "currencyId"], unique = true)]
)
data class YearlyClosingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val closingDate: String,
    val currencyId: Long,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
