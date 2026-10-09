package com.makemission.makeflow.ui.viewmodel

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsCsvExportTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val savedFiles = mutableListOf<Uri>()
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepositoryImpl
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = FinanceRepositoryImpl(database.categoryDao(), database.transactionDao(), database.taskDao(), database.accountDao())
        val dataStore = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                return transform(data.value).also { data.value = it }
            }
        }
        viewModel = SettingsViewModel(repository, UserPreferencesDataStore(dataStore))
    }

    @After
    fun tearDown() {
        savedFiles.forEach { context.contentResolver.delete(it, null, null) }
        database.close()
    }

    @Test
    fun exportsPersistedRowsAndExcludesExclusiveEnd() = runBlocking {
        repository.insertAccount(AccountEntity("cash", "Test wallet", 0.0, 0.0, currencyCode = "KES"))
        repository.insertTransaction(TransactionEntity(id = "in-range", amount = 5.0, type = "INCOME", accountId = "cash", note = "Included", timestamp = 1000L))
        repository.insertTransaction(TransactionEntity(id = "at-end", amount = 8.0, type = "INCOME", accountId = "cash", note = "Excluded", timestamp = 2000L))
        val result = CompletableDeferred<Uri>()
        instrumentation.runOnMainSync {
            viewModel.exportCsv(context, 1000L, 2000L,
                onError = { result.completeExceptionally(AssertionError("Export failed")) },
                onComplete = { uri, _ -> savedFiles.add(uri); result.complete(uri) }
            )
        }
        val uri = withTimeout(10000L) { result.await() }
        val csv = context.contentResolver.openInputStream(uri)!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
        assertTrue(csv.startsWith("\uFEFFDate,Type,Amount,Currency"))
        assertTrue(csv.contains("\"Test wallet\""))
        assertTrue(csv.contains("\"Included\""))
        assertFalse(csv.contains("\"Excluded\""))
        instrumentation.runOnMainSync { assertFalse(viewModel.isExportingCsv.value) }
    }

    @Test
    fun overlappingRequestsProduceOneCompletedFile() = runBlocking {
        val result = CompletableDeferred<Unit>()
        var completions = 0
        instrumentation.runOnMainSync {
            repeat(2) {
                viewModel.exportCsv(context,
                    onError = { result.completeExceptionally(AssertionError("Export failed")) },
                    onComplete = { uri, _ -> savedFiles.add(uri); completions++; result.complete(Unit) }
                )
            }
            assertTrue(viewModel.isExportingCsv.value)
        }
        withTimeout(10000L) { result.await() }
        instrumentation.runOnMainSync {
            assertEquals(1, completions)
            assertFalse(viewModel.isExportingCsv.value)
        }
    }

    @Test
    fun invalidRangeReportsErrorAndClearsBusyState() = runBlocking {
        val failed = CompletableDeferred<Unit>()
        var completed = false
        instrumentation.runOnMainSync {
            viewModel.exportCsv(context, 2000L, 1000L,
                onError = { failed.complete(Unit) },
                onComplete = { uri, _ -> savedFiles.add(uri); completed = true }
            )
        }
        withTimeout(10000L) { failed.await() }
        instrumentation.runOnMainSync {
            assertFalse(completed)
            assertFalse(viewModel.isExportingCsv.value)
        }
    }
}
