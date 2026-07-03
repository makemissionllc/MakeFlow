package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.makemission.makeflow.data.local.entity.RecurringPatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringPatternDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(patterns: List<RecurringPatternEntity>)

    @Query("SELECT * FROM recurring_patterns")
    fun getAll(): Flow<List<RecurringPatternEntity>>

    @Query("DELETE FROM recurring_patterns")
    suspend fun deleteAll()

    @Query("SELECT * FROM recurring_patterns WHERE nextExpectedDate >= :fromDate ORDER BY nextExpectedDate ASC")
    fun getUpcoming(fromDate: Long): Flow<List<RecurringPatternEntity>>

    @Query("SELECT * FROM recurring_patterns")
    suspend fun getAllOnce(): List<RecurringPatternEntity>

    @Query("UPDATE recurring_patterns SET lastNotifiedNextDate = :nextExpectedDate WHERE id = :id")
    suspend fun updateLastNotifiedNextDate(id: Long, nextExpectedDate: Long)
}
