package com.makemission.makeflow.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.makemission.makeflow.data.local.dao.CategorySpending
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pctChange returns null when prior is zero`() {
        assertNull(pctChange(324.0, 0.0))
        assertNull(pctChange(1000.0, 0.0))
        assertNull(pctChange(0.0, 0.0))
    }

    @Test
    fun `pctChange returns positive percentage when current exceeds prior`() {
        assertEquals(50.0, pctChange(150.0, 100.0)!!, 1e-9)
        assertEquals(100.0, pctChange(200.0, 100.0)!!, 1e-9)
    }

    @Test
    fun `pctChange returns negative percentage when current is less than prior`() {
        assertEquals(-50.0, pctChange(50.0, 100.0)!!, 1e-9)
        assertEquals(-100.0, pctChange(0.0, 100.0)!!, 1e-9)
    }

    @Test
    fun `pctChange returns zero when current equals prior`() {
        assertEquals(0.0, pctChange(100.0, 100.0)!!, 1e-9)
    }

    @Test
    fun `totalIncome scopes to focused account`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val now = System.currentTimeMillis()

        val incomeA = TransactionEntity(
            id = "tx_a", amount = 1000.0, type = "INCOME", categoryId = "cat1",
            accountId = "acc_a", note = "Salary", timestamp = now
        )
        val incomeB = TransactionEntity(
            id = "tx_b", amount = 500.0, type = "INCOME", categoryId = "cat1",
            accountId = "acc_b", note = "Freelance", timestamp = now
        )
        repo.setTransactions(listOf(incomeA, incomeB))

        val userPrefs = createTestUserPrefs()
        val viewModel = InsightsViewModel(repo, userPrefs)
        advanceUntilIdle()

        // Default: all income
        val allCollected = mutableListOf<Double>()
        val allJob = launch { viewModel.totalIncome.collect { allCollected.add(it) } }
        runCurrent()
        assertEquals(1500.0, allCollected.last(), 0.01)
        allJob.cancel()

        // Focus on acc_a
        viewModel.setFocusedAccountId("acc_a")
        advanceUntilIdle()

        val focusedCollected = mutableListOf<Double>()
        val focusedJob = launch { viewModel.totalIncome.collect { focusedCollected.add(it) } }
        runCurrent()
        assertEquals(1000.0, focusedCollected.last(), 0.01)
        focusedJob.cancel()
    }

    private class FakeFinanceRepository : FinanceRepository {
        private val _allTransactions = MutableStateFlow(emptyList<TransactionEntity>())
        private val _allAccounts = MutableStateFlow(emptyList<AccountEntity>())

        fun setAccounts(accounts: List<AccountEntity>) {
            _allAccounts.value = accounts
        }

        fun setTransactions(transactions: List<TransactionEntity>) {
            _allTransactions.value = transactions
        }

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = _allTransactions
        override suspend fun insertTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransaction(transaction: TransactionEntity) {}
        override fun getAllAccounts(): Flow<List<AccountEntity>> = _allAccounts
        override fun getAllCategories(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
        override fun getMonthlySpendingByCategory(monthStartMillis: Long, monthEndMillis: Long): Flow<List<CategorySpending>> = MutableStateFlow(emptyList())
        override fun getTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> = _allTransactions
        override fun getTransactionsInRangeForAccount(startMillis: Long, endMillis: Long, accountId: String): Flow<List<TransactionEntity>> {
            return _allTransactions
        }
        override fun getMonthlyTotalExpense(monthStartMillis: Long, monthEndMillis: Long): Flow<Double> = MutableStateFlow(0.0)
        override fun getMonthlyTotalIncome(monthStartMillis: Long, monthEndMillis: Long): Flow<Double> = MutableStateFlow(0.0)
        override suspend fun getMonthlyTotalExpenseOnce(monthStartMillis: Long, monthEndMillis: Long): Double = 0.0
        override suspend fun getMonthlyTotalIncomeOnce(monthStartMillis: Long, monthEndMillis: Long): Double = 0.0
        override suspend fun insertCategory(category: CategoryEntity) {}
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategory(category: CategoryEntity) {}
        override suspend fun deleteCategoryById(id: String) {}
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun insertAccount(account: AccountEntity) {}
        override suspend fun updateAccount(account: AccountEntity) {}
        override suspend fun deleteAccount(account: AccountEntity) {}
        override fun getCategoryWithTasks(id: String): Flow<com.makemission.makeflow.data.local.entity.CategoryWithTasks?> = MutableStateFlow(null)
        override suspend fun updateTaskCategory(taskId: String, categoryId: String?) {}
        override suspend fun updateTask(task: com.makemission.makeflow.data.local.entity.TaskEntity) {}
        override fun getAllBudgets(): Flow<List<BudgetEntity>> = MutableStateFlow(emptyList())
        override suspend fun insertBudget(budget: BudgetEntity) {}
        override suspend fun deleteBudget(budget: BudgetEntity) {}
        override suspend fun syncBudgetSpending(categoryId: String?, amount: Double, type: String) {}
        override suspend fun updateBudget(budgetId: String, amount: Double, period: String, name: String?) {}
        override suspend fun checkDuplicate(candidate: TransactionEntity): com.makemission.makeflow.domain.DuplicateCheckResult? = null
        override suspend fun clearAllTransactions() {}
        override suspend fun clearAllAccounts() {}
        override suspend fun clearAllBudgets() {}
        override suspend fun clearAllCategories() {}
        override suspend fun refreshRecurringPatterns() {}
        override fun getUpcomingRecurring(fromDate: Long): Flow<List<com.makemission.makeflow.data.RecurringPattern>> = MutableStateFlow(emptyList())
        override suspend fun getBudgetSuggestions(dismissedKeys: Set<String>): List<com.makemission.makeflow.domain.BudgetSuggestion> = emptyList()
        override suspend fun updateBudgetAmount(budgetId: String, newAmount: Double) {}
        override suspend fun getBudgetByCategory(categoryId: String): BudgetEntity? = null
        override suspend fun getRecentAmountsForCategory(categoryId: String, excludingTransactionId: String?, limit: Int): List<Double> = emptyList()
        override suspend fun getRecentDescriptionsAndCategories(limit: Int): List<com.makemission.makeflow.data.local.dao.DescriptionCategoryPair> = emptyList()
        override suspend fun createRecurringTransaction(entity: com.makemission.makeflow.data.local.entity.RecurringTransactionEntity) {}
        override suspend fun updateRecurringTransaction(entity: com.makemission.makeflow.data.local.entity.RecurringTransactionEntity) {}
        override suspend fun deleteRecurringTransaction(entity: com.makemission.makeflow.data.local.entity.RecurringTransactionEntity) {}
        override suspend fun toggleRecurringTransactionActive(id: String) {}
        override suspend fun skipRecurringTransaction(id: String) {}
        override suspend fun processDueRecurringTransactions() {}
        override fun getAllActiveRecurringTransactions(): Flow<List<com.makemission.makeflow.data.local.entity.RecurringTransactionEntity>> = MutableStateFlow(emptyList())
        override fun getDueRecurringTransactions(now: Long): Flow<List<com.makemission.makeflow.data.local.entity.RecurringTransactionEntity>> = MutableStateFlow(emptyList())
        override suspend fun mergeCategories(sourceId: String, targetId: String): String? = null
        override suspend fun convertCategoryType(categoryId: String, newType: String): String? = null
        override suspend fun deleteCategoryWithGuards(categoryId: String): String? = null
        override suspend fun getConvertedTotalBalance(displayCurrency: String): Double = 0.0
        override suspend fun hasExternalReference(externalRef: String): Boolean = false
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
