package com.makemission.makeflow.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.ui.state.OnboardingUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository: FinanceRepository by lazy {
        FinanceRepositoryImpl(
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            taskDao = db.taskDao(),
            accountDao = db.accountDao()
        )
    }

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val accounts = db.accountDao().getAllAccounts().first()
            if (accounts.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(isCompleted = true)
            }
        }
    }

    fun onPageChange(page: Int) {
        _uiState.value = _uiState.value.copy(currentPage = page)
    }

    fun onUsernameChange(name: String) {
        _uiState.value = _uiState.value.copy(username = name)
    }

    fun onCurrencyChange(currency: String) {
        _uiState.value = _uiState.value.copy(baseCurrency = currency)
    }

    fun onBalanceChange(balance: String) {
        _uiState.value = _uiState.value.copy(startingBalance = balance)
    }

    fun onToggleCategory(category: String) {
        val current = _uiState.value.activeCategories
        val updated = if (category in current) current - category else current + category
        _uiState.value = _uiState.value.copy(activeCategories = updated)
    }

    fun onContinue() {
        val state = _uiState.value
        if (state.currentPage < state.totalPages - 1) {
            onPageChange(state.currentPage + 1)
        } else {
            completeOnboarding(
                username = state.username,
                currency = state.baseCurrency,
                balance = state.startingBalance,
                selectedCategories = state.activeCategories
            )
        }
    }

    fun onSkip() {
        onPageChange(_uiState.value.currentPage + 1)
    }

    fun completeOnboarding(
        username: String,
        currency: String,
        balance: String,
        selectedCategories: Set<String>
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompleting = true)
            val initialBalance = balance.toDoubleOrNull() ?: 0.0
            repository.insertAccount(
                AccountEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = "${username}'s Account",
                    balance = initialBalance,
                    initialBalance = initialBalance,
                    colorHex = "#1E88E5"
                )
            )
            selectedCategories.forEach { name ->
                val template = OnboardingUiState.categoryTemplates.find { it.name == name }
                val catType = if (name == "Income" || name == "Salary" || name == "Freelance") "INCOME" else "EXPENSE"
                repository.insertCategory(
                    CategoryEntity(
                        name = name,
                        type = catType,
                        iconName = template?.iconName ?: "Check",
                        colorHex = template?.colorHex ?: "#3949AB",
                        monthlyBudget = if (catType == "INCOME") 0.0 else 500.0
                    )
                )
            }
            if (initialBalance > 0.0) {
                repository.insertTransaction(
                    TransactionEntity(
                        amount = initialBalance,
                        type = "INCOME",
                        note = "Initial deposit"
                    )
                )
            }
            delay(800)
            _uiState.value = _uiState.value.copy(isCompleting = false, isCompleted = true)
        }
    }
}
