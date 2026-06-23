package com.makemission.makeflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _transactions = financeRepository.getAllTransactions()

    val transactions: StateFlow<List<TransactionEntity>> = _transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = financeRepository.getAllAccounts()
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

    val selectedFilter = MutableStateFlow("ALL")

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        _transactions, selectedFilter.asStateFlow()
    ) { allTxs, filter ->
        when (filter) {
            "INCOME" -> allTxs.filter { it.type == "INCOME" }
            "EXPENSE" -> allTxs.filter { it.type == "EXPENSE" }
            else -> allTxs
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyDeleted = MutableStateFlow<TransactionEntity?>(null)

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            recentlyDeleted.value = transaction
            financeRepository.deleteTransaction(transaction)
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            recentlyDeleted.value?.let { transaction ->
                financeRepository.insertTransaction(transaction)
                recentlyDeleted.value = null
            }
        }
    }
}

class HistoryViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(financeRepository, userPreferencesDataStore) as T
    }
}
