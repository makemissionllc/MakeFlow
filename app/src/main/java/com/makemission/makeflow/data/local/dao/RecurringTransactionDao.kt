package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.makemission.makeflow.data.local.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getAllActive(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextDueDate <= :now ORDER BY nextDueDate ASC")
    fun getDueTodayOrOverdue(now: Long): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextDueDate <= :now ORDER BY nextDueDate ASC")
    suspend fun getDueTodayOrOverdueOnce(now: Long): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: String): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAllOnce(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1")
    suspend fun getAllActiveOnce(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecurringTransactionEntity)

    @Update
    suspend fun update(entity: RecurringTransactionEntity)

    @Delete
    suspend fun delete(entity: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions")
    suspend fun clearAll()
}
