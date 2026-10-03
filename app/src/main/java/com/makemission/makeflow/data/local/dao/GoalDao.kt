package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.makemission.makeflow.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity)

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)

    @Query("SELECT * FROM savings_goals ORDER BY createdDate DESC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE status = 'ACTIVE' ORDER BY createdDate DESC")
    fun getActiveGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoalById(id: String): SavingsGoalEntity?

    @Query("SELECT COUNT(*) FROM savings_goals WHERE linkedAccountId = :accountId AND status = 'ACTIVE'")
    suspend fun getActiveGoalCountForAccount(accountId: String): Int

    @Query("UPDATE savings_goals SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllOnce(): List<SavingsGoalEntity>

    @Query("DELETE FROM savings_goals")
    suspend fun clearAll()
}
