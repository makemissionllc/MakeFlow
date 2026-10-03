package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.makemission.makeflow.data.local.entity.AppLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLogDao {

    @Insert
    suspend fun insert(log: AppLogEntity)

    @Insert
    fun insertSync(log: AppLogEntity)

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<AppLogEntity>>

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC")
    suspend fun getAll(): List<AppLogEntity>

    @Query("SELECT * FROM app_logs WHERE level = :level ORDER BY timestamp DESC")
    fun getByLevelFlow(level: String): Flow<List<AppLogEntity>>

    @Query("DELETE FROM app_logs WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM app_logs")
    suspend fun count(): Int

    @Query("DELETE FROM app_logs WHERE id NOT IN (SELECT id FROM app_logs ORDER BY timestamp DESC LIMIT :keep)")
    suspend fun pruneToMostRecent(keep: Int)

    @Query("DELETE FROM app_logs")
    suspend fun clearAll()
}
