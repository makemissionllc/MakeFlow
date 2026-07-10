package com.makemission.makeflow.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createTransaction(
        id: String = "tx1",
        amount: Double = 50.0,
        type: String = "EXPENSE",
        categoryId: String? = "cat1",
        note: String = "coffee"
    ) = TransactionEntity(
        id = id, amount = amount, type = type,
        categoryId = categoryId, accountId = "acct1",
        note = note, timestamp = 1000L
    )

    @Test
    fun deleteTransaction_removesItFromDisplayedList() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val tx1 = createTransaction("tx1")
        val tx2 = createTransaction("tx2", amount = 30.0, note = "tea")
        repo.addTransaction(tx1)
        repo.addTransaction(tx2)

        val viewModel = HistoryViewModel(repo, createTestUserPrefs())
        val emitted = mutableListOf<List<TransactionEntity>>()
        val job = launch { viewModel.transactions.collect { emitted.add(it) } }
        runCurrent()

        assertEquals(2, emitted.last().size)

        viewModel.deleteTransaction(tx1)
        runCurrent()

        assertEquals(1, emitted.last().size)
        assertEquals("tx2", emitted.last()[0].id)
        job.cancel()
    }

    @Test
    fun undoDelete_reinsertsSameTransaction() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val tx = createTransaction("tx1")
        repo.addTransaction(tx)

        val viewModel = HistoryViewModel(repo, createTestUserPrefs())
        val emitted = mutableListOf<List<TransactionEntity>>()
        val job = launch { viewModel.transactions.collect { emitted.add(it) } }
        runCurrent()

        assertEquals(1, emitted.last().size)

        viewModel.deleteTransaction(tx)
        runCurrent()
        assertEquals(0, emitted.last().size)
        assertNotNull(viewModel.recentlyDeleted.value)

        viewModel.undoDelete()
        runCurrent()

        assertEquals(1, emitted.last().size)
        val restored = emitted.last()[0]
        assertEquals(tx.id, restored.id)
        assertEquals(tx.amount, restored.amount, 0.001)
        assertEquals(tx.categoryId, restored.categoryId)
        assertEquals(tx.timestamp, restored.timestamp)
        assertEquals(tx.note, restored.note)
        job.cancel()
    }

    @Test
    fun undoOverwrittenBySubsequentDelete() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val tx1 = createTransaction("tx1", note = "coffee")
        val tx2 = createTransaction("tx2", amount = 30.0, note = "tea")
        repo.addTransaction(tx1)
        repo.addTransaction(tx2)

        val viewModel = HistoryViewModel(repo, createTestUserPrefs())
        val emitted = mutableListOf<List<TransactionEntity>>()
        val job = launch { viewModel.transactions.collect { emitted.add(it) } }
        runCurrent()

        viewModel.deleteTransaction(tx1)
        runCurrent()
        assertEquals("tx1", viewModel.recentlyDeleted.value?.id)

        viewModel.deleteTransaction(tx2)
        runCurrent()
        assertEquals("tx2", viewModel.recentlyDeleted.value?.id)

        viewModel.undoDelete()
        runCurrent()

        assertEquals(1, emitted.last().size)
        assertEquals("tx2", emitted.last()[0].id)
        job.cancel()
    }

    @Test
    fun filterRespectedAfterDeleteUndoCycle() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val income = createTransaction("tx1", amount = 500.0, type = "INCOME", note = "salary")
        val expense = createTransaction("tx2", amount = 20.0, type = "EXPENSE", note = "snacks")
        repo.addTransaction(income)
        repo.addTransaction(expense)

        val viewModel = HistoryViewModel(repo, createTestUserPrefs())
        val txEmitted = mutableListOf<List<TransactionEntity>>()
        val filterEmitted = mutableListOf<List<TransactionEntity>>()
        val job1 = launch { viewModel.transactions.collect { txEmitted.add(it) } }
        val job2 = launch { viewModel.filteredTransactions.collect { filterEmitted.add(it) } }
        runCurrent()

        viewModel.selectedFilter.value = "INCOME"
        runCurrent()
        assertEquals(1, filterEmitted.last().size)
        assertEquals("tx1", filterEmitted.last()[0].id)

        viewModel.selectedFilter.value = "EXPENSE"
        runCurrent()
        assertEquals(1, filterEmitted.last().size)
        assertEquals("tx2", filterEmitted.last()[0].id)

        viewModel.deleteTransaction(expense)
        runCurrent()

        assertEquals(0, filterEmitted.last().size)

        viewModel.undoDelete()
        runCurrent()

        assertEquals(1, filterEmitted.last().size)
        assertEquals("tx2", filterEmitted.last()[0].id)

        viewModel.selectedFilter.value = "ALL"
        runCurrent()
        assertEquals(2, filterEmitted.last().size)

        job1.cancel()
        job2.cancel()
    }

    private class FakeFinanceRepository : FinanceRepository {
        private val transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())

        fun addTransaction(tx: TransactionEntity) {
            transactions.value = transactions.value + tx
        }

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactions

        override suspend fun insertTransaction(transaction: TransactionEntity) {
            transactions.value = transactions.value + transaction
        }

        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            transactions.value = transactions.value.filter { it.id != transaction.id }
        }

        override fun getAllAccounts(): Flow<List<com.makemission.makeflow.data.local.entity.AccountEntity>> =
            MutableStateFlow(emptyList())

        override fun getAllCategories(): Flow<List<com.makemission.makeflow.data.local.entity.CategoryEntity>> =
            MutableStateFlow(emptyList())

        override fun getMonthlySpendingByCategory(
            monthStartMillis: Long, monthEndMillis: Long
        ): Flow<List<com.makemission.makeflow.data.local.dao.CategorySpending>> =
            MutableStateFlow(emptyList())

        override fun getTransactionsInRange(
            startMillis: Long, endMillis: Long
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override fun getMonthlyTotalExpense(monthStartMillis: Long, monthEndMillis: Long): Flow<Double> =
            MutableStateFlow(0.0)

        override fun getMonthlyTotalIncome(monthStartMillis: Long, monthEndMillis: Long): Flow<Double> =
            MutableStateFlow(0.0)

        override suspend fun insertCategory(category: com.makemission.makeflow.data.local.entity.CategoryEntity) {}
        override suspend fun updateCategory(category: com.makemission.makeflow.data.local.entity.CategoryEntity) {}
        override suspend fun deleteCategory(category: com.makemission.makeflow.data.local.entity.CategoryEntity) {}
        override suspend fun deleteCategoryById(id: String) {}
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun insertAccount(account: com.makemission.makeflow.data.local.entity.AccountEntity) {}
        override suspend fun updateAccount(account: com.makemission.makeflow.data.local.entity.AccountEntity) {}
        override suspend fun deleteAccount(account: com.makemission.makeflow.data.local.entity.AccountEntity) {}

        override fun getCategoryWithTasks(
            id: String
        ): Flow<com.makemission.makeflow.data.local.entity.CategoryWithTasks?> = MutableStateFlow(null)

        override suspend fun updateTaskCategory(taskId: String, categoryId: String?) {}
        override suspend fun updateTask(task: com.makemission.makeflow.data.local.entity.TaskEntity) {}
        override fun getAllBudgets(): Flow<List<com.makemission.makeflow.data.local.entity.BudgetEntity>> =
            MutableStateFlow(emptyList())

        override suspend fun insertBudget(budget: com.makemission.makeflow.data.local.entity.BudgetEntity) {}
        override suspend fun deleteBudget(budget: com.makemission.makeflow.data.local.entity.BudgetEntity) {}
        override suspend fun syncBudgetSpending(categoryId: String?, amount: Double, type: String) {}
        override suspend fun clearAllTransactions() {}
        override suspend fun clearAllAccounts() {}
        override suspend fun clearAllBudgets() {}
        override suspend fun clearAllCategories() {}
        override suspend fun refreshRecurringPatterns() {}
        override fun getUpcomingRecurring(fromDate: Long): Flow<List<com.makemission.makeflow.data.RecurringPattern>> =
            MutableStateFlow(emptyList())
        override suspend fun getBudgetSuggestions(dismissedKeys: Set<String>): List<com.makemission.makeflow.domain.BudgetSuggestion> = emptyList()
        override suspend fun updateBudgetAmount(budgetId: String, newAmount: Double) {}
        override suspend fun getBudgetByCategory(categoryId: String): com.makemission.makeflow.data.local.entity.BudgetEntity? = null
        override suspend fun getRecentAmountsForCategory(categoryId: String, excludingTransactionId: String?, limit: Int): List<Double> = emptyList()
        override suspend fun getRecentDescriptionsAndCategories(limit: Int): List<com.makemission.makeflow.data.local.dao.DescriptionCategoryPair> = emptyList()
    }

    private fun createTestUserPrefs(): UserPreferencesDataStore {
        val dataStore = object : DataStore<Preferences> {
            private val _data = MutableStateFlow(emptyPreferences())
            override val data: Flow<Preferences> = _data
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                val newValue = transform(_data.value)
                _data.value = newValue
                return newValue
            }
        }
        return UserPreferencesDataStore(dataStore)
    }
}
