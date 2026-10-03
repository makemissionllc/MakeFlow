package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.makemission.makeflow.data.local.entity.QueuedNotificationEntity

@Dao
interface QueuedNotificationDao {
    @Insert
    suspend fun insert(entity: QueuedNotificationEntity)

    @Query("SELECT * FROM queued_notifications ORDER BY createdAt ASC")
    suspend fun getAll(): List<QueuedNotificationEntity>

    @Query("DELETE FROM queued_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM queued_notifications")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM queued_notifications")
    suspend fun count(): Int

    @Query("DELETE FROM queued_notifications WHERE id IN (SELECT id FROM queued_notifications ORDER BY createdAt ASC LIMIT :excessCount)")
    suspend fun trimOldest(excessCount: Int)
}
