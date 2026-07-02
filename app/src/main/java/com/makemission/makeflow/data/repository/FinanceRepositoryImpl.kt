package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.RecurringDetector
import com.makemission.makeflow.data.RecurringPattern
import com.makemission.makeflow.data.local.dao.CategoryDao
import com.makemission.makeflow.data.local.dao.CategorySpending
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
import com.makemission.makeflow.domain.SuggestionReason
import com.makemission.makeflow.domain.BudgetSuggestionEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Calendar

class FinanceRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val taskDao: TaskDao,
    private val accountDao: com.makemission.makeflow.data.local.dao.AccountDao? = null,
    private val budgetDao: com.makemission.makeflow.data.local.dao.BudgetDao? = null,
    private val recurringPatternDao: RecurringPatternDao? = null
) : FinanceRepository {

    override suspend fun insertAccount(account: AccountEntity) {
        accountDao?.insertAccount(account)
    }

    override suspend fun updateAccount(account: AccountEntity) {
        accountDao?.updateAccount(account)
    }

    override suspend fun deleteAccount(account: AccountEntity) {
        accountDao?.deleteAccount(account)
    }

    override fun getAllAccounts(): Flow<List<AccountEntity>> =
        accountDao?.getAllAccounts() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override fun getMonthlySpendingByCategory(
        monthStartMillis: Long,
        monthEndMillis: Long
    ): Flow<List<CategorySpending>> =
        categoryDao.getMonthlySpendingByCategory(monthStartMillis, monthEndMillis)

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    override fun getTransactionsInRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<TransactionEntity>> = transactionDao.getTransactionsInRange(startMillis, endMillis)

    override fun getMonthlyTotalExpense(
        monthStartMillis: Long,
        monthEndMillis: Long
    ): Flow<Double> = transactionDao.getMonthlyTotalExpense(monthStartMillis, monthEndMillis)

    override fun getMonthlyTotalIncome(
        monthStartMillis: Long,
        monthEndMillis: Long
    ): Flow<Double> = transactionDao.getMonthlyTotalIncome(monthStartMillis, monthEndMillis)

    override suspend fun insertCategory(category: CategoryEntity) = categoryDao.insertCategory(category)

    override suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    override suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    override suspend fun deleteCategoryById(id: String) = categoryDao.deleteCategoryById(id)

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
        accountDao?.getAccountById(transaction.accountId)?.let { account ->
            val delta = if (transaction.type == "INCOME") transaction.amount else -transaction.amount
            accountDao?.updateAccount(account.copy(balance = account.balance + delta))
        }
        if (transaction.categoryId != null && transaction.type == "EXPENSE") {
            budgetDao?.updateBudgetSpending(transaction.categoryId, transaction.amount)
        }
        refreshRecurringPatterns()
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.getTransactionById(transaction.id)?.let { old ->
            accountDao?.getAccountById(old.accountId)?.let { account ->
                val reverseDelta = if (old.type == "INCOME") -old.amount else old.amount
                accountDao?.updateAccount(account.copy(balance = account.balance + reverseDelta))
            }
            if (old.categoryId != null && old.type == "EXPENSE") {
                budgetDao?.decrementBudgetSpending(old.categoryId, old.amount)
            }
        }
        transactionDao.updateTransaction(transaction)
        accountDao?.getAccountById(transaction.accountId)?.let { account ->
            val delta = if (transaction.type == "INCOME") transaction.amount else -transaction.amount
            accountDao?.updateAccount(account.copy(balance = account.balance + delta))
        }
        if (transaction.categoryId != null && transaction.type == "EXPENSE") {
            budgetDao?.updateBudgetSpending(transaction.categoryId, transaction.amount)
        }
        refreshRecurringPatterns()
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        accountDao?.getAccountById(transaction.accountId)?.let { account ->
            val delta = if (transaction.type == "INCOME") -transaction.amount else transaction.amount
            accountDao?.updateAccount(account.copy(balance = account.balance + delta))
        }
        if (transaction.categoryId != null && transaction.type == "EXPENSE") {
            budgetDao?.decrementBudgetSpending(transaction.categoryId, transaction.amount)
        }
    }

    override fun getCategoryWithTasks(id: String): Flow<CategoryWithTasks?> =
        categoryDao.getCategoryWithTasks(id)

    override suspend fun updateTaskCategory(taskId: String, categoryId: String?) =
        taskDao.updateTaskCategory(taskId, categoryId)

    override suspend fun updateTask(task: TaskEntity) =
        taskDao.update(task)

    override fun getAllBudgets(): Flow<List<BudgetEntity>> =
        budgetDao?.getAllBudgetsFlow() ?: flowOf(emptyList())

    override suspend fun insertBudget(budget: BudgetEntity) {
        budgetDao?.insertBudget(budget)
    }

    override suspend fun deleteBudget(budget: BudgetEntity) {
        budgetDao?.deleteBudget(budget)
    }

    override suspend fun syncBudgetSpending(categoryId: String?, amount: Double, type: String) {
        if (categoryId != null && type == "EXPENSE") {
            budgetDao?.updateBudgetSpending(categoryId, amount)
        }
    }

    override suspend fun refreshRecurringPatterns() {
        val allTransactions = transactionDao.getAllTransactions().first()
        val patterns = RecurringDetector.detect(allTransactions)
        recurringPatternDao?.let { dao ->
            dao.deleteAll()
            if (patterns.isNotEmpty()) {
                dao.insertAll(patterns.map { it.toEntity() })
            }
        }
    }

    override fun getUpcomingRecurring(fromDate: Long): Flow<List<RecurringPattern>> {
        return recurringPatternDao?.getUpcoming(fromDate)
            ?.map { entities -> entities.map { it.toDomain() } }
            ?: flowOf(emptyList())
    }

    override suspend fun clearAllTransactions() {
        transactionDao.clearAll()
    }

    override suspend fun clearAllAccounts() {
        accountDao?.clearAll()
    }

    override suspend fun clearAllBudgets() {
        budgetDao?.clearAll()
    }

    override suspend fun clearAllCategories() {
        categoryDao.clearAll()
    }

    override suspend fun getBudgetSuggestions(
        dismissedKeys: Set<String>
    ): List<BudgetSuggestion> {
        val categories = categoryDao.getAllCategories().first()
        val budgets = budgetDao?.getAllBudgetsFlow()?.first() ?: emptyList()
        val budgetMap = budgets.associateBy { it.categoryId }
        val now = Calendar.getInstance()
        val endCal = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            add(Calendar.MONTH, -1)
        }
        val startCal = Calendar.getInstance().apply {
            timeInMillis = endCal.timeInMillis
            add(Calendar.MONTH, -2)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return categories.filter { it.type == "EXPENSE" }.mapNotNull { cat ->
            val key = "${cat.id}:${SuggestionReason.NO_BUDGET_SET}"
            if (key in dismissedKeys || "all" in dismissedKeys) return@mapNotNull null

            val rawMonthly = transactionDao.getCategorySpendByMonth(
                categoryId = cat.id,
                startMillis = startCal.timeInMillis,
                endMillis = endCal.timeInMillis
            )

            val startYm = "%04d-%02d".format(startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH) + 1)
            val endYm = "%04d-%02d".format(endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH) + 1)
            val monthly = BudgetSuggestionEngine.fillMissingMonths(rawMonthly, startYm, endYm)

            val currentBudget = budgetMap[cat.id]
            BudgetSuggestionEngine.suggest(
                categoryId = cat.id,
                categoryName = cat.name,
                monthlySpend = monthly,
                currentBudget = currentBudget?.amount
            )
        }
    }

    override suspend fun updateBudgetAmount(budgetId: String, newAmount: Double) {
        budgetDao?.updateBudgetAmount(budgetId, newAmount)
    }

    override suspend fun getBudgetByCategory(categoryId: String): BudgetEntity? {
        return budgetDao?.getBudgetByCategory(categoryId)
    }
}

private fun RecurringPatternEntity.toDomain() = RecurringPattern(
    normalizedDescription = normalizedDescription,
    avgAmount = avgAmount,
    intervalDays = intervalDays,
    lastSeenDate = lastSeenDate,
    nextExpectedDate = nextExpectedDate,
    confidence = confidence,
    categoryId = categoryId,
    transactionType = transactionType
)

private fun RecurringPattern.toEntity() = RecurringPatternEntity(
    normalizedDescription = normalizedDescription,
    avgAmount = avgAmount,
    intervalDays = intervalDays,
    lastSeenDate = lastSeenDate,
    nextExpectedDate = nextExpectedDate,
    confidence = confidence,
    categoryId = categoryId,
    transactionType = transactionType
)
