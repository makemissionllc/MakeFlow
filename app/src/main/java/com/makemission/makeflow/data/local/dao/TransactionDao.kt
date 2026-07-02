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

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND timestamp >= :monthStartMillis AND timestamp <= :monthEndMillis")
    fun getMonthlyTotalIncome(monthStartMillis: Long, monthEndMillis: Long): Flow<Double>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startMillis AND timestamp <= :endMillis ORDER BY timestamp ASC")
    fun getTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
