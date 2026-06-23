package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.local.dao.CategoryDao
import com.makemission.makeflow.data.local.dao.CategorySpending
import com.makemission.makeflow.data.local.dao.TaskDao
import com.makemission.makeflow.data.local.dao.TransactionDao
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.CategoryWithTasks
import com.makemission.makeflow.data.local.entity.TaskEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FinanceRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val taskDao: TaskDao,
    private val accountDao: com.makemission.makeflow.data.local.dao.AccountDao? = null,
    private val budgetDao: com.makemission.makeflow.data.local.dao.BudgetDao? = null
) : FinanceRepository {

    override suspend fun insertAccount(account: AccountEntity) {
        accountDao?.insertAccount(account)
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
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        accountDao?.getAccountById(transaction.accountId)?.let { account ->
            val delta = if (transaction.type == "INCOME") -transaction.amount else transaction.amount
            accountDao?.updateAccount(account.copy(balance = account.balance + delta))
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
}
