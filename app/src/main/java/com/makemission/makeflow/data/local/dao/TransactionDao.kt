package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.makemission.makeflow.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class MonthlyCategorySpend(
    val yearMonth: String,
    val totalSpent: Double
)

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query("""
        SELECT strftime('%Y-%m', timestamp / 1000.0, 'unixepoch') AS yearMonth,
               COALESCE(SUM(amount), 0) AS totalSpent
        FROM transactions
        WHERE categoryId = :categoryId
          AND type = 'EXPENSE'
          AND timestamp >= :startMillis
          AND timestamp <= :endMillis
        GROUP BY yearMonth
        ORDER BY yearMonth ASC
    """)
    suspend fun getCategorySpendByMonth(
        categoryId: String,
        startMillis: Long,
        endMillis: Long
    ): List<MonthlyCategorySpend>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND timestamp >= :monthStartMillis AND timestamp <= :monthEndMillis")
    fun getMonthlyTotalExpense(monthStartMillis: Long, monthEndMillis: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND timestamp >= :monthStartMillis AND timestamp <= :monthEndMillis")
    suspend fun getMonthlyTotalExpenseOnce(monthStartMillis: Long, monthEndMillis: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND timestamp >= :monthStartMillis AND timestamp <= :monthEndMillis")
    fun getMonthlyTotalIncome(monthStartMillis: Long, monthEndMillis: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND timestamp >= :monthStartMillis AND timestamp <= :monthEndMillis")
    suspend fun getMonthlyTotalIncomeOnce(monthStartMillis: Long, monthEndMillis: Long): Double

    @Query("SELECT * FROM transactions WHERE timestamp >= :startMillis AND timestamp <= :endMillis ORDER BY timestamp ASC")
    fun getTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE timestamp >= :startMillis AND timestamp <= :endMillis
          AND (accountId = :accountId OR fromAccountId = :accountId OR toAccountId = :accountId)
        ORDER BY timestamp ASC
    """)
    fun getTransactionsInRangeForAccount(startMillis: Long, endMillis: Long, accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT amount FROM transactions WHERE categoryId = :categoryId AND type = 'EXPENSE' AND id != COALESCE(:excludingTransactionId, '') ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentAmountsForCategory(categoryId: String, excludingTransactionId: String? = null, limit: Int = 30): List<Double>

    @Query("SELECT note AS description, categoryId FROM transactions WHERE note IS NOT NULL AND note != '' AND categoryId IS NOT NULL ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentDescriptionsAndCategories(limit: Int = 100): List<DescriptionCategoryPair>

    @Query("SELECT * FROM transactions WHERE timestamp >= :sinceMillis ORDER BY timestamp DESC")
    suspend fun getTransactionsSince(sinceMillis: Long): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE externalReference = :externalRef LIMIT 1)")
    suspend fun hasExternalReference(externalRef: String): Boolean
}

data class DescriptionCategoryPair(
    val description: String,
    val categoryId: String
)
