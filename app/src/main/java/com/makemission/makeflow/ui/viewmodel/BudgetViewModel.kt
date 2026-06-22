package com.makemission.makeflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class BudgetViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val budgets: StateFlow<List<BudgetEntity>> = financeRepository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = financeRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCurrency: StateFlow<String> = userPreferencesDataStore.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    val currencySymbol: StateFlow<String> = selectedCurrency.map { code ->
        when (code) {
            "USD" -> "$"
            "EUR" -> "\u20AC"
            "GBP" -> "\u00A3"
            "UGX" -> "UGX"
            "TZS" -> "TZS"
            else -> "KES"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    fun addBudget(categoryId: String, categoryName: String, amount: Double, period: String) {
        viewModelScope.launch {
            financeRepository.insertBudget(
                BudgetEntity(
                    id = UUID.randomUUID().toString(),
                    categoryId = categoryId,
                    categoryName = categoryName,
                    amount = amount,
                    spentAmount = 0.0,
                    period = period,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            financeRepository.deleteBudget(budget)
        }
    }
}

class BudgetViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BudgetViewModel(financeRepository, userPreferencesDataStore) as T
    }
}
