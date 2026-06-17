package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.local.dao.CategorySpending
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getMonthlySpendingByCategory(monthStartMillis: Long, monthEndMillis: Long): Flow<List<CategorySpending>>
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getMonthlyTotalExpense(monthStartMillis: Long, monthEndMillis: Long): Flow<Double>
    suspend fun insertCategory(category: CategoryEntity)
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)
    suspend fun deleteCategoryById(id: String)
    suspend fun insertTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
}
