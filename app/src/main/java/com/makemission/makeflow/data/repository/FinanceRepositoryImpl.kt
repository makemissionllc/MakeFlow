package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.local.dao.CategoryDao
import com.makemission.makeflow.data.local.dao.CategorySpending
import com.makemission.makeflow.data.local.dao.TransactionDao
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

class FinanceRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : FinanceRepository {

    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override fun getMonthlySpendingByCategory(
        monthStartMillis: Long,
        monthEndMillis: Long
    ): Flow<List<CategorySpending>> =
        categoryDao.getMonthlySpendingByCategory(monthStartMillis, monthEndMillis)

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    override fun getMonthlyTotalExpense(
        monthStartMillis: Long,
        monthEndMillis: Long
    ): Flow<Double> = transactionDao.getMonthlyTotalExpense(monthStartMillis, monthEndMillis)

    override suspend fun insertCategory(category: CategoryEntity) = categoryDao.insertCategory(category)

    override suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    override suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    override suspend fun deleteCategoryById(id: String) = categoryDao.deleteCategoryById(id)

    override suspend fun insertTransaction(transaction: TransactionEntity) =
        transactionDao.insertTransaction(transaction)

    override suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)
}
