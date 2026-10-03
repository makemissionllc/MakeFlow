package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.makemission.makeflow.data.local.entity.CorrectionEntity

@Dao
interface CorrectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(correction: CorrectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(corrections: List<CorrectionEntity>)

    @Query("SELECT * FROM corrections WHERE isTrained = 0 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getUntrainedCorrections(limit: Int): List<CorrectionEntity>

    @Query("SELECT COUNT(*) FROM corrections WHERE isTrained = 0")
    suspend fun getUntrainedCount(): Int

    @Query("UPDATE corrections SET isTrained = 1 WHERE id = :id")
    suspend fun markAsTrained(id: Long)

    @Query("DELETE FROM corrections")
    suspend fun deleteAll()
}
