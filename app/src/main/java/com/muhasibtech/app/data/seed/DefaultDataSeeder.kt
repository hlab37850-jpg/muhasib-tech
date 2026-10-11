package com.muhasibtech.app.data.seed

import androidx.room.withTransaction
import com.muhasibtech.app.data.local.AppDatabase
import com.muhasibtech.app.data.local.entity.AccountEntity
import com.muhasibtech.app.data.local.entity.CashboxEntity
import com.muhasibtech.app.data.local.entity.CategoryEntity
import com.muhasibtech.app.data.local.entity.CurrencyEntity
import com.muhasibtech.app.data.local.entity.ItemGroupEntity
import com.muhasibtech.app.data.local.entity.SettingEntity
import com.muhasibtech.app.data.local.entity.UnitEntity
import com.muhasibtech.app.data.local.entity.UserEntity
import com.muhasibtech.app.data.local.entity.WarehouseEntity
import com.muhasibtech.app.data.model.AccountNature
import com.muhasibtech.app.data.model.DefaultData
import com.muhasibtech.app.data.model.SettingKeys
import kotlinx.coroutines.flow.first

object DefaultDataSeeder {

    suspend fun seed(db: AppDatabase) {
        val seedSetting = db.settingDao().getByKey(SettingKeys.SEED_VERSION)
        val existingAccounts = db.accountDao().observeAll().first()

        if (seedSetting != null || existingAccounts.isNotEmpty()) {
            if (seedSetting == null) {
                db.settingDao().upsert(
                    SettingEntity(
                        key = SettingKeys.SEED_VERSION,
                        value = "1"
                    )
                )
            }
            return
        }

        db.withTransaction {

            val localCurrencyId = db.currencyDao().insert(
                CurrencyEntity(
                    code = DefaultData.CURRENCY_LOCAL_CODE,
                    name = DefaultData.CURRENCY_LOCAL_NAME,
                    symbol = DefaultData.CURRENCY_LOCAL_SYMBOL,
                    decimalPlaces = 2,
                    isBase = true
                )
            )

            val usdCurrencyId = db.currencyDao().insert(
                CurrencyEntity(
                    code = DefaultData.CURRENCY_USD_CODE,
                    name = DefaultData.CURRENCY_USD_NAME,
                    symbol = DefaultData.CURRENCY_USD_SYMBOL,
                    decimalPlaces = 2,
                    isBase = false
                )
            )

            val warehouseId = db.warehouseDao().insert(
                WarehouseEntity(
                    name = DefaultData.WAREHOUSE_MAIN,
                    isDefault = true
                )
            )

            val assets = insertAccount(db, "1", "الأصول", 0, AccountNature.ASSET)
            val liabilities = insertAccount(db, "2", "الالتزامات", 0, AccountNature.LIABILITY)
            val equity = insertAccount(db, "3", "حقوق الملكية", 0, AccountNature.EQUITY)
            val revenue = insertAccount(db, "4", "الإيرادات", 0, AccountNature.REVENUE)
            val expenses = insertAccount(db, "5", "المصروفات", 0, AccountNature.EXPENSE)

            val cashAccountId = insertAccount(db, "101", "الصندوق", assets, AccountNature.ASSET)
            insertAccount(db, "102", "البنك", assets, AccountNature.ASSET)
            val customersAccountId = insertAccount(db, "103", "العملاء", assets, AccountNature.ASSET)
            val inventoryAccountId = insertAccount(db, "104", "المخزون", assets, AccountNature.ASSET)

            val suppliersAccountId = insertAccount(db, "201", "الموردون", liabilities, AccountNature.LIABILITY)

            insertAccount(db, "301", "رأس المال", equity, AccountNature.EQUITY)
            val openingAccountId = insertAccount(db, "302", "الحساب الافتتاحي", equity, AccountNature.EQUITY)

            val salesAccountId = insertAccount(db, "401", "المبيعات", revenue, AccountNature.REVENUE)
            val otherRevenueAccountId = insertAccount(db, "402", "إيرادات أخرى", revenue, AccountNature.REVENUE)

            val cogsAccountId = insertAccount(db, "501", "تكلفة البضاعة المباعة", expenses, AccountNature.EXPENSE)
            val discountAccountId = insertAccount(db, "502", "الخصم التجاري", expenses, AccountNature.EXPENSE)
            val taxAccountId = insertAccount(db, "503", "الضريبة", expenses, AccountNature.EXPENSE)
            val feesAccountId = insertAccount(db, "504", "رسوم أخرى", expenses, AccountNature.EXPENSE)
            insertAccount(db, "505", "مصروفات عامة", expenses, AccountNature.EXPENSE)
            val stockLossAccountId = insertAccount(db, "506", "تلف ونقص مخزني", expenses, AccountNature.EXPENSE)

            val cashboxId = db.cashboxDao().insert(
                CashboxEntity(
                    name = DefaultData.CASHBOX_MAIN,
                    accountId = cashAccountId,
                    warehouseId = warehouseId,
                    isDefault = true
                )
            )

            val categoryId = db.categoryDao().insert(
                CategoryEntity(
                    name = DefaultData.CATEGORY_GENERAL,
                    type = "GENERAL"
                )
            )

            db.itemGroupDao().insert(
                ItemGroupEntity(
                    name = DefaultData.GROUP_GENERAL,
                    categoryId = categoryId
                )
            )

            db.unitDao().insert(UnitEntity(name = DefaultData.UNIT_PIECE))
            db.unitDao().insert(UnitEntity(name = DefaultData.UNIT_KILO))
            db.unitDao().insert(UnitEntity(name = DefaultData.UNIT_CARTON))
            db.unitDao().insert(UnitEntity(name = DefaultData.UNIT_BAG))

            db.userDao().insert(
                UserEntity(
                    username = "admin",
                    fullName = DefaultData.USER_ADMIN,
                    passwordHash = "",
                    isAdmin = true,
                    warehouseId = warehouseId,
                    cashboxId = cashboxId,
                    permissionsJson = "{\"all\":true}"
                )
            )

            upsert(db, SettingKeys.COMPANY_NAME, "المحاسب تك")
            upsert(db, SettingKeys.CURRENT_WAREHOUSE_ID, warehouseId)
            upsert(db, SettingKeys.CURRENT_CURRENCY_ID, localCurrencyId)
            upsert(db, SettingKeys.CURRENCY_LOCAL_ID, localCurrencyId)
            upsert(db, SettingKeys.CURRENCY_USD_ID, usdCurrencyId)

            upsert(db, SettingKeys.ACCOUNT_CASH_ID, cashAccountId)
            upsert(db, SettingKeys.ACCOUNT_CUSTOMERS_ID, customersAccountId)
            upsert(db, SettingKeys.ACCOUNT_SUPPLIERS_ID, suppliersAccountId)
            upsert(db, SettingKeys.ACCOUNT_INVENTORY_ID, inventoryAccountId)
            upsert(db, SettingKeys.ACCOUNT_OPENING_ID, openingAccountId)
            upsert(db, SettingKeys.ACCOUNT_SALES_ID, salesAccountId)
            upsert(db, SettingKeys.ACCOUNT_OTHER_REVENUE_ID, otherRevenueAccountId)
            upsert(db, SettingKeys.ACCOUNT_COGS_ID, cogsAccountId)
            upsert(db, SettingKeys.ACCOUNT_DISCOUNT_ID, discountAccountId)
            upsert(db, SettingKeys.ACCOUNT_TAX_ID, taxAccountId)
            upsert(db, SettingKeys.ACCOUNT_FEES_ID, feesAccountId)
            upsert(db, SettingKeys.ACCOUNT_STOCK_LOSS_ID, stockLossAccountId)

            upsert(db, SettingKeys.ALLOW_EDIT_PRICE, "1")
            upsert(db, SettingKeys.ALLOW_DELETE_OPERATION, "1")
            upsert(db, SettingKeys.ALLOW_EDIT_DATE, "1")
            upsert(db, SettingKeys.AUTO_ADD_PURCHASE_PRICE, "1")
            upsert(db, SettingKeys.USE_LAST_PRICE, "1")
            upsert(db, SettingKeys.UPDATE_AVERAGE_COST, "1")

            upsert(db, SettingKeys.SEED_VERSION, "1")
        }
    }

    private suspend fun insertAccount(
        db: AppDatabase,
        code: String,
        name: String,
        parentId: Long,
        nature: String
    ): Long {
        return db.accountDao().insert(
            AccountEntity(
                code = code,
                name = name,
                parentId = parentId,
                nature = nature,
                isSystem = false,
                isActive = true
            )
        )
    }

    private suspend fun upsert(db: AppDatabase, key: String, value: String) {
        db.settingDao().upsert(SettingEntity(key = key, value = value))
    }

    private suspend fun upsert(db: AppDatabase, key: String, value: Long) {
        db.settingDao().upsert(SettingEntity(key = key, value = value.toString()))
    }
}
