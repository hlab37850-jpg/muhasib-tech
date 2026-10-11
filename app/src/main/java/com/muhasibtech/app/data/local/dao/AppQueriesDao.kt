package com.muhasibtech.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.muhasibtech.app.data.local.entity.AccountLimitEntity
import com.muhasibtech.app.data.local.entity.DocumentEntity
import com.muhasibtech.app.data.local.entity.DocumentLineEntity
import com.muhasibtech.app.data.local.entity.ExchangeRateEntity
import com.muhasibtech.app.data.local.entity.ItemPriceEntity
import com.muhasibtech.app.data.local.entity.ItemUnitEntity
import com.muhasibtech.app.data.local.entity.JournalLineEntity
import com.muhasibtech.app.data.local.entity.StockBalanceEntity
import kotlinx.coroutines.flow.Flow

data class AccountBalance(
    val accountId: Long,
    val totalDebit: Double,
    val totalCredit: Double
)

@Dao
interface AppQueriesDao {

    @Query("SELECT * FROM exchange_rates ORDER BY rateDate DESC, id DESC")
    fun observeExchangeRates(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM account_limits ORDER BY id DESC")
    fun observeAccountLimits(): Flow<List<AccountLimitEntity>>

    @Query("SELECT * FROM item_units ORDER BY id DESC")
    fun observeItemUnits(): Flow<List<ItemUnitEntity>>

    @Query("SELECT * FROM item_prices ORDER BY effectiveDate DESC, id DESC")
    fun observeItemPrices(): Flow<List<ItemPriceEntity>>

    @Query("SELECT * FROM stock_balances ORDER BY updatedAt DESC")
    fun observeStockBalances(): Flow<List<StockBalanceEntity>>

    @Query("SELECT * FROM journal_lines ORDER BY date DESC, id DESC")
    fun observeJournalLines(): Flow<List<JournalLineEntity>>

    @Query("SELECT * FROM documents WHERE docType IN ('SALE_CASH', 'SALE_CREDIT') ORDER BY date DESC, id DESC")
    fun observeSales(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE docType IN ('PURCHASE_CASH', 'PURCHASE_CREDIT') ORDER BY date DESC, id DESC")
    fun observePurchases(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE docType IN ('RECEIPT', 'PAYMENT') ORDER BY date DESC, id DESC")
    fun observeReceiptPayment(): Flow<List<DocumentEntity>>

    @Query(
        "SELECT dl.* FROM document_lines dl INNER JOIN documents d ON d.id = dl.documentId " +
            "WHERE dl.itemId = :itemId AND d.docType IN ('PURCHASE_CASH', 'PURCHASE_CREDIT') " +
            "ORDER BY d.date DESC, d.id DESC LIMIT 1"
    )
    suspend fun getLastPurchaseLine(itemId: Long): DocumentLineEntity?

    @Query(
        "SELECT dl.* FROM document_lines dl INNER JOIN documents d ON d.id = dl.documentId " +
            "WHERE dl.itemId = :itemId AND d.docType IN ('SALE_CASH', 'SALE_CREDIT') " +
            "ORDER BY d.date DESC, d.id DESC LIMIT 1"
    )
    suspend fun getLastSaleLine(itemId: Long): DocumentLineEntity?

    @Query("SELECT accountId, SUM(debit) AS totalDebit, SUM(credit) AS totalCredit FROM journal_lines GROUP BY accountId")
    suspend fun getAccountBalances(): List<AccountBalance>
}
