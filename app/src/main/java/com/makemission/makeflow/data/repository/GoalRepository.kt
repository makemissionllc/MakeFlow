package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.local.dao.GoalDao
import com.makemission.makeflow.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>
    fun getActiveGoals(): Flow<List<SavingsGoalEntity>>
    suspend fun getGoalById(id: String): SavingsGoalEntity?
    suspend fun insert(goal: SavingsGoalEntity)
    suspend fun update(goal: SavingsGoalEntity)
    suspend fun delete(goal: SavingsGoalEntity)
    suspend fun updateStatus(id: String, status: String)
    suspend fun getActiveGoalCountForAccount(accountId: String): Int
    suspend fun clearAll()
}

class GoalRepositoryImpl(
    private val goalDao: GoalDao
) : GoalRepository {
    override fun getAllGoals(): Flow<List<SavingsGoalEntity>> = goalDao.getAllGoals()

    override fun getActiveGoals(): Flow<List<SavingsGoalEntity>> = goalDao.getActiveGoals()

    override suspend fun getGoalById(id: String): SavingsGoalEntity? = goalDao.getGoalById(id)

    override suspend fun insert(goal: SavingsGoalEntity) = goalDao.insert(goal)

    override suspend fun update(goal: SavingsGoalEntity) = goalDao.update(goal)

    override suspend fun delete(goal: SavingsGoalEntity) = goalDao.delete(goal)

    override suspend fun updateStatus(id: String, status: String) = goalDao.updateStatus(id, status)

    override suspend fun getActiveGoalCountForAccount(accountId: String): Int =
        goalDao.getActiveGoalCountForAccount(accountId)

    override suspend fun clearAll() = goalDao.clearAll()
}
