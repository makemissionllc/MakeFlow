package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.CategoryWithTasks
import kotlinx.coroutines.flow.Flow

data class CategorySpending(
    val categoryId: String,
    val totalSpent: Double
)

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Query("""
        SELECT t.categoryId AS categoryId, SUM(t.amount) AS totalSpent
        FROM transactions t
        WHERE t.type = 'EXPENSE'
          AND t.timestamp >= :monthStartMillis
          AND t.timestamp <= :monthEndMillis
        GROUP BY t.categoryId
    """)
    fun getMonthlySpendingByCategory(
        monthStartMillis: Long,
        monthEndMillis: Long
    ): Flow<List<CategorySpending>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("SELECT * FROM categories LIMIT 1")
    suspend fun getAnyCategory(): CategoryEntity?

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Transaction
    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryWithTasks(id: String): Flow<CategoryWithTasks?>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int

    @Query("DELETE FROM categories")
    suspend fun clearAll()

    @Query("UPDATE transactions SET categoryId = :targetId WHERE categoryId = :sourceId")
    suspend fun reassignTransactionCategory(sourceId: String, targetId: String)

    @Query("UPDATE budgets SET categoryId = :targetId, categoryName = :targetName WHERE categoryId = :sourceId")
    suspend fun reassignBudgetCategory(sourceId: String, targetId: String, targetName: String)

    @Query("SELECT COUNT(*) FROM categories WHERE parentCategoryId = :categoryId")
    suspend fun getChildCategoryCount(categoryId: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun getTransactionCountForCategory(categoryId: String): Int

    @Transaction
    suspend fun mergeCategoryAtomic(sourceId: String, targetId: String, targetName: String) {
        reassignTransactionCategory(sourceId, targetId)
        reassignBudgetCategory(sourceId, targetId, targetName)
        deleteCategoryById(sourceId)
    }
}
