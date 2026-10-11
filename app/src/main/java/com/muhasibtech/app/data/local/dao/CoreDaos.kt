package com.muhasibtech.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.muhasibtech.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert
    suspend fun insert(entity: AccountEntity): Long

    @Update
    suspend fun update(entity: AccountEntity)

    @Delete
    suspend fun delete(entity: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE parentId = :parentId ORDER BY name COLLATE NOCASE ASC")
    fun observeChildren(parentId: Long): Flow<List<AccountEntity>>

    @Query(
        "SELECT * FROM accounts WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC"
    )
    fun search(query: String): Flow<List<AccountEntity>>
}

@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(entity: CategoryEntity): Long

    @Update
    suspend fun update(entity: CategoryEntity)

    @Delete
    suspend fun delete(entity: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name COLLATE NOCASE ASC")
    fun observeByType(type: String): Flow<List<CategoryEntity>>
}

@Dao
interface CurrencyDao {

    @Insert
    suspend fun insert(entity: CurrencyEntity): Long

    @Update
    suspend fun update(entity: CurrencyEntity)

    @Delete
    suspend fun delete(entity: CurrencyEntity)

    @Query("SELECT * FROM currencies WHERE id = :id")
    suspend fun getById(id: Long): CurrencyEntity?

    @Query("SELECT * FROM currencies WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): CurrencyEntity?

    @Query("SELECT * FROM currencies ORDER BY isBase DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE isActive = 1 ORDER BY isBase DESC, name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<CurrencyEntity>>
}

@Dao
interface ExchangeRateDao {

    @Insert
    suspend fun insert(entity: ExchangeRateEntity): Long

    @Delete
    suspend fun delete(entity: ExchangeRateEntity)

    @Query("SELECT * FROM exchange_rates WHERE currencyId = :currencyId ORDER BY rateDate DESC, id DESC")
    fun observeByCurrency(currencyId: Long): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates WHERE currencyId = :currencyId ORDER BY rateDate DESC, id DESC LIMIT 1")
    suspend fun getLatest(currencyId: Long): ExchangeRateEntity?
}

@Dao
interface WarehouseDao {

    @Insert
    suspend fun insert(entity: WarehouseEntity): Long

    @Update
    suspend fun update(entity: WarehouseEntity)

    @Delete
    suspend fun delete(entity: WarehouseEntity)

    @Query("SELECT * FROM warehouses WHERE id = :id")
    suspend fun getById(id: Long): WarehouseEntity?

    @Query("SELECT * FROM warehouses ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<WarehouseEntity>>

    @Query("SELECT * FROM warehouses WHERE isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<WarehouseEntity>>

    @Query("SELECT * FROM warehouses WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): WarehouseEntity?
}

@Dao
interface CashboxDao {

    @Insert
    suspend fun insert(entity: CashboxEntity): Long

    @Update
    suspend fun update(entity: CashboxEntity)

    @Delete
    suspend fun delete(entity: CashboxEntity)

    @Query("SELECT * FROM cashboxes WHERE id = :id")
    suspend fun getById(id: Long): CashboxEntity?

    @Query("SELECT * FROM cashboxes ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CashboxEntity>>

    @Query("SELECT * FROM cashboxes WHERE isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<CashboxEntity>>

    @Query("SELECT * FROM cashboxes WHERE accountId = :accountId LIMIT 1")
    suspend fun getByAccountId(accountId: Long): CashboxEntity?

    @Query("SELECT * FROM cashboxes WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): CashboxEntity?
}

@Dao
interface AccountLimitDao {

    @Insert
    suspend fun insert(entity: AccountLimitEntity): Long

    @Update
    suspend fun update(entity: AccountLimitEntity)

    @Delete
    suspend fun delete(entity: AccountLimitEntity)

    @Query("SELECT * FROM account_limits WHERE accountId = :accountId AND currencyId = :currencyId LIMIT 1")
    suspend fun getByAccountAndCurrency(accountId: Long, currencyId: Long): AccountLimitEntity?

    @Query("SELECT * FROM account_limits WHERE accountId = :accountId")
    fun observeByAccount(accountId: Long): Flow<List<AccountLimitEntity>>
}

@Dao
interface UserDao {

    @Insert
    suspend fun insert(entity: UserEntity): Long

    @Update
    suspend fun update(entity: UserEntity)

    @Delete
    suspend fun delete(entity: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY fullName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY fullName COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<UserEntity>>
}

@Dao
interface SettingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SettingEntity)

    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getByKey(key: String): SettingEntity?

    @Query("SELECT * FROM settings")
    fun observeAll(): Flow<List<SettingEntity>>
}

@Dao
interface AlertDao {

    @Insert
    suspend fun insert(entity: AlertEntity): Long

    @Update
    suspend fun update(entity: AlertEntity)

    @Delete
    suspend fun delete(entity: AlertEntity)

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun getById(id: Long): AlertEntity?

    @Query("SELECT * FROM alerts ORDER BY alertAt ASC")
    fun observeAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isDone = 0 ORDER BY alertAt ASC")
    fun observePending(): Flow<List<AlertEntity>>

    @Query("UPDATE alerts SET isDone = 1 WHERE id = :id")
    suspend fun markDone(id: Long)
}

@Dao
interface YearlyClosingDao {

    @Insert
    suspend fun insert(entity: YearlyClosingEntity): Long

    @Delete
    suspend fun delete(entity: YearlyClosingEntity)

    @Query("SELECT * FROM yearly_closings ORDER BY closingDate DESC")
    fun observeAll(): Flow<List<YearlyClosingEntity>>

    @Query("SELECT * FROM yearly_closings WHERE closingDate = :date AND currencyId = :currencyId LIMIT 1")
    suspend fun getByDateAndCurrency(date: String, currencyId: Long): YearlyClosingEntity?
}
