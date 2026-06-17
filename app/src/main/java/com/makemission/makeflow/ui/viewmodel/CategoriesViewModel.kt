package com.makemission.makeflow.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.ui.state.CategoryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

class CategoriesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepositoryImpl(db.categoryDao(), db.transactionDao())
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val (monthStart, monthEnd) = currentMonthRange()

            combine(
                repository.getAllCategories(),
                repository.getMonthlySpendingByCategory(monthStart, monthEnd)
            ) { categories, spending ->
                val spendingMap = spending.associate { it.categoryId to it.totalSpent }
                CategoryUiState(
                    isLoading = false,
                    categories = categories,
                    totalBudget = categories.sumOf { it.monthlyBudget },
                    totalSpent = spendingMap.values.sum(),
                    spendingMap = spendingMap
                )
            }
                .catch { e ->
                    _uiState.value = CategoryUiState(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load categories"
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onAddCategory(name: String, budget: Double, colorHex: String) {
        viewModelScope.launch {
            repository.insertCategory(
                CategoryEntity(
                    name = name.trim(),
                    monthlyBudget = budget,
                    colorHex = colorHex
                )
            )
        }
    }

    fun onDeleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategoryById(id)
        }
    }

    fun onAddTransaction(amount: Double, type: String, categoryId: String?, note: String) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    note = note.trim()
                )
            )
        }
    }

    private fun currentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return Pair(start, cal.timeInMillis)
    }
}
