package com.makemission.makeflow.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import com.makemission.makeflow.domain.HistoryPeriod
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

    @Test
    fun searchMatchesNotesAmountsCategoriesAccountsAndReferences() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val matatu = createTransaction("matatu", amount = 75.0, note = "Matatu fare")
            .copy(externalReference = "ABC123")
        val lunch = createTransaction("lunch", amount = 450.0, note = "Lunch", categoryId = "cat2")
            .copy(accountId = "acct2")
        repo.addTransaction(matatu)
        repo.addTransaction(lunch)
        repo.addAccount(AccountEntity("acct1", "Daily wallet", 0.0, 0.0))
        repo.addAccount(AccountEntity("acct2", "Bank account", 0.0, 0.0))
        repo.addCategory(CategoryEntity("cat1", "Transport", "EXPENSE"))
        repo.addCategory(CategoryEntity("cat2", "Food", "EXPENSE"))

        val viewModel = HistoryViewModel(repo, createTestUserPrefs())
        val results = mutableListOf<List<TransactionEntity>>()
        val job = launch { viewModel.filteredTransactions.collect { results.add(it) } }
        runCurrent()

        fun assertSearch(query: String, expectedId: String) {
            viewModel.searchQuery.value = query
            runCurrent()
            assertEquals(listOf(expectedId), results.last().map { it.id })
        }

        assertSearch("matatu", "matatu")
        assertSearch("75", "matatu")
        assertSearch("transport", "matatu")
        assertSearch("daily wallet", "matatu")
        assertSearch("abc-123", "matatu")
        assertSearch("lunch", "lunch")

        job.cancel()
    }

    @Test
    fun datePeriodCombinesWithSearchTypeAndFocusedAccount() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val now = ZonedDateTime.parse("2026-10-09T12:00:00+03:00[Africa/Nairobi]")
        val match = createTransaction("match").copy(timestamp = now.toInstant().toEpochMilli())
        repo.addTransaction(match)
        repo.addTransaction(match.copy(id = "yesterday", timestamp = now.minusDays(1).toInstant().toEpochMilli()))
        repo.addTransaction(match.copy(id = "other-account", accountId = "acct2"))
        repo.addTransaction(match.copy(id = "income", type = "INCOME"))
        repo.addTransaction(match.copy(id = "tea", note = "tea"))
        val prefs = createTestUserPrefs()
        prefs.saveFocusedAccountId("acct1")
        val viewModel = HistoryViewModel(repo, prefs) { now }
        val job = launch { viewModel.filteredTransactions.collect {} }
        viewModel.selectedPeriod.value = HistoryPeriod.TODAY
        viewModel.selectedFilter.value = "EXPENSE"
        viewModel.searchQuery.value = "coffee"
        runCurrent()

        assertEquals(listOf("match"), viewModel.filteredTransactions.value.map { it.id })

        viewModel.clearFilters()
        runCurrent()
        assertEquals(HistoryPeriod.ALL_TIME, viewModel.selectedPeriod.value)
        assertEquals("ALL", viewModel.selectedFilter.value)
        assertEquals("", viewModel.searchQuery.value)
        assertEquals("acct1", viewModel.focusedAccountId.value)
        assertEquals(setOf("match", "yesterday", "income", "tea"), viewModel.filteredTransactions.value.map { it.id }.toSet())
        job.cancel()
    }

    @Test
    fun refreshDateFiltersRollsTodayForwardWithoutChangingSelection() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        var now = ZonedDateTime.parse("2026-10-09T23:59:00+03:00[Africa/Nairobi]")
        repo.addTransaction(createTransaction("today").copy(timestamp = now.toInstant().toEpochMilli()))
        repo.addTransaction(createTransaction("tomorrow").copy(timestamp = now.plusMinutes(2).toInstant().toEpochMilli()))
        val viewModel = HistoryViewModel(repo, createTestUserPrefs()) { now }
        viewModel.selectedPeriod.value = HistoryPeriod.TODAY
        val job = launch { viewModel.filteredTransactions.collect {} }
        runCurrent()
        assertEquals(listOf("today"), viewModel.filteredTransactions.value.map { it.id })

        now = now.plusMinutes(2)
        viewModel.refreshDateFilters()
        runCurrent()
        assertEquals(listOf("tomorrow"), viewModel.filteredTransactions.value.map { it.id })
        assertEquals(HistoryPeriod.TODAY, viewModel.selectedPeriod.value)
        job.cancel()
    }

    @Test
    fun refreshDateFiltersUsesNewTimeZone() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        var now = ZonedDateTime.parse("2026-10-09T00:30:00+03:00[Africa/Nairobi]")
        val previousEvening = now.minusHours(1).toInstant().toEpochMilli()
        repo.addTransaction(createTransaction("previous-evening").copy(timestamp = previousEvening))
        val viewModel = HistoryViewModel(repo, createTestUserPrefs()) { now }
        viewModel.selectedPeriod.value = HistoryPeriod.TODAY
        val job = launch { viewModel.filteredTransactions.collect {} }
        runCurrent()
        assertEquals(emptyList<TransactionEntity>(), viewModel.filteredTransactions.value)

        now = now.withZoneSameInstant(ZoneId.of("UTC"))
        viewModel.refreshDateFilters()
        runCurrent()
        assertEquals(listOf("previous-evening"), viewModel.filteredTransactions.value.map { it.id })
        job.cancel()
    }

    @Test
    fun datePeriodRespectsTransferDestinationAccount() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val now = ZonedDateTime.parse("2026-10-09T12:00:00+03:00[Africa/Nairobi]")
        repo.addTransaction(createTransaction("transfer", type = "TRANSFER").copy(
            fromAccountId = "acct1", toAccountId = "acct2", timestamp = now.toInstant().toEpochMilli()
        ))
        val prefs = createTestUserPrefs()
        prefs.saveFocusedAccountId("acct2")
        val viewModel = HistoryViewModel(repo, prefs) { now }
        viewModel.selectedPeriod.value = HistoryPeriod.THIS_MONTH
        val job = launch { viewModel.filteredTransactions.collect {} }
        runCurrent()
        assertEquals(listOf("transfer"), viewModel.filteredTransactions.value.map { it.id })
        job.cancel()
    }

    private class FakeFinanceRepository : FinanceRepository {
        private val transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
        private val accounts = MutableStateFlow<List<AccountEntity>>(emptyList())
        private val categories = MutableStateFlow<List<CategoryEntity>>(emptyList())

        fun addTransaction(tx: TransactionEntity) {
            transactions.value = transactions.value + tx
        }

        fun addAccount(account: AccountEntity) {
            accounts.value = accounts.value + account
        }

        fun addCategory(category: CategoryEntity) {
            categories.value = categories.value + category
        }

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactions

        override suspend fun insertTransaction(transaction: TransactionEntity) {
            transactions.value = transactions.value + transaction
        }

        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            transactions.value = transactions.value.filter { it.id != transaction.id }
        }

        override fun getAllAccounts(): Flow<List<AccountEntity>> = accounts

        override fun getAllCategories(): Flow<List<CategoryEntity>> = categories

        override fun getMonthlySpendingByCategory(
            monthStartMillis: Long, monthEndMillis: Long
        ): Flow<List<com.makemission.makeflow.data.local.dao.CategorySpending>> =
            MutableStateFlow(emptyList())

        override fun getTransactionsInRange(
            startMillis: Long, endMillis: Long
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override fun getTransactionsInRangeForAccount(
            startMillis: Long, endMillis: Long, accountId: String
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override fun getMonthlyTotalExpense(monthStartMillis: Long, monthEndMillis: Long): Flow<Double> =
            MutableStateFlow(0.0)

        override fun getMonthlyTotalIncome(monthStartMillis: Long, monthEndMillis: Long): Flow<Double> =
            MutableStateFlow(0.0)

        override suspend fun getMonthlyTotalExpenseOnce(monthStartMillis: Long, monthEndMillis: Long): Double = 0.0

        override suspend fun getMonthlyTotalIncomeOnce(monthStartMillis: Long, monthEndMillis: Long): Double = 0.0

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
        override suspend fun updateBudget(budgetId: String, amount: Double, period: String, name: String?) {}
        override suspend fun checkDuplicate(candidate: TransactionEntity): com.makemission.makeflow.domain.DuplicateCheckResult? = null
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

    @Test
    fun `filteredTransactions scopes to focused account`() = runTest(testDispatcher) {
        val repo = FakeFinanceRepository()
        val txA = TransactionEntity(
            id = "tx_a", amount = 100.0, type = "EXPENSE", categoryId = "cat1",
            accountId = "acc_a", note = "coffee", timestamp = 1000L
        )
        val txB = TransactionEntity(
            id = "tx_b", amount = 200.0, type = "EXPENSE", categoryId = "cat1",
            accountId = "acc_b", note = "tea", timestamp = 2000L
        )
        repo.addTransaction(txA)
        repo.addTransaction(txB)

        val userPrefs = createTestUserPrefs()
        val viewModel = HistoryViewModel(repo, userPrefs)

        // Default: all transactions
        val allCollected = mutableListOf<List<TransactionEntity>>()
        val allJob = launch { viewModel.filteredTransactions.collect { allCollected.add(it) } }
        runCurrent()
        assertEquals(2, allCollected.last().size)
        allJob.cancel()

        // Set focused account via DataStore (ViewModel doesn't expose setter directly)
        userPrefs.saveFocusedAccountId("acc_a")
        advanceUntilIdle()

        val focusedCollected = mutableListOf<List<TransactionEntity>>()
        val focusedJob = launch { viewModel.filteredTransactions.collect { focusedCollected.add(it) } }
        runCurrent()
        assertEquals(1, focusedCollected.last().size)
        assertEquals("tx_a", focusedCollected.last()[0].id)
        focusedJob.cancel()
    }
}
