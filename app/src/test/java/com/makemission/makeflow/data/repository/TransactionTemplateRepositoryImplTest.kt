package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.local.dao.TransactionTemplateDao
import com.makemission.makeflow.data.local.entity.TransactionTemplateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionTemplateRepositoryImplTest {

    private class FakeTransactionTemplateDao : TransactionTemplateDao {
        val entities = mutableListOf<TransactionTemplateEntity>()
        private val entitiesFlow = MutableStateFlow<List<TransactionTemplateEntity>>(emptyList())

        var insertCount = 0
        var updateCount = 0
        var deleteCount = 0

        override fun getAll(): Flow<List<TransactionTemplateEntity>> = entitiesFlow

        override suspend fun getAllOnce(): List<TransactionTemplateEntity> = entities.toList()

        override suspend fun getById(id: String): TransactionTemplateEntity? = entities.find { it.id == id }

        override suspend fun insert(entity: TransactionTemplateEntity) {
            insertCount++
            entities.add(entity)
            entitiesFlow.value = entities.toList()
        }

        override suspend fun update(entity: TransactionTemplateEntity) {
            updateCount++
            val idx = entities.indexOfFirst { it.id == entity.id }
            if (idx >= 0) entities[idx] = entity
            entitiesFlow.value = entities.toList()
        }

        override suspend fun delete(entity: TransactionTemplateEntity) {
            deleteCount++
            entities.removeAll { it.id == entity.id }
            entitiesFlow.value = entities.toList()
        }

        override suspend fun clearAll() {
            entities.clear()
            entitiesFlow.value = emptyList()
        }
    }

    @Test
    fun insertTemplate_stores_and_can_be_retrieved() = runBlocking {
        val dao = FakeTransactionTemplateDao()
        val repo = TransactionTemplateRepositoryImpl(dao)

        val template = TransactionTemplateEntity(
            id = "t1", name = "Monthly Rent", amount = 1500.0,
            type = "EXPENSE", categoryId = "cat1", accountId = "acct1", note = "rent"
        )
        repo.insertTemplate(template)

        assertEquals(1, dao.insertCount)
        val all = repo.getAllTemplatesOnce()
        assertEquals(1, all.size)
        assertEquals("Monthly Rent", all[0].name)
    }

    @Test
    fun insert_and_getById_returns_correct_entity() = runBlocking {
        val dao = FakeTransactionTemplateDao()
        val repo = TransactionTemplateRepositoryImpl(dao)

        val template = TransactionTemplateEntity(
            id = "t1", name = "Freelance Income", amount = 3000.0,
            type = "INCOME", categoryId = null, accountId = "acct1", note = ""
        )
        repo.insertTemplate(template)

        val fetched = repo.getTemplateById("t1")!!
        assertEquals("Freelance Income", fetched.name)
        assertEquals(3000.0, fetched.amount, 0.001)
        assertEquals("INCOME", fetched.type)
    }

    @Test
    fun updateTemplate_modifies_existing_entity() = runBlocking {
        val dao = FakeTransactionTemplateDao()
        val repo = TransactionTemplateRepositoryImpl(dao)

        val template = TransactionTemplateEntity(
            id = "t1", name = "Old Name", amount = 100.0,
            type = "EXPENSE", categoryId = "cat1", accountId = "acct1", note = "old"
        )
        repo.insertTemplate(template)

        repo.updateTemplate(template.copy(name = "New Name", note = "updated"))

        assertEquals(1, dao.updateCount)
        val fetched = repo.getTemplateById("t1")
        assertEquals("New Name", fetched?.name)
        assertEquals("updated", fetched?.note)
    }

    @Test
    fun deleteTemplate_removes_entity() = runBlocking {
        val dao = FakeTransactionTemplateDao()
        val repo = TransactionTemplateRepositoryImpl(dao)

        val template = TransactionTemplateEntity(
            id = "t1", name = "Temp", amount = 50.0,
            type = "EXPENSE", categoryId = null, accountId = "acct1", note = ""
        )
        repo.insertTemplate(template)
        assertEquals(1, repo.getAllTemplatesOnce().size)

        repo.deleteTemplate(template)

        assertEquals(1, dao.deleteCount)
        assertEquals(0, repo.getAllTemplatesOnce().size)
        assertNull(repo.getTemplateById("t1"))
    }

    @Test
    fun getAllTemplates_flow_emits_inserted_templates() = runBlocking {
        val dao = FakeTransactionTemplateDao()
        val repo = TransactionTemplateRepositoryImpl(dao)

        val t1 = TransactionTemplateEntity(
            id = "t1", name = "A", amount = 10.0,
            type = "EXPENSE", categoryId = null, accountId = "acct1", note = ""
        )
        val t2 = TransactionTemplateEntity(
            id = "t2", name = "B", amount = 20.0,
            type = "INCOME", categoryId = null, accountId = "acct1", note = ""
        )
        repo.insertTemplate(t1)
        repo.insertTemplate(t2)

        val emitted = repo.getAllTemplates().first()
        assertEquals(2, emitted.size)
    }

    @Test
    fun clearAll_removes_all_templates() = runBlocking {
        val dao = FakeTransactionTemplateDao()
        val repo = TransactionTemplateRepositoryImpl(dao)

        repo.insertTemplate(
            TransactionTemplateEntity(
                id = "t1", name = "X", amount = 1.0,
                type = "EXPENSE", categoryId = null, accountId = "acct1", note = ""
            )
        )
        repo.insertTemplate(
            TransactionTemplateEntity(
                id = "t2", name = "Y", amount = 2.0,
                type = "INCOME", categoryId = null, accountId = "acct1", note = ""
            )
        )

        repo.clearAllTemplates()

        assertEquals(0, repo.getAllTemplatesOnce().size)
        assertEquals(0, dao.entities.size)
    }
}
