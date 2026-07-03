package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.local.dao.CategorySpending
import com.makemission.makeflow.data.local.dao.DescriptionCategoryPair
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.CategoryWithTasks
import com.makemission.makeflow.data.local.entity.TaskEntity
import com.makemission.makeflow.data.RecurringPattern
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.domain.BudgetSuggestion
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    suspend fun insertAccount(account: AccountEntity)
    suspend fun updateAccount(account: AccountEntity)
    suspend fun deleteAccount(account: AccountEntity)
    fun getAllAccounts(): Flow<List<AccountEntity>>
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getMonthlySpendingByCategory(monthStartMillis: Long, monthEndMillis: Long): Flow<List<CategorySpending>>
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>
    fun getMonthlyTotalExpense(monthStartMillis: Long, monthEndMillis: Long): Flow<Double>
    fun getMonthlyTotalIncome(monthStartMillis: Long, monthEndMillis: Long): Flow<Double>
    suspend fun insertCategory(category: CategoryEntity)
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)
    suspend fun deleteCategoryById(id: String)
    suspend fun insertTransaction(transaction: TransactionEntity)
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    fun getCategoryWithTasks(id: String): Flow<CategoryWithTasks?>
    suspend fun updateTaskCategory(taskId: String, categoryId: String?)
    suspend fun updateTask(task: TaskEntity)
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    suspend fun insertBudget(budget: BudgetEntity)
    suspend fun deleteBudget(budget: BudgetEntity)
    suspend fun syncBudgetSpending(categoryId: String?, amount: Double, type: String)
    suspend fun clearAllTransactions()
    suspend fun clearAllAccounts()
    suspend fun clearAllBudgets()
    suspend fun clearAllCategories()

    suspend fun refreshRecurringPatterns()
    fun getUpcomingRecurring(fromDate: Long): Flow<List<RecurringPattern>>

    suspend fun getBudgetSuggestions(
        dismissedKeys: Set<String>
    ): List<BudgetSuggestion>

    suspend fun updateBudgetAmount(budgetId: String, newAmount: Double)

    suspend fun getBudgetByCategory(categoryId: String): BudgetEntity?

    suspend fun getRecentAmountsForCategory(categoryId: String, excludingTransactionId: String? = null, limit: Int = 30): List<Double>

    suspend fun getRecentDescriptionsAndCategories(limit: Int = 100): List<DescriptionCategoryPair>
}
