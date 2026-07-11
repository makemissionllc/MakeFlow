package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.RecurringDetector
import com.makemission.makeflow.data.RecurringPattern
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
import com.makemission.makeflow.domain.SuggestionReason
import com.makemission.makeflow.domain.BudgetSuggestionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

class FinanceRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val taskDao: TaskDao,
    private val accountDao: com.makemission.makeflow.data.local.dao.AccountDao? = null,
    private val budgetDao: com.makemission.makeflow.data.local.dao.BudgetDao? = null,
    private val recurringPatternDao: RecurringPatternDao? = null,
    private val recurringTransactionDao: RecurringTransactionDao? = null,
    private val exchangeRateDao: com.makemission.makeflow.data.local.dao.ExchangeRateDao? = null
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

    override suspend fun getMonthlyTotalExpenseOnce(
        monthStartMillis: Long,
        monthEndMillis: Long
    ): Double = transactionDao.getMonthlyTotalExpenseOnce(monthStartMillis, monthEndMillis)

    override suspend fun getMonthlyTotalIncomeOnce(
        monthStartMillis: Long,
        monthEndMillis: Long
    ): Double = transactionDao.getMonthlyTotalIncomeOnce(monthStartMillis, monthEndMillis)

    override suspend fun insertCategory(category: CategoryEntity) = categoryDao.insertCategory(category)

    override suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    override suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    override suspend fun deleteCategoryById(id: String) = categoryDao.deleteCategoryById(id)

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
        if (transaction.type == "TRANSFER" && transaction.fromAccountId != null && transaction.toAccountId != null) {
            accountDao?.getAccountById(transaction.fromAccountId!!)?.let { from ->
                accountDao?.updateAccount(from.copy(balance = from.balance - transaction.amount))
            }
            accountDao?.getAccountById(transaction.toAccountId!!)?.let { to ->
                accountDao?.updateAccount(to.copy(balance = to.balance + transaction.amount))
            }
        } else {
            accountDao?.getAccountById(transaction.accountId)?.let { account ->
                val delta = if (transaction.type == "INCOME") transaction.amount else -transaction.amount
                accountDao?.updateAccount(account.copy(balance = account.balance + delta))
            }
            if (transaction.categoryId != null && transaction.type == "EXPENSE") {
                budgetDao?.updateBudgetSpending(transaction.categoryId, transaction.amount)
            }
        }
        if (transaction.type != "TRANSFER") {
            refreshRecurringPatterns()
        }
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.getTransactionById(transaction.id)?.let { old ->
            if (old.type == "TRANSFER" && old.fromAccountId != null && old.toAccountId != null) {
                accountDao?.getAccountById(old.fromAccountId!!)?.let { from ->
                    accountDao?.updateAccount(from.copy(balance = from.balance + old.amount))
                }
                accountDao?.getAccountById(old.toAccountId!!)?.let { to ->
                    accountDao?.updateAccount(to.copy(balance = to.balance - old.amount))
                }
            } else {
                accountDao?.getAccountById(old.accountId)?.let { account ->
                    val reverseDelta = if (old.type == "INCOME") -old.amount else old.amount
                    accountDao?.updateAccount(account.copy(balance = account.balance + reverseDelta))
                }
                if (old.categoryId != null && old.type == "EXPENSE") {
                    budgetDao?.decrementBudgetSpending(old.categoryId, old.amount)
                }
            }
        }
        transactionDao.updateTransaction(transaction)
        if (transaction.type == "TRANSFER" && transaction.fromAccountId != null && transaction.toAccountId != null) {
            accountDao?.getAccountById(transaction.fromAccountId!!)?.let { from ->
                accountDao?.updateAccount(from.copy(balance = from.balance - transaction.amount))
            }
            accountDao?.getAccountById(transaction.toAccountId!!)?.let { to ->
                accountDao?.updateAccount(to.copy(balance = to.balance + transaction.amount))
            }
        } else {
            accountDao?.getAccountById(transaction.accountId)?.let { account ->
                val delta = if (transaction.type == "INCOME") transaction.amount else -transaction.amount
                accountDao?.updateAccount(account.copy(balance = account.balance + delta))
            }
            if (transaction.categoryId != null && transaction.type == "EXPENSE") {
                budgetDao?.updateBudgetSpending(transaction.categoryId, transaction.amount)
            }
        }
        if (transaction.type != "TRANSFER") {
            refreshRecurringPatterns()
        }
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        if (transaction.type == "TRANSFER" && transaction.fromAccountId != null && transaction.toAccountId != null) {
            accountDao?.getAccountById(transaction.fromAccountId!!)?.let { from ->
                accountDao?.updateAccount(from.copy(balance = from.balance + transaction.amount))
            }
            accountDao?.getAccountById(transaction.toAccountId!!)?.let { to ->
                accountDao?.updateAccount(to.copy(balance = to.balance - transaction.amount))
            }
        } else {
            accountDao?.getAccountById(transaction.accountId)?.let { account ->
                val delta = if (transaction.type == "INCOME") -transaction.amount else transaction.amount
                accountDao?.updateAccount(account.copy(balance = account.balance + delta))
            }
            if (transaction.categoryId != null && transaction.type == "EXPENSE") {
                budgetDao?.decrementBudgetSpending(transaction.categoryId, transaction.amount)
            }
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
        val incomeExpenseOnly = allTransactions.filter { it.type != "TRANSFER" }
        val patterns = withContext(Dispatchers.Default) {
            RecurringDetector.detect(incomeExpenseOnly)
        }
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

    override suspend fun getRecentAmountsForCategory(categoryId: String, excludingTransactionId: String?, limit: Int): List<Double> {
        return transactionDao.getRecentAmountsForCategory(categoryId, excludingTransactionId, limit)
    }

    override suspend fun getRecentDescriptionsAndCategories(limit: Int): List<DescriptionCategoryPair> {
        return transactionDao.getRecentDescriptionsAndCategories(limit)
    }

    override suspend fun createRecurringTransaction(entity: RecurringTransactionEntity) {
        recurringTransactionDao?.insert(entity)
    }

    override suspend fun updateRecurringTransaction(entity: RecurringTransactionEntity) {
        recurringTransactionDao?.update(entity)
    }

    override suspend fun deleteRecurringTransaction(entity: RecurringTransactionEntity) {
        recurringTransactionDao?.delete(entity)
    }

    override suspend fun toggleRecurringTransactionActive(id: String) {
        recurringTransactionDao?.getById(id)?.let { entity ->
            recurringTransactionDao?.update(entity.copy(isActive = !entity.isActive))
        }
    }

    override suspend fun skipRecurringTransaction(id: String) {
        recurringTransactionDao?.getById(id)?.let { entity ->
            val nextDue = advanceDate(entity.nextDueDate, entity.intervalDays)
            recurringTransactionDao?.update(entity.copy(nextDueDate = nextDue))
        }
    }

    override suspend fun processDueRecurringTransactions() {
        val now = System.currentTimeMillis()
        val dueItems = recurringTransactionDao?.getDueTodayOrOverdueOnce(now) ?: return
        for (item in dueItems) {
            if (item.endDate != null && item.nextDueDate > item.endDate) {
                recurringTransactionDao?.update(item.copy(isActive = false))
                continue
            }
            if (item.autoConfirm) {
                val tx = TransactionEntity(
                    amount = item.amount,
                    type = item.type,
                    categoryId = item.categoryId,
                    accountId = item.accountId,
                    note = item.description,
                    timestamp = System.currentTimeMillis()
                )
                insertTransaction(tx)
            }
            val nextDue = advanceDate(item.nextDueDate, item.intervalDays)
            val updated = item.copy(nextDueDate = nextDue)
            if (updated.endDate != null && nextDue > updated.endDate) {
                recurringTransactionDao?.update(updated.copy(isActive = false))
            } else {
                recurringTransactionDao?.update(updated)
            }
        }
    }

    override fun getAllActiveRecurringTransactions(): Flow<List<RecurringTransactionEntity>> =
        recurringTransactionDao?.getAllActive() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    override fun getDueRecurringTransactions(now: Long): Flow<List<RecurringTransactionEntity>> =
        recurringTransactionDao?.getDueTodayOrOverdue(now) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun mergeCategories(sourceId: String, targetId: String): String? {
        val source = categoryDao.getCategoryById(sourceId) ?: return "Source category not found."
        val target = categoryDao.getCategoryById(targetId) ?: return "Target category not found."
        if (source.type != target.type) return "Cannot merge categories of different types."
        if (source.id == target.id) return "Cannot merge a category into itself."
        val childCount = categoryDao.getChildCategoryCount(sourceId)
        if (childCount > 0) return "Cannot merge \"${source.name}\" because it has $childCount subcategories. Remove or reassign them first."
        categoryDao.mergeCategoryAtomic(sourceId, targetId, target.name)
        return null
    }

    override suspend fun convertCategoryType(categoryId: String, newType: String): String? {
        val category = categoryDao.getCategoryById(categoryId) ?: return "Category not found."
        if (category.type == newType) return null
        val txCount = categoryDao.getTransactionCountForCategory(categoryId)
        if (txCount > 0) return "Cannot convert \"${category.name}\" because it has $txCount transaction(s). Remove or reassign them first."
        categoryDao.updateCategory(category.copy(type = newType))
        return null
    }

    override suspend fun getConvertedTotalBalance(displayCurrency: String): Double {
        val accounts = accountDao?.let {
            it.getAllAccounts().first()
        } ?: return 0.0
        val rates = exchangeRateDao?.getAllRatesOnce() ?: emptyList()
        val rateMap = rates.associate { it.currencyCode to it.rateToDisplayCurrency }
        return accounts.sumOf { account ->
            val acctCurrency = account.currencyCode ?: displayCurrency
            if (acctCurrency == displayCurrency) {
                account.balance
            } else {
                val rate = rateMap[acctCurrency] ?: return@sumOf account.balance
                account.balance * rate
            }
        }
    }
}

private fun advanceDate(fromDate: Long, intervalDays: Int): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = fromDate
    cal.add(java.util.Calendar.DAY_OF_YEAR, intervalDays)
    return cal.timeInMillis
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
