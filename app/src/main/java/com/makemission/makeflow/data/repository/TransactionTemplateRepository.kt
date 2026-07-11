package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.local.dao.TransactionTemplateDao
import com.makemission.makeflow.data.local.entity.TransactionTemplateEntity
import kotlinx.coroutines.flow.Flow

interface TransactionTemplateRepository {
    fun getAllTemplates(): Flow<List<TransactionTemplateEntity>>
    suspend fun getAllTemplatesOnce(): List<TransactionTemplateEntity>
    suspend fun getTemplateById(id: String): TransactionTemplateEntity?
    suspend fun insertTemplate(entity: TransactionTemplateEntity)
    suspend fun updateTemplate(entity: TransactionTemplateEntity)
    suspend fun deleteTemplate(entity: TransactionTemplateEntity)
    suspend fun clearAllTemplates()
}

class TransactionTemplateRepositoryImpl(
    private val dao: TransactionTemplateDao
) : TransactionTemplateRepository {
    override fun getAllTemplates(): Flow<List<TransactionTemplateEntity>> = dao.getAll()

    override suspend fun getAllTemplatesOnce(): List<TransactionTemplateEntity> = dao.getAllOnce()

    override suspend fun getTemplateById(id: String): TransactionTemplateEntity? = dao.getById(id)

    override suspend fun insertTemplate(entity: TransactionTemplateEntity) = dao.insert(entity)

    override suspend fun updateTemplate(entity: TransactionTemplateEntity) = dao.update(entity)

    override suspend fun deleteTemplate(entity: TransactionTemplateEntity) = dao.delete(entity)

    override suspend fun clearAllTemplates() = dao.clearAll()
}
