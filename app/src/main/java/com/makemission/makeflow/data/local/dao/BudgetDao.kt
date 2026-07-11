package com.makemission.makeflow.data.local.dao

import androidx.room.*
import com.makemission.makeflow.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY createdAt DESC")
    fun getAllBudgetsFlow(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY createdAt DESC")
    suspend fun getAllBudgetsOnce(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET spentAmount = spentAmount + :amount WHERE categoryId = :categoryId")
    suspend fun updateBudgetSpending(categoryId: String, amount: Double)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET spentAmount = MAX(spentAmount - :amount, 0) WHERE categoryId = :categoryId")
    suspend fun decrementBudgetSpending(categoryId: String, amount: Double)

    @Query("UPDATE budgets SET amount = :newAmount WHERE id = :budgetId")
    suspend fun updateBudgetAmount(budgetId: String, newAmount: Double)

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getBudgetByCategory(categoryId: String): BudgetEntity?

    @Query("DELETE FROM budgets")
    suspend fun clearAll()
}
