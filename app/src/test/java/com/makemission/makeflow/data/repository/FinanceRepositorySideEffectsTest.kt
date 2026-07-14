package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.RecurringDetector
import com.makemission.makeflow.data.local.dao.AccountDao
import com.makemission.makeflow.data.local.dao.BudgetDao
import com.makemission.makeflow.data.local.dao.CategoryDao
import com.makemission.makeflow.data.local.dao.CategorySpending
import com.makemission.makeflow.data.local.dao.DescriptionCategoryPair
import com.makemission.makeflow.data.local.dao.MonthlyCategorySpend
import com.makemission.makeflow.data.local.dao.RecurringPatternDao
import com.makemission.makeflow.data.local.dao.RecurringTransactionDao
import com.makemission.makeflow.data.local.dao.TaskDao
import com.makemission.makeflow.data.local.dao.TransactionDao
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.CategoryWithTasks
import com.makemission.makeflow.data.local.entity.RecurringPatternEntity
import com.makemission.makeflow.data.local.entity.RecurringTransactionEntity
import com.makemission.makeflow.data.local.entity.TaskEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.domain.BudgetSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
            flowOf(transactions.filter { it.timestamp in startMillis until endMillis })

        override fun getTransactionsInRangeForAccount(startMillis: Long, endMillis: Long, accountId: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { (it.timestamp in startMillis until endMillis) && (it.accountId == accountId || it.fromAccountId == accountId || it.toAccountId == accountId) })

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

        override suspend fun getMonthlyTotalExpenseOnce(monthStartMillis: Long, monthEndMillis: Long): Double = 0.0

        override suspend fun getMonthlyTotalIncomeOnce(monthStartMillis: Long, monthEndMillis: Long): Double = 0.0

        override suspend fun getRecentAmountsForCategory(
            categoryId: String, excludingTransactionId: String?, limit: Int
        ): List<Double> = emptyList()

        override suspend fun getRecentDescriptionsAndCategories(limit: Int): List<DescriptionCategoryPair> = emptyList()

        override suspend fun clearAll() {
            transactions.clear()
            transactionsFlow.value = emptyList()
        }

        override suspend fun getAllTransactionsOnce(): List<TransactionEntity> = transactions.toList()

        override suspend fun getTransactionsSince(sinceMillis: Long): List<TransactionEntity> =
            transactions.filter { it.timestamp >= sinceMillis }

        override suspend fun hasExternalReference(externalRef: String): Boolean =
            transactions.any { it.externalReference == externalRef }
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
        val categories = mutableListOf<CategoryEntity>()
        private val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())
        var txCountForCategory: (String) -> Int = { 0 }

        override fun getAllCategories(): Flow<List<CategoryEntity>> = categoriesFlow
        override fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = categoriesFlow

        override suspend fun insertCategory(category: CategoryEntity) {
            categories.add(category)
            categoriesFlow.value = categories.toList()
        }

        override suspend fun updateCategory(category: CategoryEntity) {
            val idx = categories.indexOfFirst { it.id == category.id }
            if (idx >= 0) categories[idx] = category
            categoriesFlow.value = categories.toList()
        }

        override suspend fun deleteCategory(category: CategoryEntity) {
            categories.removeAll { it.id == category.id }
            categoriesFlow.value = categories.toList()
        }

        override suspend fun deleteCategoryById(id: String) {
            categories.removeAll { it.id == id }
            categoriesFlow.value = categories.toList()
        }

        override suspend fun getCategoryById(id: String): CategoryEntity? = categories.find { it.id == id }

        override suspend fun getCategoryByName(name: String): CategoryEntity? = categories.find { it.name == name }

        override suspend fun getAnyCategory(): CategoryEntity? = categories.firstOrNull()

        override suspend fun getCount(): Int = categories.size

        override fun getMonthlySpendingByCategory(monthStartMillis: Long, monthEndMillis: Long): Flow<List<CategorySpending>> = flowOf(emptyList())

        override fun getCategoryWithTasks(id: String): Flow<CategoryWithTasks?> = flowOf(null)

        override suspend fun clearAll() {
            categories.clear()
            categoriesFlow.value = emptyList()
        }

        var reassignTxSource: String? = null
        var reassignTxTarget: String? = null
        var reassignBudgetSource: String? = null
        var reassignBudgetTarget: String? = null
        var reassignBudgetTargetName: String? = null

        override suspend fun reassignTransactionCategory(sourceId: String, targetId: String) {
            reassignTxSource = sourceId
            reassignTxTarget = targetId
        }

        override suspend fun reassignBudgetCategory(sourceId: String, targetId: String, targetName: String) {
            reassignBudgetSource = sourceId
            reassignBudgetTarget = targetId
            reassignBudgetTargetName = targetName
        }

        override suspend fun getChildCategoryCount(categoryId: String): Int = categories.count { it.parentCategoryId == categoryId }

        override suspend fun getTransactionCountForCategory(categoryId: String): Int =
            txCountForCategory(categoryId)

        override suspend fun getAllCategoriesOnce(): List<CategoryEntity> = categories.toList()
    }

    private class FakeTaskDao : TaskDao {
        override suspend fun update(task: TaskEntity) {}
        override suspend fun delete(task: TaskEntity) {}
        override suspend fun updateTaskCategory(taskId: String, categoryId: String?) {}
        override fun getAllTasks(): Flow<List<TaskEntity>> = flowOf(emptyList())
        override suspend fun insert(task: TaskEntity) {}
        override suspend fun getAllOnce(): List<TaskEntity> = emptyList()
        override suspend fun clearAll() {}
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

        override suspend fun getAllAccountsOnce(): List<AccountEntity> = accounts.toList()
    }

    private class FakeBudgetDao : BudgetDao {
        val budgets = mutableListOf<BudgetEntity>()
        private val budgetsFlow = MutableStateFlow<List<BudgetEntity>>(emptyList())

        override suspend fun insertBudget(budget: BudgetEntity) {
            budgets.add(budget)
            budgetsFlow.value = budgets.toList()
        }

        override suspend fun deleteBudget(budget: BudgetEntity) {
            budgets.removeAll { it.id == budget.id }
            budgetsFlow.value = budgets.toList()
        }

        override suspend fun updateBudget(budgetId: String, amount: Double, period: String, name: String?) {
            val idx = budgets.indexOfFirst { it.id == budgetId }
            if (idx >= 0) {
                budgets[idx] = budgets[idx].copy(amount = amount, period = period, name = name)
                budgetsFlow.value = budgets.toList()
            }
        }

        override suspend fun updateBudgetSpending(categoryId: String, amount: Double) {
            val idx = budgets.indexOfFirst { it.categoryId == categoryId }
            if (idx >= 0) {
                budgets[idx] = budgets[idx].copy(spentAmount = budgets[idx].spentAmount + amount)
                budgetsFlow.value = budgets.toList()
            }
        }

        override suspend fun decrementBudgetSpending(categoryId: String, amount: Double) {
            val idx = budgets.indexOfFirst { it.categoryId == categoryId }
            if (idx >= 0) {
                budgets[idx] = budgets[idx].copy(spentAmount = maxOf(budgets[idx].spentAmount - amount, 0.0))
                budgetsFlow.value = budgets.toList()
            }
        }

        override suspend fun updateBudgetAmount(budgetId: String, newAmount: Double) {}

        override suspend fun getBudgetByCategory(categoryId: String): BudgetEntity? = budgets.find { it.categoryId == categoryId }

        override fun getAllBudgetsFlow(): Flow<List<BudgetEntity>> = budgetsFlow

        override suspend fun clearAll() {
            budgets.clear()
            budgetsFlow.value = emptyList()
        }

        override suspend fun getAllBudgetsOnce(): List<BudgetEntity> = budgets.toList()
    }

    private class FakeRecurringTransactionDao : RecurringTransactionDao {
        val entities = mutableListOf<RecurringTransactionEntity>()
        private val activeFlow = MutableStateFlow<List<RecurringTransactionEntity>>(emptyList())
        private val dueFlow = MutableStateFlow<List<RecurringTransactionEntity>>(emptyList())

        var insertCount = 0
        var updateCount = 0
        var deleteCount = 0

        override fun getAllActive(): Flow<List<RecurringTransactionEntity>> = activeFlow

        override fun getDueTodayOrOverdue(now: Long): Flow<List<RecurringTransactionEntity>> = dueFlow

        override suspend fun getDueTodayOrOverdueOnce(now: Long): List<RecurringTransactionEntity> =
            entities.filter { it.isActive && it.nextDueDate <= now }

        override suspend fun getById(id: String): RecurringTransactionEntity? = entities.find { it.id == id }

        override suspend fun getAllOnce(): List<RecurringTransactionEntity> = entities.toList()

        override suspend fun getAllActiveOnce(): List<RecurringTransactionEntity> = entities.filter { it.isActive }

        override suspend fun insert(entity: RecurringTransactionEntity) {
            insertCount++
            entities.add(entity)
            activeFlow.value = entities.filter { it.isActive }
            dueFlow.value = entities.filter { it.isActive && it.nextDueDate <= System.currentTimeMillis() }
        }

        override suspend fun update(entity: RecurringTransactionEntity) {
            updateCount++
            val idx = entities.indexOfFirst { it.id == entity.id }
            if (idx >= 0) entities[idx] = entity
            activeFlow.value = entities.filter { it.isActive }
            dueFlow.value = entities.filter { it.isActive && it.nextDueDate <= System.currentTimeMillis() }
        }

        override suspend fun delete(entity: RecurringTransactionEntity) {
            deleteCount++
            entities.removeAll { it.id == entity.id }
            activeFlow.value = entities.filter { it.isActive }
            dueFlow.value = entities.filter { it.isActive && it.nextDueDate <= System.currentTimeMillis() }
        }

        override suspend fun clearAll() {
            entities.clear()
            activeFlow.value = emptyList()
            dueFlow.value = emptyList()
        }
    }

    @Test
    fun processDueRecurring_autoConfirm_insertsTransaction_and_advancesDate() {
        runBlocking {
            val txDao = FakeTransactionDao()
            val acctDao = FakeAccountDao()
            acctDao.insertAccount(AccountEntity(id = "acct1", name = "Wallet", balance = 500.0, initialBalance = 0.0))
            val recurringDao = FakeRecurringTransactionDao()
            recurringDao.insert(
                RecurringTransactionEntity(
                    id = "r1", description = "Netflix", amount = 15.0, type = "EXPENSE",
                    accountId = "acct1", categoryId = null,
                    frequency = "MONTHLY", intervalDays = 30,
                    startDate = 1000, nextDueDate = 1000,
                    autoConfirm = true, isActive = true
                )
            )
            val repo = FinanceRepositoryImpl(
                categoryDao = FakeCategoryDao(),
                transactionDao = txDao,
                taskDao = FakeTaskDao(),
                accountDao = acctDao,
                budgetDao = FakeBudgetDao(),
                recurringPatternDao = FakeRecurringPatternDao(),
                recurringTransactionDao = recurringDao
            )

            repo.processDueRecurringTransactions()

            assertEquals(1, txDao.insertCount)
            assertEquals("Netflix", txDao.lastInserted?.note)
            val updated = recurringDao.getById("r1")
            assertEquals(1000 + 30L * 24 * 60 * 60 * 1000, updated?.nextDueDate)
            assertTrue(updated?.isActive == true)
        }
    }

    @Test
    fun processDueRecurring_pastEndDate_deactivates_without_inserting() {
        runBlocking {
            val txDao = FakeTransactionDao()
            val recurringDao = FakeRecurringTransactionDao()
            recurringDao.insert(
                RecurringTransactionEntity(
                    id = "r1", description = "Trial", amount = 10.0, type = "EXPENSE",
                    accountId = "acct1", categoryId = null,
                    frequency = "MONTHLY", intervalDays = 30,
                    startDate = 1000, endDate = 500, nextDueDate = 1000,
                    autoConfirm = true, isActive = true
                )
            )
            val repo = FinanceRepositoryImpl(
                categoryDao = FakeCategoryDao(),
                transactionDao = txDao,
                taskDao = FakeTaskDao(),
                accountDao = FakeAccountDao(),
                budgetDao = FakeBudgetDao(),
                recurringPatternDao = FakeRecurringPatternDao(),
                recurringTransactionDao = recurringDao
            )

            repo.processDueRecurringTransactions()

            assertEquals(0, txDao.insertCount)
            val updated = recurringDao.getById("r1")
            assertEquals(false, updated?.isActive)
        }
    }

    @Test
    fun processDueRecurring_nonAutoConfirm_doesNotInsert() {
        runBlocking {
            val txDao = FakeTransactionDao()
            val recurringDao = FakeRecurringTransactionDao()
            recurringDao.insert(
                RecurringTransactionEntity(
                    id = "r1", description = "Rent", amount = 1000.0, type = "EXPENSE",
                    accountId = "acct1", categoryId = null,
                    frequency = "MONTHLY", intervalDays = 30,
                    startDate = 1000, nextDueDate = 1000,
                    autoConfirm = false, isActive = true
                )
            )
            val repo = FinanceRepositoryImpl(
                categoryDao = FakeCategoryDao(),
                transactionDao = txDao,
                taskDao = FakeTaskDao(),
                accountDao = FakeAccountDao(),
                budgetDao = FakeBudgetDao(),
                recurringPatternDao = FakeRecurringPatternDao(),
                recurringTransactionDao = recurringDao
            )

            repo.processDueRecurringTransactions()

            assertEquals(0, txDao.insertCount)
        }
    }

    @Test
    fun skipRecurringTransaction_advancesDate_without_inserting() {
        runBlocking {
            val txDao = FakeTransactionDao()
            val recurringDao = FakeRecurringTransactionDao()
            recurringDao.insert(
                RecurringTransactionEntity(
                    id = "r1", description = "Gym", amount = 50.0, type = "EXPENSE",
                    accountId = "acct1", categoryId = null,
                    frequency = "MONTHLY", intervalDays = 30,
                    startDate = 1000, nextDueDate = 1000,
                    autoConfirm = false, isActive = true
                )
            )
            val repo = FinanceRepositoryImpl(
                categoryDao = FakeCategoryDao(),
                transactionDao = txDao,
                taskDao = FakeTaskDao(),
                accountDao = FakeAccountDao(),
                budgetDao = FakeBudgetDao(),
                recurringPatternDao = FakeRecurringPatternDao(),
                recurringTransactionDao = recurringDao
            )

            repo.skipRecurringTransaction("r1")

            assertEquals(0, txDao.insertCount)
            val updated = recurringDao.getById("r1")
            assertEquals(1000 + 30L * 24 * 60 * 60 * 1000, updated?.nextDueDate)
        }
    }

    @Test
    fun transfer_insert_correctly_updates_both_accounts() {
        runBlocking {
            val txDao = FakeTransactionDao()
            val acctDao = FakeAccountDao()
            acctDao.insertAccount(AccountEntity(id = "from1", name = "Wallet", balance = 1000.0, initialBalance = 0.0))
            acctDao.insertAccount(AccountEntity(id = "to1", name = "Bank", balance = 500.0, initialBalance = 0.0))
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
                    id = "tx1", amount = 200.0, type = "TRANSFER",
                    fromAccountId = "from1", toAccountId = "to1", note = "transfer", timestamp = 1000
                )
            )

            assertEquals(800.0, acctDao.getAccountById("from1")!!.balance, 0.001)
            assertEquals(700.0, acctDao.getAccountById("to1")!!.balance, 0.001)
            assertEquals(0, txDao.deleteCount)
        }
    }

    @Test
    fun transfer_delete_reverses_both_accounts() {
        runBlocking {
            val txDao = FakeTransactionDao()
            val acctDao = FakeAccountDao()
            acctDao.insertAccount(AccountEntity(id = "from1", name = "Wallet", balance = 1000.0, initialBalance = 0.0))
            acctDao.insertAccount(AccountEntity(id = "to1", name = "Bank", balance = 500.0, initialBalance = 0.0))
            val repo = FinanceRepositoryImpl(
                categoryDao = FakeCategoryDao(),
                transactionDao = txDao,
                taskDao = FakeTaskDao(),
                accountDao = acctDao,
                budgetDao = FakeBudgetDao(),
                recurringPatternDao = FakeRecurringPatternDao()
            )

            val tx = TransactionEntity(
                id = "tx1", amount = 200.0, type = "TRANSFER",
                fromAccountId = "from1", toAccountId = "to1", note = "transfer", timestamp = 1000
            )
            repo.insertTransaction(tx)
            assertEquals(800.0, acctDao.getAccountById("from1")!!.balance, 0.001)
            assertEquals(700.0, acctDao.getAccountById("to1")!!.balance, 0.001)

            repo.deleteTransaction(tx)
            assertEquals(1000.0, acctDao.getAccountById("from1")!!.balance, 0.001)
            assertEquals(500.0, acctDao.getAccountById("to1")!!.balance, 0.001)
        }
    }

    @Test
    fun transfer_update_reverses_old_and_applies_new() {
        runBlocking {
            val txDao = FakeTransactionDao()
            val acctDao = FakeAccountDao()
            val fromId = "from1"
            val toIdOld = "to1"
            val toIdNew = "to2"
            acctDao.insertAccount(AccountEntity(id = fromId, name = "Wallet", balance = 1000.0, initialBalance = 0.0))
            acctDao.insertAccount(AccountEntity(id = toIdOld, name = "Bank A", balance = 500.0, initialBalance = 0.0))
            acctDao.insertAccount(AccountEntity(id = toIdNew, name = "Bank B", balance = 300.0, initialBalance = 0.0))
            val repo = FinanceRepositoryImpl(
                categoryDao = FakeCategoryDao(),
                transactionDao = txDao,
                taskDao = FakeTaskDao(),
                accountDao = acctDao,
                budgetDao = FakeBudgetDao(),
                recurringPatternDao = FakeRecurringPatternDao()
            )

            val oldTx = TransactionEntity(
                id = "tx1", amount = 200.0, type = "TRANSFER",
                fromAccountId = fromId, toAccountId = toIdOld, note = "transfer", timestamp = 1000
            )
            repo.insertTransaction(oldTx)
            assertEquals(800.0, acctDao.getAccountById(fromId)!!.balance, 0.001)
            assertEquals(700.0, acctDao.getAccountById(toIdOld)!!.balance, 0.001)

            val updatedTx = oldTx.copy(toAccountId = toIdNew, amount = 150.0)
            repo.updateTransaction(updatedTx)
            assertEquals(850.0, acctDao.getAccountById(fromId)!!.balance, 0.001)
            assertEquals(500.0, acctDao.getAccountById(toIdOld)!!.balance, 0.001)
            assertEquals(450.0, acctDao.getAccountById(toIdNew)!!.balance, 0.001)
        }
    }

    @Test
    fun transfer_does_not_trigger_recurring_detection() {
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
                    id = "tx1", amount = 100.0, type = "TRANSFER",
                    fromAccountId = "a1", toAccountId = "a2", note = "transfer", timestamp = 1000
                )
            )

            assertEquals(1, txDao.insertCount)
            assertEquals(0, patternDao.deleteAllCount)
        }
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

    @Test
    fun mergeCategories_sameType_succeeds_and_deletesSource() = runBlocking {
        val catDao = FakeCategoryDao()
        catDao.insertCategory(CategoryEntity(id = "src", name = "Groceries", type = "EXPENSE"))
        catDao.insertCategory(CategoryEntity(id = "tgt", name = "Food", type = "EXPENSE"))
        val repo = FinanceRepositoryImpl(
            categoryDao = catDao,
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val error = repo.mergeCategories("src", "tgt")
        assertNull(error)
        assertNull(catDao.getCategoryById("src"))
        assertEquals("Food", catDao.getCategoryById("tgt")?.name)
        assertEquals("src", catDao.reassignTxSource)
        assertEquals("tgt", catDao.reassignTxTarget)
        assertEquals("src", catDao.reassignBudgetSource)
        assertEquals("tgt", catDao.reassignBudgetTarget)
        assertEquals("Food", catDao.reassignBudgetTargetName)
    }

    @Test
    fun mergeCategories_differentTypes_returnsError() = runBlocking {
        val catDao = FakeCategoryDao()
        catDao.insertCategory(CategoryEntity(id = "src", name = "Salary", type = "INCOME"))
        catDao.insertCategory(CategoryEntity(id = "tgt", name = "Food", type = "EXPENSE"))
        val repo = FinanceRepositoryImpl(
            categoryDao = catDao,
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val error = repo.mergeCategories("src", "tgt")
        assertNotNull(error)
        assertTrue(error!!.contains("different types"))
        assertNotNull(catDao.getCategoryById("src"))
    }

    @Test
    fun mergeCategories_sourceHasChildren_returnsError() = runBlocking {
        val catDao = FakeCategoryDao()
        catDao.insertCategory(CategoryEntity(id = "src", name = "Transport", type = "EXPENSE"))
        catDao.insertCategory(CategoryEntity(id = "child", name = "Fuel", type = "EXPENSE", parentCategoryId = "src"))
        catDao.insertCategory(CategoryEntity(id = "tgt", name = "Travel", type = "EXPENSE"))
        val repo = FinanceRepositoryImpl(
            categoryDao = catDao,
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val error = repo.mergeCategories("src", "tgt")
        assertNotNull(error)
        assertTrue(error!!.contains("subcategor"))
        assertNotNull(catDao.getCategoryById("src"))
    }

    @Test
    fun mergeCategories_sameId_returnsError() = runBlocking {
        val catDao = FakeCategoryDao()
        catDao.insertCategory(CategoryEntity(id = "c1", name = "Same", type = "EXPENSE"))
        val repo = FinanceRepositoryImpl(
            categoryDao = catDao,
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val error = repo.mergeCategories("c1", "c1")
        assertNotNull(error)
    }

    @Test
    fun convertCategoryType_zeroTransactions_succeeds() = runBlocking {
        val catDao = FakeCategoryDao()
        catDao.insertCategory(CategoryEntity(id = "c1", name = "Old Expense", type = "EXPENSE"))
        val repo = FinanceRepositoryImpl(
            categoryDao = catDao,
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val error = repo.convertCategoryType("c1", "INCOME")
        assertNull(error)
        assertEquals("INCOME", catDao.getCategoryById("c1")?.type)
    }

    @Test
    fun convertCategoryType_withTransactions_returnsError() = runBlocking {
        val catDao = FakeCategoryDao()
        catDao.insertCategory(CategoryEntity(id = "c1", name = "Expense With Tx", type = "EXPENSE"))
        catDao.txCountForCategory = { if (it == "c1") 5 else 0 }
        val repo = FinanceRepositoryImpl(
            categoryDao = catDao,
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val error = repo.convertCategoryType("c1", "INCOME")
        assertNotNull(error)
        assertTrue(error!!.contains("transaction"))
        assertEquals("EXPENSE", catDao.getCategoryById("c1")?.type)
    }

    @Test
    fun convertCategoryType_sameType_returnsNull() = runBlocking {
        val catDao = FakeCategoryDao()
        catDao.insertCategory(CategoryEntity(id = "c1", name = "Expense", type = "EXPENSE"))
        val repo = FinanceRepositoryImpl(
            categoryDao = catDao,
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val error = repo.convertCategoryType("c1", "EXPENSE")
        assertNull(error)
        assertEquals("EXPENSE", catDao.getCategoryById("c1")?.type)
    }

    @Test
    fun insertCategory_persistsWithCorrectFields() = runBlocking {
        val catDao = FakeCategoryDao()
        val repo = FinanceRepositoryImpl(
            categoryDao = catDao,
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val category = CategoryEntity(
            id = "new1",
            name = "Groceries",
            type = "EXPENSE",
            parentCategoryId = null,
            iconName = "local_grocery_store",
            colorHex = "#4CAF50"
        )
        repo.insertCategory(category)

        val saved = catDao.getCategoryById("new1")
        assertNotNull(saved)
        assertEquals("Groceries", saved!!.name)
        assertEquals("EXPENSE", saved.type)
        assertNull(saved.parentCategoryId)
        assertEquals("local_grocery_store", saved.iconName)
        assertEquals("#4CAF50", saved.colorHex)
    }

    @Test
    fun insertCategory_withParentCategory_persistsCorrectly() = runBlocking {
        val catDao = FakeCategoryDao()
        catDao.insertCategory(CategoryEntity(id = "parent", name = "Food", type = "EXPENSE"))

        val child = CategoryEntity(
            id = "child1",
            name = "Groceries",
            type = "EXPENSE",
            parentCategoryId = "parent",
            iconName = "local_grocery_store",
            colorHex = "#BA68C8"
        )
        catDao.insertCategory(child)

        val saved = catDao.getCategoryById("child1")
        assertNotNull(saved)
        assertEquals("parent", saved!!.parentCategoryId)
        assertEquals("EXPENSE", catDao.getCategoryById("parent")?.type)
    }

    @Test
    fun deleteCategoryWithGuards_unusedCategory_succeeds() = runBlocking {
        val catDao = FakeCategoryDao()
        catDao.insertCategory(CategoryEntity(id = "c1", name = "Vacations", type = "EXPENSE"))
        val repo = FinanceRepositoryImpl(
            categoryDao = catDao,
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val error = repo.deleteCategoryWithGuards("c1")
        assertNull(error)
        assertNull(catDao.getCategoryById("c1"))
    }

    @Test
    fun deleteCategoryWithGuards_withTransactions_returnsError() = runBlocking {
        val catDao = FakeCategoryDao()
        catDao.insertCategory(CategoryEntity(id = "c1", name = "Groceries", type = "EXPENSE"))
        catDao.txCountForCategory = { if (it == "c1") 5 else 0 }
        val txDao = FakeTransactionDao()
        val repo = FinanceRepositoryImpl(
            categoryDao = catDao,
            transactionDao = txDao,
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val error = repo.deleteCategoryWithGuards("c1")
        assertNotNull(error)
        assertTrue(error!!.contains("5 transaction"))
        assertNotNull(catDao.getCategoryById("c1"))
    }

    @Test
    fun deleteCategoryWithGuards_withChildren_returnsError() = runBlocking {
        val catDao = FakeCategoryDao()
        catDao.insertCategory(CategoryEntity(id = "parent", name = "Food", type = "EXPENSE"))
        catDao.insertCategory(CategoryEntity(id = "child1", name = "Groceries", type = "EXPENSE", parentCategoryId = "parent"))
        val repo = FinanceRepositoryImpl(
            categoryDao = catDao,
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val error = repo.deleteCategoryWithGuards("parent")
        assertNotNull(error)
        assertTrue(error!!.contains("subcategor"))
        assertNotNull(catDao.getCategoryById("parent"))
    }

    @Test
    fun deleteCategoryWithGuards_withBudget_returnsError() = runBlocking {
        val catDao = FakeCategoryDao()
        catDao.insertCategory(CategoryEntity(id = "c1", name = "Groceries", type = "EXPENSE"))
        val budgetDao = FakeBudgetDao()
        budgetDao.insertBudget(
            com.makemission.makeflow.data.local.entity.BudgetEntity(
                id = "b1",
                categoryId = "c1",
                categoryName = "Groceries",
                amount = 500.0,
                period = "Monthly"
            )
        )
        val repo = FinanceRepositoryImpl(
            categoryDao = catDao,
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = budgetDao,
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val error = repo.deleteCategoryWithGuards("c1")
        assertNotNull(error)
        assertTrue(error!!.contains("budget"))
        assertNotNull(catDao.getCategoryById("c1"))
    }

    @Test
    fun `getTransactionsInRange filters by date range`() = runBlocking {
        val txDao = FakeTransactionDao()
        val repo = FinanceRepositoryImpl(
            categoryDao = FakeCategoryDao(),
            transactionDao = txDao,
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val tx1 = TransactionEntity(id = "1", amount = 100.0, type = "INCOME", timestamp = 1000L)
        val tx2 = TransactionEntity(id = "2", amount = 50.0, type = "EXPENSE", timestamp = 2000L)
        val tx3 = TransactionEntity(id = "3", amount = 75.0, type = "INCOME", timestamp = 3000L)

        repo.insertTransaction(tx1)
        repo.insertTransaction(tx2)
        repo.insertTransaction(tx3)

        val inRange = repo.getTransactionsInRange(1500L, 3500L).first()

        assertEquals(2, inRange.size)
        assertTrue(inRange.all { it.id in listOf("2", "3") })
    }

    @Test
    fun `getTransactionsInRange returns empty for out-of-range`() = runBlocking {
        val txDao = FakeTransactionDao()
        val repo = FinanceRepositoryImpl(
            categoryDao = FakeCategoryDao(),
            transactionDao = txDao,
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        repo.insertTransaction(TransactionEntity(id = "1", amount = 100.0, type = "INCOME", timestamp = 1000L))

        val inRange = repo.getTransactionsInRange(5000L, 6000L).first()

        assertTrue(inRange.isEmpty())
    }

    @Test
    fun `transaction summary calculates income expense and net correctly`() = runBlocking {
        val txDao = FakeTransactionDao()
        val repo = FinanceRepositoryImpl(
            categoryDao = FakeCategoryDao(),
            transactionDao = txDao,
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val txs = listOf(
            TransactionEntity(id = "1", amount = 1000.0, type = "INCOME", timestamp = 1000L),
            TransactionEntity(id = "2", amount = 200.0, type = "EXPENSE", timestamp = 2000L),
            TransactionEntity(id = "3", amount = 500.0, type = "INCOME", timestamp = 3000L),
            TransactionEntity(id = "4", amount = 150.0, type = "EXPENSE", timestamp = 4000L),
            TransactionEntity(id = "5", amount = 300.0, type = "EXPENSE", timestamp = 5000L)
        )
        txs.forEach { repo.insertTransaction(it) }

        val all = repo.getTransactionsInRange(0L, Long.MAX_VALUE).first()

        val totalIncome = all.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = all.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val net = totalIncome - totalExpense

        assertEquals(1500.0, totalIncome, 0.001)
        assertEquals(650.0, totalExpense, 0.001)
        assertEquals(850.0, net, 0.001)
    }

    @Test
    fun `updateBudget changes amount and period`() = runBlocking {
        val repo = FinanceRepositoryImpl(
            categoryDao = FakeCategoryDao(),
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        repo.insertBudget(
            BudgetEntity(id = "b1", categoryId = "cat1", categoryName = "Food", amount = 500.0, spentAmount = 200.0, period = "Monthly")
        )

        repo.updateBudget("b1", 600.0, "Weekly")

        val updated = repo.getAllBudgets().first().first()
        assertEquals(600.0, updated.amount, 0.001)
        assertEquals("Weekly", updated.period)
    }

    @Test
    fun `updateBudget preserves spentAmount and categoryId`() = runBlocking {
        val repo = FinanceRepositoryImpl(
            categoryDao = FakeCategoryDao(),
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        repo.insertBudget(
            BudgetEntity(id = "b1", categoryId = "cat1", categoryName = "Food", amount = 500.0, spentAmount = 200.0, period = "Monthly")
        )

        repo.updateBudget("b1", 600.0, "Weekly")

        val updated = repo.getAllBudgets().first().first()
        assertEquals("cat1", updated.categoryId)
        assertEquals(200.0, updated.spentAmount, 0.001)
    }

    @Test
    fun `updateBudget does not affect unrelated budgets`() = runBlocking {
        val repo = FinanceRepositoryImpl(
            categoryDao = FakeCategoryDao(),
            transactionDao = FakeTransactionDao(),
            taskDao = FakeTaskDao(),
            accountDao = FakeAccountDao(),
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        repo.insertBudget(
            BudgetEntity(id = "b1", categoryId = "cat1", categoryName = "Food", amount = 500.0, spentAmount = 200.0, period = "Monthly")
        )
        repo.insertBudget(
            BudgetEntity(id = "b2", categoryId = "cat2", categoryName = "Transport", amount = 300.0, spentAmount = 50.0, period = "Monthly")
        )

        repo.updateBudget("b1", 600.0, "Weekly")

        val all = repo.getAllBudgets().first()
        val unchanged = all.find { it.id == "b2" }
        assertNotNull(unchanged)
        assertEquals(300.0, unchanged!!.amount, 0.001)
        assertEquals("Monthly", unchanged.period)
        assertEquals(50.0, unchanged.spentAmount, 0.001)
    }

    @Test
    fun `updateExpense_reversesOldAmount_and_appliesNewAmount`() = runBlocking {
        val txDao = FakeTransactionDao()
        val acctDao = FakeAccountDao()
        acctDao.insertAccount(AccountEntity(id = "acct1", name = "Wallet", balance = 1000.0, initialBalance = 0.0))
        val budgetDao = FakeBudgetDao()
        budgetDao.insertBudget(BudgetEntity(id = "b1", categoryId = "cat1", categoryName = "Food", amount = 500.0, spentAmount = 0.0, period = "Monthly"))
        val repo = FinanceRepositoryImpl(
            categoryDao = FakeCategoryDao(),
            transactionDao = txDao,
            taskDao = FakeTaskDao(),
            accountDao = acctDao,
            budgetDao = budgetDao,
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val old = TransactionEntity(
            id = "tx1", amount = 200.0, type = "EXPENSE",
            categoryId = "cat1", accountId = "acct1", note = "groceries", timestamp = 1000
        )
        repo.insertTransaction(old)
        assertEquals(800.0, acctDao.getAccountById("acct1")!!.balance, 0.001)
        assertEquals(200.0, budgetDao.getBudgetByCategory("cat1")!!.spentAmount, 0.001)

        val updated = old.copy(amount = 350.0)
        repo.updateTransaction(updated)
        assertEquals(650.0, acctDao.getAccountById("acct1")!!.balance, 0.001)
        assertEquals(350.0, budgetDao.getBudgetByCategory("cat1")!!.spentAmount, 0.001)
    }

    @Test
    fun `updateIncome_reversesOldAmount_and_appliesNewAmount`() = runBlocking {
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

        val old = TransactionEntity(
            id = "tx1", amount = 1000.0, type = "INCOME",
            categoryId = null, accountId = "acct1", note = "salary", timestamp = 1000
        )
        repo.insertTransaction(old)
        assertEquals(1500.0, acctDao.getAccountById("acct1")!!.balance, 0.001)

        val updated = old.copy(amount = 1200.0)
        repo.updateTransaction(updated)
        assertEquals(1700.0, acctDao.getAccountById("acct1")!!.balance, 0.001)
    }

    @Test
    fun `updateAccount_movesBalanceEffect_fromOldToNew`() = runBlocking {
        val txDao = FakeTransactionDao()
        val acctDao = FakeAccountDao()
        acctDao.insertAccount(AccountEntity(id = "acct1", name = "Wallet", balance = 1000.0, initialBalance = 0.0))
        acctDao.insertAccount(AccountEntity(id = "acct2", name = "Bank", balance = 500.0, initialBalance = 0.0))
        val budgetDao = FakeBudgetDao()
        budgetDao.insertBudget(BudgetEntity(id = "b1", categoryId = "cat1", categoryName = "Food", amount = 500.0, spentAmount = 0.0, period = "Monthly"))
        val repo = FinanceRepositoryImpl(
            categoryDao = FakeCategoryDao(),
            transactionDao = txDao,
            taskDao = FakeTaskDao(),
            accountDao = acctDao,
            budgetDao = budgetDao,
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val old = TransactionEntity(
            id = "tx1", amount = 200.0, type = "EXPENSE",
            categoryId = "cat1", accountId = "acct1", note = "groceries", timestamp = 1000
        )
        repo.insertTransaction(old)
        assertEquals(800.0, acctDao.getAccountById("acct1")!!.balance, 0.001)
        assertEquals(500.0, acctDao.getAccountById("acct2")!!.balance, 0.001)

        val updated = old.copy(accountId = "acct2")
        repo.updateTransaction(updated)
        assertEquals(1000.0, acctDao.getAccountById("acct1")!!.balance, 0.001)
        assertEquals(300.0, acctDao.getAccountById("acct2")!!.balance, 0.001)
        assertEquals(200.0, budgetDao.getBudgetByCategory("cat1")!!.spentAmount, 0.001)
    }

    @Test
    fun `updateCategory_decrementsOldBudget_and_incrementsNewBudget`() = runBlocking {
        val txDao = FakeTransactionDao()
        val acctDao = FakeAccountDao()
        acctDao.insertAccount(AccountEntity(id = "acct1", name = "Wallet", balance = 1000.0, initialBalance = 0.0))
        val budgetDao = FakeBudgetDao()
        budgetDao.insertBudget(BudgetEntity(id = "b1", categoryId = "cat1", categoryName = "Food", amount = 500.0, spentAmount = 0.0, period = "Monthly"))
        budgetDao.insertBudget(BudgetEntity(id = "b2", categoryId = "cat2", categoryName = "Transport", amount = 300.0, spentAmount = 0.0, period = "Monthly"))
        val repo = FinanceRepositoryImpl(
            categoryDao = FakeCategoryDao(),
            transactionDao = txDao,
            taskDao = FakeTaskDao(),
            accountDao = acctDao,
            budgetDao = budgetDao,
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val old = TransactionEntity(
            id = "tx1", amount = 150.0, type = "EXPENSE",
            categoryId = "cat1", accountId = "acct1", note = "lunch", timestamp = 1000
        )
        repo.insertTransaction(old)
        assertEquals(150.0, budgetDao.getBudgetByCategory("cat1")!!.spentAmount, 0.001)
        assertEquals(0.0, budgetDao.getBudgetByCategory("cat2")!!.spentAmount, 0.001)

        val updated = old.copy(categoryId = "cat2")
        repo.updateTransaction(updated)
        assertEquals(0.0, budgetDao.getBudgetByCategory("cat1")!!.spentAmount, 0.001)
        assertEquals(150.0, budgetDao.getBudgetByCategory("cat2")!!.spentAmount, 0.001)
    }

    @Test
    fun `updateTransfer_changesFromAndTo_reversesOld_and_appliesNew`() = runBlocking {
        val txDao = FakeTransactionDao()
        val acctDao = FakeAccountDao()
        val fromId = "from1"
        val toIdOld = "to1"
        val toIdNew = "to2"
        acctDao.insertAccount(AccountEntity(id = fromId, name = "Wallet", balance = 1000.0, initialBalance = 0.0))
        acctDao.insertAccount(AccountEntity(id = toIdOld, name = "Bank A", balance = 500.0, initialBalance = 0.0))
        acctDao.insertAccount(AccountEntity(id = toIdNew, name = "Bank B", balance = 300.0, initialBalance = 0.0))
        val repo = FinanceRepositoryImpl(
            categoryDao = FakeCategoryDao(),
            transactionDao = txDao,
            taskDao = FakeTaskDao(),
            accountDao = acctDao,
            budgetDao = FakeBudgetDao(),
            recurringPatternDao = FakeRecurringPatternDao()
        )

        val oldTx = TransactionEntity(
            id = "tx1", amount = 200.0, type = "TRANSFER",
            fromAccountId = fromId, toAccountId = toIdOld, note = "transfer", timestamp = 1000
        )
        repo.insertTransaction(oldTx)
        assertEquals(800.0, acctDao.getAccountById(fromId)!!.balance, 0.001)
        assertEquals(700.0, acctDao.getAccountById(toIdOld)!!.balance, 0.001)
        assertEquals(300.0, acctDao.getAccountById(toIdNew)!!.balance, 0.001)

        val updatedTx = oldTx.copy(toAccountId = toIdNew, amount = 150.0)
        repo.updateTransaction(updatedTx)
        assertEquals(850.0, acctDao.getAccountById(fromId)!!.balance, 0.001)
        assertEquals(500.0, acctDao.getAccountById(toIdOld)!!.balance, 0.001)
        assertEquals(450.0, acctDao.getAccountById(toIdNew)!!.balance, 0.001)
    }
}
