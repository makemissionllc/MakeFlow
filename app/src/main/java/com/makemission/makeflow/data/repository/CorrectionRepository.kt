package com.makemission.makeflow.data.repository

import com.makemission.makeflow.core.ml.RetrainableCategoryClassifier
import com.makemission.makeflow.data.local.dao.CorrectionDao
import com.makemission.makeflow.data.local.dao.CorrectionMemoryDao
import com.makemission.makeflow.data.local.entity.CorrectionEntity
import com.makemission.makeflow.data.local.entity.CorrectionMemoryEntity

interface CorrectionRepository {
    suspend fun queueCorrection(
        description: String,
        correctedLabelName: String,
        correctedLabelIndex: Int,
        originalSuggestedLabelName: String?
    )
    suspend fun getUntrainedCount(): Int
    suspend fun getUntrainedCorrections(limit: Int): List<CorrectionEntity>
    suspend fun markAsTrained(id: Long)
    suspend fun resetCorrections()
    suspend fun upsertCorrectionMemory(normalizedDescription: String, categoryId: String)
    suspend fun getCorrectionMemory(normalizedDescription: String): String?
    suspend fun getAllCorrectionMemory(): Map<String, String>
    suspend fun clearCorrectionMemory()
    suspend fun trainBatchIfReady(classifier: RetrainableCategoryClassifier): Boolean
}

class CorrectionRepositoryImpl(
    private val correctionDao: CorrectionDao,
    private val correctionMemoryDao: CorrectionMemoryDao
) : CorrectionRepository {

    companion object {
        private const val MIN_BATCH_SIZE = 5
    }

    override suspend fun queueCorrection(
        description: String,
        correctedLabelName: String,
        correctedLabelIndex: Int,
        originalSuggestedLabelName: String?
    ) {
        correctionDao.insert(
            CorrectionEntity(
                description = description,
                correctedLabelName = correctedLabelName,
                correctedLabelIndex = correctedLabelIndex,
                originalSuggestedLabelName = originalSuggestedLabelName
            )
        )
    }

    override suspend fun getUntrainedCount(): Int = correctionDao.getUntrainedCount()

    override suspend fun getUntrainedCorrections(limit: Int): List<CorrectionEntity> =
        correctionDao.getUntrainedCorrections(limit)

    override suspend fun markAsTrained(id: Long) = correctionDao.markAsTrained(id)

    override suspend fun resetCorrections() = correctionDao.deleteAll()

    override suspend fun upsertCorrectionMemory(normalizedDescription: String, categoryId: String) {
        correctionMemoryDao.upsert(
            CorrectionMemoryEntity(
                normalizedDescription = normalizedDescription,
                categoryId = categoryId
            )
        )
    }

    override suspend fun getCorrectionMemory(normalizedDescription: String): String? =
        correctionMemoryDao.get(normalizedDescription)

    override suspend fun getAllCorrectionMemory(): Map<String, String> =
        correctionMemoryDao.getAll().associate { it.normalizedDescription to it.categoryId }

    override suspend fun clearCorrectionMemory() = correctionMemoryDao.clearAll()

    override suspend fun trainBatchIfReady(classifier: RetrainableCategoryClassifier): Boolean {
        val count = correctionDao.getUntrainedCount()
        if (count < MIN_BATCH_SIZE) return false

        val batch = correctionDao.getUntrainedCorrections(limit = MIN_BATCH_SIZE)
        if (batch.isEmpty()) return false

        val descriptions = batch.map { it.description }
        val labelIndices = batch.map { it.correctedLabelIndex }.toIntArray()

        val ok = classifier.train(descriptions, labelIndices)
        if (ok) {
            batch.forEach { correctionDao.markAsTrained(it.id) }
            classifier.saveCheckpoint()
        }
        return ok
    }
}
