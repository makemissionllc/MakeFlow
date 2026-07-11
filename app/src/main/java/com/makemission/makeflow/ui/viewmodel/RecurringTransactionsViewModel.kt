package com.makemission.makeflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.RecurringTransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurringTransactionsViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val recurringTransactions: StateFlow<List<RecurringTransactionEntity>> =
        financeRepository.getAllActiveRecurringTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = financeRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = financeRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currencySymbol: StateFlow<String> = userPreferencesDataStore.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    private val _editEntity = MutableStateFlow<RecurringTransactionEntity?>(null)
    val editEntity: StateFlow<RecurringTransactionEntity?> = _editEntity.asStateFlow()

    fun setEditEntity(entity: RecurringTransactionEntity?) {
        _editEntity.value = entity
    }

    fun save(entity: RecurringTransactionEntity) {
        viewModelScope.launch {
            val existing = _editEntity.value
            if (existing != null) {
                financeRepository.updateRecurringTransaction(entity)
            } else {
                financeRepository.createRecurringTransaction(entity)
            }
            _editEntity.value = null
        }
    }

    fun delete(entity: RecurringTransactionEntity) {
        viewModelScope.launch {
            financeRepository.deleteRecurringTransaction(entity)
        }
    }

    fun toggleActive(id: String) {
        viewModelScope.launch {
            financeRepository.toggleRecurringTransactionActive(id)
        }
    }

    fun skip(entity: RecurringTransactionEntity) {
        viewModelScope.launch {
            financeRepository.skipRecurringTransaction(entity.id)
        }
    }
}

class RecurringTransactionsViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RecurringTransactionsViewModel(financeRepository, userPreferencesDataStore) as T
    }
}
