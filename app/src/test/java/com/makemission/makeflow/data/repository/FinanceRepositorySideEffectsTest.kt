package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.RecurringDetector
import com.makemission.makeflow.data.local.dao.AccountDao
import com.makemission.makeflow.data.local.dao.BudgetDao
import com.makemission.makeflow.data.local.dao.CategoryDao
import com.makemission.makeflow.data.local.dao.CategorySpending
import com.makemission.makeflow.data.local.dao.DescriptionCategoryPair
import com.makemission.makeflow.data.local.dao.MonthlyCategorySpend
import com.makemission.makeflow.data.local.dao.RecurringPatternDao
import com.makemission.makeflow.data.local.dao.TaskDao
import com.makemission.makeflow.data.local.dao.TransactionDao
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.CategoryWithTasks
import com.makemission.makeflow.data.local.entity.RecurringPatternEntity
import com.makemission.makeflow.data.local.entity.TaskEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.domain.BudgetSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceRepositorySideEffectsTest {

    private class FakeTransactionDao : TransactionDao {
        var insertCount = 0
        var deleteCount = 0
        var updateCount = 0
        var lastInserted: TransactionEntity? = null
        val transactions = mutableListOf<TransactionEntity>()
        private val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionsFlow

        override fun getTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
            flowOf(transactions)

        override suspend fun getCategorySpendByMonth(
            categoryId: String, startMillis: Long, endMillis: Long
        ): List<MonthlyCategorySpend> = emptyList()

        override fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>> =
            flowOf(emptyList())

        override suspend fun insertTransaction(transaction: TransactionEntity) {
            insertCount++
            lastInserted = transaction
            transactions.add(transaction)
            transactionsFlow.value = transactions.toList()
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            updateCount++
            val idx = transactions.indexOfFirst { it.id == transaction.id }
            if (idx >= 0) transactions[idx] = transaction
            transactionsFlow.value = transactions.toList()
        }

        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            deleteCount++
            transactions.removeAll { it.id == transaction.id }
            transactionsFlow.value = transactions.toList()
        }

        override suspend fun getTransactionById(id: String): TransactionEntity? =
            transactions.find { it.id == id }

        override suspend fun deleteTransactionById(id: String) {
            transactions.removeAll { it.id == id }
            transactionsFlow.value = transactions.toList()
        }

        override fun getMonthlyTotalExpense(monthStartMillis: Long, monthEndMillis: Long): Flow<Double> = flowOf(0.0)

        override fun getMonthlyTotalIncome(monthStartMillis: Long, monthEndMillis: Long): Flow<Double> = flowOf(0.0)

        override suspend fun getRecentAmountsForCategory(
            categoryId: String, excludingTransactionId: String?, limit: Int
        ): List<Double> = emptyList()

        override suspend fun getRecentDescriptionsAndCategories(limit: Int): List<DescriptionCategoryPair> = emptyList()

        override suspend fun clearAll() {
            transactions.clear()
            transactionsFlow.value = emptyList()
        }
    }

    private class FakeRecurringPatternDao : RecurringPatternDao {
        var deleteAllCount = 0
        var insertAllCount = 0
        var lastInsertedPatterns: List<RecurringPatternEntity>? = null
        private val patterns = mutableListOf<RecurringPatternEntity>()
        private val patternsFlow = MutableStateFlow<List<RecurringPatternEntity>>(emptyList())

        override suspend fun insertAll(patterns: List<RecurringPatternEntity>) {
            insertAllCount++
            lastInsertedPatterns = patterns
            this.patterns.clear()
            this.patterns.addAll(patterns)
            patternsFlow.value = this.patterns.toList()
        }

        override fun getAll(): Flow<List<RecurringPatternEntity>> = patternsFlow

        override suspend fun deleteAll() {
            deleteAllCount++
            patterns.clear()
            patternsFlow.value = emptyList()
        }

        override fun getUpcoming(fromDate: Long): Flow<List<RecurringPatternEntity>> = patternsFlow

        override suspend fun getAllOnce(): List<RecurringPatternEntity> = patterns.toList()

        override suspend fun updateLastNotifiedNextDate(id: Long, nextExpectedDate: Long) {}
    }

    private class FakeCategoryDao : CategoryDao {
        override fun getAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override suspend fun insertCategory(category: CategoryEntity) {}
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategory(category: CategoryEntity) {}
        override suspend fun deleteCategoryById(id: String) {}
        override suspend fun getCategoryById(id: String): CategoryEntity? = null
        override suspend fun getCategoryByName(name: String): CategoryEntity? = null
        override suspend fun getAnyCategory(): CategoryEntity? = null
        override suspend fun getCount(): Int = 0
        override fun getMonthlySpendingByCategory(monthStartMillis: Long, monthEndMillis: Long): Flow<List<CategorySpending>> = flowOf(emptyList())
        override fun getCategoryWithTasks(id: String): Flow<CategoryWithTasks?> = flowOf(null)
        override suspend fun clearAll() {}
    }

    private class FakeTaskDao : TaskDao {
        override suspend fun update(task: TaskEntity) {}
        override suspend fun delete(task: TaskEntity) {}
        override suspend fun updateTaskCategory(taskId: String, categoryId: String?) {}
        override fun getAllTasks(): Flow<List<TaskEntity>> = flowOf(emptyList())
        override suspend fun insert(task: TaskEntity) {}
    }

    private class FakeAccountDao : AccountDao {
        private val accounts = mutableListOf<AccountEntity>()
        private val accountsFlow = MutableStateFlow<List<AccountEntity>>(emptyList())

        override fun getAllAccounts(): Flow<List<AccountEntity>> = accountsFlow
        override fun getAllAccountsFlow(): Flow<List<AccountEntity>> = accountsFlow

        override suspend fun insertAccount(account: AccountEntity) {
            accounts.add(account)
            accountsFlow.value = accounts.toList()
        }

        override suspend fun updateAccount(account: AccountEntity) {
            val idx = accounts.indexOfFirst { it.id == account.id }
            if (idx >= 0) accounts[idx] = account
            accountsFlow.value = accounts.toList()
        }

        override suspend fun deleteAccount(account: AccountEntity) {
            accounts.removeAll { it.id == account.id }
            accountsFlow.value = accounts.toList()
        }

        override suspend fun getAccountById(id: String): AccountEntity? = accounts.find { it.id == id }

        override suspend fun getAnyAccount(): AccountEntity? = accounts.firstOrNull()

        override suspend fun updateBalance(accountId: String, newBalance: Double) {
            val idx = accounts.indexOfFirst { it.id == accountId }
            if (idx >= 0) {
                accounts[idx] = accounts[idx].copy(balance = newBalance)
                accountsFlow.value = accounts.toList()
            }
        }

        override suspend fun clearAll() {
            accounts.clear()
            accountsFlow.value = emptyList()
        }
    }

    private class FakeBudgetDao : BudgetDao {
        override suspend fun insertBudget(budget: BudgetEntity) {}
        override suspend fun deleteBudget(budget: BudgetEntity) {}
        override suspend fun updateBudgetSpending(categoryId: String, amount: Double) {}
        override suspend fun decrementBudgetSpending(categoryId: String, amount: Double) {}
        override suspend fun updateBudgetAmount(budgetId: String, newAmount: Double) {}
        override suspend fun getBudgetByCategory(categoryId: String): BudgetEntity? = null
        override fun getAllBudgetsFlow(): Flow<List<BudgetEntity>> = flowOf(emptyList())
        override suspend fun clearAll() {}
    }

    @Test
    fun insertTransaction_triggers_refreshRecurringPatterns_deleteAll() {
        runBlocking {
            val txDao = FakeTransactionDao()
            val patternDao = FakeRecurringPatternDao()
            val repo = FinanceRepositoryImpl(
                categoryDao = FakeCategoryDao(),
                transactionDao = txDao,
                taskDao = FakeTaskDao(),
                accountDao = FakeAccountDao(),
                budgetDao = FakeBudgetDao(),
                recurringPatternDao = patternDao
            )

            repo.insertTransaction(
                TransactionEntity(
                    id = "tx1", amount = 100.0, type = "INCOME",
                    categoryId = null, accountId = "", note = "test", timestamp = 1000
                )
            )

            assertEquals(1, txDao.insertCount)
            assertEquals(1, patternDao.deleteAllCount)
        }
    }

    @Test
    fun insertTransaction_with_account_updates_balance() {
        runBlocking {
            val txDao = FakeTransactionDao()
            val acctDao = FakeAccountDao()
            acctDao.insertAccount(AccountEntity(id = "acct1", name = "Wallet", balance = 500.0, initialBalance = 0.0))
            val repo = FinanceRepositoryImpl(
                categoryDao = FakeCategoryDao(),
                transactionDao = txDao,
                taskDao = FakeTaskDao(),
                accountDao = acctDao,
                budgetDao = FakeBudgetDao(),
                recurringPatternDao = FakeRecurringPatternDao()
            )

            repo.insertTransaction(
                TransactionEntity(
                    id = "tx1", amount = 200.0, type = "INCOME",
                    categoryId = null, accountId = "acct1", note = "salary", timestamp = 1000
                )
            )

            val updated = acctDao.getAccountById("acct1")
            assertEquals(700.0, updated!!.balance, 0.001)
        }
    }

    @Test
    fun deleteTransaction_reverses_balance() {
        runBlocking {
            val txDao = FakeTransactionDao()
            val acctDao = FakeAccountDao()
            acctDao.insertAccount(AccountEntity(id = "acct1", name = "Wallet", balance = 500.0, initialBalance = 0.0))
            val repo = FinanceRepositoryImpl(
                categoryDao = FakeCategoryDao(),
                transactionDao = txDao,
                taskDao = FakeTaskDao(),
                accountDao = acctDao,
                budgetDao = FakeBudgetDao(),
                recurringPatternDao = FakeRecurringPatternDao()
            )

            repo.insertTransaction(
                TransactionEntity(
                    id = "tx1", amount = 100.0, type = "EXPENSE",
                    categoryId = null, accountId = "acct1", note = "coffee", timestamp = 1000
                )
            )
            assertEquals(400.0, acctDao.getAccountById("acct1")!!.balance, 0.001)

            repo.deleteTransaction(
                TransactionEntity(
                    id = "tx1", amount = 100.0, type = "EXPENSE",
                    categoryId = null, accountId = "acct1", note = "coffee", timestamp = 1000
                )
            )

            assertEquals(500.0, acctDao.getAccountById("acct1")!!.balance, 0.001)
        }
    }

    @Test
    fun transaction_with_3_occurrences_is_detected_as_recurring() {
        runBlocking {
            val txDao = FakeTransactionDao()
            val patternDao = FakeRecurringPatternDao()
            val repo = FinanceRepositoryImpl(
                categoryDao = FakeCategoryDao(),
                transactionDao = txDao,
                taskDao = FakeTaskDao(),
                accountDao = FakeAccountDao(),
                budgetDao = FakeBudgetDao(),
                recurringPatternDao = patternDao
            )

            val day1 = 1700000000000L
            val day2 = day1 + 86400000L
            val day3 = day2 + 86400000L

            repo.insertTransaction(
                TransactionEntity(id = "tx1", amount = 50.0, type = "EXPENSE", note = "netflix", timestamp = day1)
            )
            repo.insertTransaction(
                TransactionEntity(id = "tx2", amount = 50.0, type = "EXPENSE", note = "Netflix", timestamp = day2)
            )
            repo.insertTransaction(
                TransactionEntity(id = "tx3", amount = 55.0, type = "EXPENSE", note = "  netflix  ", timestamp = day3)
            )

            assertTrue(patternDao.insertAllCount >= 1)
            patternDao.lastInsertedPatterns?.let { patterns ->
                assertTrue(patterns.isNotEmpty())
                val netflixPattern = patterns.find { it.normalizedDescription == "netflix" }
                assertTrue("Expected netflix pattern but found: ${patterns.map { it.normalizedDescription }}", netflixPattern != null)
            }
        }
    }
}
