package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.makemission.makeflow.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET categoryId = :categoryId WHERE id = :taskId")
    suspend fun updateTaskCategory(taskId: String, categoryId: String?)

    @Query("SELECT * FROM tasks")
    suspend fun getAllOnce(): List<TaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}
