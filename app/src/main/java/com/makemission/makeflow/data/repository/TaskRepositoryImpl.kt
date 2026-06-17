package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.local.dao.TaskDao
import com.makemission.makeflow.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    override suspend fun insert(task: TaskEntity) = taskDao.insert(task)

    override suspend fun update(task: TaskEntity) = taskDao.update(task)

    override suspend fun delete(task: TaskEntity) = taskDao.delete(task)
}
