package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.makemission.makeflow.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class TransactionWithDetails(
    val transactionId: Long,
    val amount: Double,
    val type: String,
    val note: String?,
    val timestamp: Long,
    val accountId: Long,
    val categoryId: Long?,
    val accountName: String,
    val categoryName: String?
)

@Dao
interface TransactionDao {

    @Query("""
        SELECT t.id AS transactionId, t.amount, t.type, t.note, t.timestamp,
               t.accountId, t.categoryId, a.name AS accountName, c.name AS categoryName
        FROM transactions t
        INNER JOIN accounts a ON t.accountId = a.id
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.timestamp DESC
    """)
    fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>
}
