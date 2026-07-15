package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.local.dao.CorrectionDao
import com.makemission.makeflow.data.local.dao.CorrectionMemoryDao
import com.makemission.makeflow.data.local.entity.CorrectionEntity
import com.makemission.makeflow.data.local.entity.CorrectionMemoryEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CorrectionRepositoryTest {

    private lateinit var repo: CorrectionRepositoryImpl
    private lateinit var fakeDao: FakeCorrectionDao
    private lateinit var fakeMemoryDao: FakeCorrectionMemoryDao

    @Before
    fun setUp() {
        fakeDao = FakeCorrectionDao()
        fakeMemoryDao = FakeCorrectionMemoryDao()
        repo = CorrectionRepositoryImpl(fakeDao, fakeMemoryDao)
    }

    @Test
    fun queueCorrection_insertsIntoDao() = runTest {
        repo.queueCorrection("test desc", "Fuel", 3, null)
        assertEquals(1, fakeDao.corrections.size)
        assertEquals("test desc", fakeDao.corrections[0].description)
        assertEquals("Fuel", fakeDao.corrections[0].correctedLabelName)
        assertEquals(3, fakeDao.corrections[0].correctedLabelIndex)
    }

    @Test
    fun untrainedCount_delegatesToDao() = runTest {
        repo.queueCorrection("a", "Fuel", 3, null)
        repo.queueCorrection("b", "Fuel", 3, null)
        val count = repo.getUntrainedCount()
        assertEquals(2, count)
    }

    @Test
    fun upsertCorrectionMemory_storesInDao() = runTest {
        repo.upsertCorrectionMemory("safari", "cat_id")
        assertEquals("cat_id", repo.getCorrectionMemory("safari"))
    }

    @Test
    fun getCorrectionMemory_missing_returnsNull() = runTest {
        val result = repo.getCorrectionMemory("nonexistent")
        assertNull(result)
    }

    @Test
    fun getAllCorrectionMemory_returnsMap() = runTest {
        repo.upsertCorrectionMemory("a", "id_a")
        repo.upsertCorrectionMemory("b", "id_b")
        val map = repo.getAllCorrectionMemory()
        assertEquals(2, map.size)
        assertEquals("id_a", map["a"])
        assertEquals("id_b", map["b"])
    }

    @Test
    fun clearCorrectionMemory_emptiesTable() = runTest {
        repo.upsertCorrectionMemory("a", "id_a")
        repo.clearCorrectionMemory()
        assertTrue(repo.getAllCorrectionMemory().isEmpty())
    }

    @Test
    fun resetCorrections_deletesAll() = runTest {
        repo.queueCorrection("a", "Fuel", 3, null)
        repo.resetCorrections()
        assertEquals(0, repo.getUntrainedCount())
    }

    @Test
    fun markAsTrained_setsFlag() = runTest {
        repo.queueCorrection("a", "Fuel", 3, null)
        val id = fakeDao.corrections[0].id
        repo.markAsTrained(id)
        assertTrue(fakeDao.corrections[0].isTrained)
    }
}

private class FakeCorrectionDao : CorrectionDao {

    val corrections = mutableListOf<CorrectionEntity>()
    private var nextId = 1L

    override suspend fun insert(correction: CorrectionEntity) {
        corrections.add(correction.copy(id = nextId++))
    }

    override suspend fun insertAll(corrections: List<CorrectionEntity>) {
        corrections.forEach { insert(it) }
    }

    override suspend fun getUntrainedCorrections(limit: Int): List<CorrectionEntity> =
        corrections.filter { !it.isTrained }.take(limit)

    override suspend fun getUntrainedCount(): Int =
        corrections.count { !it.isTrained }

    override suspend fun markAsTrained(id: Long) {
        val idx = corrections.indexOfFirst { it.id == id }
        if (idx >= 0) corrections[idx] = corrections[idx].copy(isTrained = true)
    }

    override suspend fun deleteAll() {
        corrections.clear()
    }
}

private class FakeCorrectionMemoryDao : CorrectionMemoryDao {

    private val store = mutableMapOf<String, CorrectionMemoryEntity>()

    override suspend fun upsert(memory: CorrectionMemoryEntity) {
        store[memory.normalizedDescription] = memory
    }

    override suspend fun get(normalizedDescription: String): String? =
        store[normalizedDescription]?.categoryId

    override suspend fun getAll(): List<CorrectionMemoryEntity> =
        store.values.toList()

    override suspend fun clearAll() {
        store.clear()
    }
}
