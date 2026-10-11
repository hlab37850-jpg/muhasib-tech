package com.muhasibtech.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.muhasibtech.app.data.local.dao.*
import com.muhasibtech.app.data.local.entity.*

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        CurrencyEntity::class,
        ExchangeRateEntity::class,
        WarehouseEntity::class,
        CashboxEntity::class,
        AccountLimitEntity::class,
        UserEntity::class,
        SettingEntity::class,
        AlertEntity::class,
        YearlyClosingEntity::class,
        ItemGroupEntity::class,
        UnitEntity::class,
        ItemEntity::class,
        ItemUnitEntity::class,
        ItemPriceEntity::class,
        StockBalanceEntity::class,
        DocumentEntity::class,
        DocumentLineEntity::class,
        JournalLineEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun cashboxDao(): CashboxDao
    abstract fun accountLimitDao(): AccountLimitDao
    abstract fun userDao(): UserDao
    abstract fun settingDao(): SettingDao
    abstract fun alertDao(): AlertDao
    abstract fun yearlyClosingDao(): YearlyClosingDao

    abstract fun itemGroupDao(): ItemGroupDao
    abstract fun unitDao(): UnitDao
    abstract fun itemDao(): ItemDao
    abstract fun itemUnitDao(): ItemUnitDao
    abstract fun itemPriceDao(): ItemPriceDao
    abstract fun stockBalanceDao(): StockBalanceDao

    abstract fun documentDao(): DocumentDao
    abstract fun documentLineDao(): DocumentLineDao
    abstract fun journalLineDao(): JournalLineDao

    abstract fun appQueriesDao(): AppQueriesDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "muhasib_tech.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
