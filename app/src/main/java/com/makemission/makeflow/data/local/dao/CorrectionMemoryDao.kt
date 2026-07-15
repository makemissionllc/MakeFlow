package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.makemission.makeflow.data.local.entity.CorrectionMemoryEntity

@Dao
interface CorrectionMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: CorrectionMemoryEntity)

    @Query("SELECT categoryId FROM correction_memory WHERE normalizedDescription = :normalizedDescription")
    suspend fun get(normalizedDescription: String): String?

    @Query("SELECT * FROM correction_memory")
    suspend fun getAll(): List<CorrectionMemoryEntity>

    @Query("DELETE FROM correction_memory")
    suspend fun clearAll()
}
