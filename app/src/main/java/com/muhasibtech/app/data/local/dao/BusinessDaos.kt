package com.muhasibtech.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.muhasibtech.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemGroupDao {

    @Insert
    suspend fun insert(entity: ItemGroupEntity): Long

    @Update
    suspend fun update(entity: ItemGroupEntity)

    @Delete
    suspend fun delete(entity: ItemGroupEntity)

    @Query("SELECT * FROM item_groups WHERE id = :id")
    suspend fun getById(id: Long): ItemGroupEntity?

    @Query("SELECT * FROM item_groups ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ItemGroupEntity>>

    @Query("SELECT * FROM item_groups WHERE isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<ItemGroupEntity>>
}

@Dao
interface UnitDao {

    @Insert
    suspend fun insert(entity: UnitEntity): Long

    @Update
    suspend fun update(entity: UnitEntity)

    @Delete
    suspend fun delete(entity: UnitEntity)

    @Query("SELECT * FROM units WHERE id = :id")
    suspend fun getById(id: Long): UnitEntity?

    @Query("SELECT * FROM units ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units WHERE isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<UnitEntity>>
}

@Dao
interface ItemDao {

    @Insert
    suspend fun insert(entity: ItemEntity): Long

    @Update
    suspend fun update(entity: ItemEntity)

    @Delete
    suspend fun delete(entity: ItemEntity)

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?

    @Query("SELECT * FROM items ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<ItemEntity>>

    @Query(
        "SELECT * FROM items WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC"
    )
    fun search(query: String): Flow<List<ItemEntity>>
}

@Dao
interface ItemUnitDao {

    @Insert
    suspend fun insert(entity: ItemUnitEntity): Long

    @Update
    suspend fun update(entity: ItemUnitEntity)

    @Delete
    suspend fun delete(entity: ItemUnitEntity)

    @Query("SELECT * FROM item_units WHERE itemId = :itemId")
    fun observeByItem(itemId: Long): Flow<List<ItemUnitEntity>>

    @Query("SELECT * FROM item_units WHERE itemId = :itemId AND isBase = 1 LIMIT 1")
    suspend fun getBaseUnit(itemId: Long): ItemUnitEntity?
}

@Dao
interface ItemPriceDao {

    @Insert
    suspend fun insert(entity: ItemPriceEntity): Long

    @Delete
    suspend fun delete(entity: ItemPriceEntity)

    @Query("SELECT * FROM item_prices WHERE itemId = :itemId ORDER BY effectiveDate DESC, id DESC")
    fun observeByItem(itemId: Long): Flow<List<ItemPriceEntity>>

    @Query(
        "SELECT * FROM item_prices WHERE itemId = :itemId AND currencyId = :currencyId AND isActive = 1 ORDER BY effectiveDate DESC, id DESC LIMIT 1"
    )
    suspend fun getLatest(itemId: Long, currencyId: Long): ItemPriceEntity?

    @Query(
        "SELECT * FROM item_prices WHERE itemId = :itemId AND currencyId = :currencyId AND isActive = 1 AND effectiveDate <= :date ORDER BY effectiveDate DESC, id DESC LIMIT 1"
    )
    suspend fun getEffectivePrice(itemId: Long, currencyId: Long, date: String): ItemPriceEntity?
}

@Dao
interface StockBalanceDao {

    @Insert
    suspend fun insert(entity: StockBalanceEntity): Long

    @Query("SELECT * FROM stock_balances WHERE itemId = :itemId AND warehouseId = :warehouseId")
    fun observe(itemId: Long, warehouseId: Long): Flow<StockBalanceEntity?>

    @Query("SELECT * FROM stock_balances WHERE itemId = :itemId AND warehouseId = :warehouseId")
    suspend fun getOnce(itemId: Long, warehouseId: Long): StockBalanceEntity?

    @Query("SELECT * FROM stock_balances WHERE warehouseId = :warehouseId AND qty > 0")
    fun observeInStock(warehouseId: Long): Flow<List<StockBalanceEntity>>

    @Query(
        "UPDATE stock_balances SET qty = :qty, averageCost = :averageCost, updatedAt = :updatedAt WHERE itemId = :itemId AND warehouseId = :warehouseId"
    )
    suspend fun updateBalance(
        itemId: Long,
        warehouseId: Long,
        qty: Double,
        averageCost: Double,
        updatedAt: Long
    ): Int
}

@Dao
interface DocumentDao {

    @Insert
    suspend fun insert(entity: DocumentEntity): Long

    @Update
    suspend fun update(entity: DocumentEntity)

    @Delete
    suspend fun delete(entity: DocumentEntity)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE docType = :docType ORDER BY date DESC, id DESC")
    fun observeByType(docType: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE date BETWEEN :fromDate AND :toDate ORDER BY date DESC, id DESC")
    fun observeBetween(fromDate: String, toDate: String): Flow<List<DocumentEntity>>

    @Query("SELECT COALESCE(MAX(docNumber), 0) FROM documents WHERE docType = :docType")
    suspend fun getLastDocNumber(docType: String): Long

    @Query("UPDATE documents SET isClosed = 1 WHERE date <= :date")
    suspend fun closeDocumentsUpTo(date: String)
}

@Dao
interface DocumentLineDao {

    @Insert
    suspend fun insertAll(lines: List<DocumentLineEntity>)

    @Query("DELETE FROM document_lines WHERE documentId = :documentId")
    suspend fun deleteByDocument(documentId: Long)

    @Query("SELECT * FROM document_lines WHERE documentId = :documentId ORDER BY sortOrder ASC, id ASC")
    fun observeByDocument(documentId: Long): Flow<List<DocumentLineEntity>>

    @Query("SELECT * FROM document_lines WHERE documentId = :documentId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByDocument(documentId: Long): List<DocumentLineEntity>
}

@Dao
interface JournalLineDao {

    @Insert
    suspend fun insertAll(lines: List<JournalLineEntity>)

    @Query("DELETE FROM journal_lines WHERE documentId = :documentId")
    suspend fun deleteByDocument(documentId: Long)

    @Query("SELECT * FROM journal_lines WHERE documentId = :documentId ORDER BY id ASC")
    fun observeByDocument(documentId: Long): Flow<List<JournalLineEntity>>

    @Query("SELECT * FROM journal_lines WHERE accountId = :accountId ORDER BY date DESC, id DESC")
    fun observeByAccount(accountId: Long): Flow<List<JournalLineEntity>>

    @Query(
        "SELECT COALESCE(SUM(debit), 0) - COALESCE(SUM(credit), 0) FROM journal_lines WHERE accountId = :accountId AND date <= :date"
    )
    suspend fun getAccountBalance(accountId: Long, date: String): Double

    @Query(
        "SELECT COALESCE(SUM(debit), 0) - COALESCE(SUM(credit), 0) FROM journal_lines WHERE accountId = :accountId AND currencyId = :currencyId AND date <= :date"
    )
    suspend fun getAccountBalanceByCurrency(accountId: Long, currencyId: Long, date: String): Double
}
