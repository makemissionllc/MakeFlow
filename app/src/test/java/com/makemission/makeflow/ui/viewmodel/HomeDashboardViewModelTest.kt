package com.makemission.makeflow.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.makemission.makeflow.data.RecurringPattern
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.dao.CardHourCount
import com.makemission.makeflow.data.local.dao.CardInteractionDao
import com.makemission.makeflow.data.local.dao.CategorySpending
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CardInteractionEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.RecurringTransactionEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.ExchangeRateRepository
import com.makemission.makeflow.data.repository.FinanceRepository
import com.makemission.makeflow.domain.DashboardCardType
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val defaultOrder = listOf(
        DashboardCardType.INSIGHTS_SUMMARY,
        DashboardCardType.ACCOUNTS,
        DashboardCardType.BUDGET_PROGRESS,
        DashboardCardType.RECURRING
    )

    private fun cardTypeByName(name: String) = DashboardCardType.valueOf(name)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `manual card order is used verbatim when stored`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val dao = FakeCardInteractionDao()
        val userPrefs = createTestUserPrefs()

        userPrefs.saveManualCardOrder(
            listOf("RECURRING", "BUDGET_PROGRESS", "ACCOUNTS", "INSIGHTS_SUMMARY")
        )

        val viewModel = HomeDashboardViewModel(repo, userPrefs, dao)

        val collected = mutableListOf<List<DashboardCardType>>()
        val job = launch { viewModel.cardOrder.collect { collected.add(it) } }
        runCurrent()

        assertEquals(1, collected.size)
        assertEquals(
            listOf(
                DashboardCardType.RECURRING,
                DashboardCardType.BUDGET_PROGRESS,
                DashboardCardType.ACCOUNTS,
                DashboardCardType.INSIGHTS_SUMMARY
            ),
            collected.last()
        )
        job.cancel()
    }

    // Generate counts at every hour so the ordering is deterministic
    // regardless of what Calendar.HOUR_OF_DAY returns at test time.
    // In any 7-hour window, BUDGET_PROGRESS gets 3*7=21, ACCOUNTS gets 2*7=14,
    // INSIGHTS_SUMMARY gets 1*7=7, RECURRING gets 0.
    private fun deterministicCountsForRanker(): List<CardHourCount> =
        (0..23).flatMap { hour ->
            listOf(
                CardHourCount("BUDGET_PROGRESS", hour, 3),
                CardHourCount("ACCOUNTS", hour, 2),
                CardHourCount("INSIGHTS_SUMMARY", hour, 1),
            )
        }

    @Test
    fun `falls back to ranker order when manual order is absent`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val dao = FakeCardInteractionDao()
        dao.addCounts(deterministicCountsForRanker())
        val userPrefs = createTestUserPrefs()

        val viewModel = HomeDashboardViewModel(repo, userPrefs, dao)

        val collected = mutableListOf<List<DashboardCardType>>()
        val job = launch { viewModel.cardOrder.collect { collected.add(it) } }
        runCurrent()

        assertEquals(1, collected.size)
        assertEquals(
            listOf(
                DashboardCardType.BUDGET_PROGRESS,
                DashboardCardType.ACCOUNTS,
                DashboardCardType.INSIGHTS_SUMMARY,
                DashboardCardType.RECURRING
            ),
            collected.last()
        )
        job.cancel()
    }

    @Test
    fun `clearManualCardOrder reverts to ranker order`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val dao = FakeCardInteractionDao()
        dao.addCounts(deterministicCountsForRanker())
        val userPrefs = createTestUserPrefs()

        userPrefs.saveManualCardOrder(
            listOf("RECURRING", "BUDGET_PROGRESS", "ACCOUNTS", "INSIGHTS_SUMMARY")
        )

        val viewModel = HomeDashboardViewModel(repo, userPrefs, dao)
        runCurrent()

        viewModel.clearManualCardOrder()
        runCurrent()

        val order = viewModel.cardOrder.first()
        assertEquals(
            listOf(
                DashboardCardType.BUDGET_PROGRESS,
                DashboardCardType.ACCOUNTS,
                DashboardCardType.INSIGHTS_SUMMARY,
                DashboardCardType.RECURRING
            ),
            order
        )
    }

    private class FakeFinanceRepository : FinanceRepository {
        override fun getAllTransactions(): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
        override suspend fun insertTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransaction(transaction: TransactionEntity) {}
        override fun getAllAccounts(): Flow<List<AccountEntity>> = MutableStateFlow(emptyList())
        override fun getAllCategories(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
        override fun getMonthlySpendingByCategory(monthStartMillis: Long, monthEndMillis: Long): Flow<List<CategorySpending>> = MutableStateFlow(emptyList())
        override fun getTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
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
        override suspend fun checkDuplicate(candidate: com.makemission.makeflow.data.local.entity.TransactionEntity): com.makemission.makeflow.domain.DuplicateCheckResult? = null
        override suspend fun clearAllTransactions() {}
        override suspend fun clearAllAccounts() {}
        override suspend fun clearAllBudgets() {}
        override suspend fun clearAllCategories() {}
        override suspend fun refreshRecurringPatterns() {}
        override fun getUpcomingRecurring(fromDate: Long): Flow<List<RecurringPattern>> = MutableStateFlow(emptyList())
        override suspend fun getBudgetSuggestions(dismissedKeys: Set<String>): List<com.makemission.makeflow.domain.BudgetSuggestion> = emptyList()
        override suspend fun updateBudgetAmount(budgetId: String, newAmount: Double) {}
        override suspend fun getBudgetByCategory(categoryId: String): BudgetEntity? = null
        override suspend fun getRecentAmountsForCategory(categoryId: String, excludingTransactionId: String?, limit: Int): List<Double> = emptyList()
        override suspend fun getRecentDescriptionsAndCategories(limit: Int): List<com.makemission.makeflow.data.local.dao.DescriptionCategoryPair> = emptyList()
        override suspend fun createRecurringTransaction(entity: RecurringTransactionEntity) {}
        override suspend fun updateRecurringTransaction(entity: RecurringTransactionEntity) {}
        override suspend fun deleteRecurringTransaction(entity: RecurringTransactionEntity) {}
        override suspend fun toggleRecurringTransactionActive(id: String) {}
        override suspend fun skipRecurringTransaction(id: String) {}
        override suspend fun processDueRecurringTransactions() {}
        override fun getAllActiveRecurringTransactions(): Flow<List<RecurringTransactionEntity>> = MutableStateFlow(emptyList())
        override fun getDueRecurringTransactions(now: Long): Flow<List<RecurringTransactionEntity>> = MutableStateFlow(emptyList())
        override suspend fun mergeCategories(sourceId: String, targetId: String): String? = null
        override suspend fun convertCategoryType(categoryId: String, newType: String): String? = null
        override suspend fun getConvertedTotalBalance(displayCurrency: String): Double = 0.0
    }

    private class FakeCardInteractionDao : CardInteractionDao {
        private val interactionCounts = MutableStateFlow<List<CardHourCount>>(emptyList())

        fun addCounts(counts: List<CardHourCount>) {
            interactionCounts.value = counts
        }

        override suspend fun insert(entity: CardInteractionEntity) {}
        override suspend fun getInteractionCounts(): List<CardHourCount> = interactionCounts.value
        override suspend fun deleteOlderThan(cutoffTimestamp: Long) {}
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
