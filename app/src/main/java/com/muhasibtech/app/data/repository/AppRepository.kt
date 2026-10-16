package com.muhasibtech.app.data.repository

import com.muhasibtech.app.data.local.AppDatabase
import com.muhasibtech.app.data.service.DocumentService

class AppRepository(
    private val database: AppDatabase
) {

    val accounts = database.accountDao()
    val categories = database.categoryDao()
    val currencies = database.currencyDao()
    val exchangeRates = database.exchangeRateDao()
    val warehouses = database.warehouseDao()
    val cashboxes = database.cashboxDao()
    val accountLimits = database.accountLimitDao()
    val users = database.userDao()
    val alerts = database.alertDao()
    val yearlyClosings = database.yearlyClosingDao()

    val itemGroups = database.itemGroupDao()
    val units = database.unitDao()
    val items = database.itemDao()
    val itemUnits = database.itemUnitDao()
    val itemPrices = database.itemPriceDao()
    val stockBalances = database.stockBalanceDao()

    val documents = database.documentDao()
    val documentLines = database.documentLineDao()
    val journalLines = database.journalLineDao()

    val queries = database.appQueriesDao()

    val settings = SettingsRepository(database.settingDao())

    val documentService: DocumentService by lazy {
        DocumentService(database, this)
    }

    companion object {

        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(database: AppDatabase): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE ?: AppRepository(database)
                INSTANCE = instance
                instance
            }
        }

        fun clearInstanceForDatabaseRestore() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
}
