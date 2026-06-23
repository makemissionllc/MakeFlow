package com.makemission.makeflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

class AccountsViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = financeRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = accounts.map { list ->
        list.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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

    suspend fun addAccount(name: String, balance: Double, colorHex: String) {
        financeRepository.insertAccount(
            AccountEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                balance = balance,
                initialBalance = balance,
                colorHex = colorHex
            )
        )
    }

    suspend fun updateAccount(existing: AccountEntity, newName: String, newBalance: Double, newColorHex: String) {
        financeRepository.updateAccount(
            existing.copy(
                name = newName,
                balance = newBalance,
                colorHex = newColorHex
            )
        )
    }

    suspend fun deleteAccount(account: AccountEntity) {
        financeRepository.deleteAccount(account)
    }
}

class AccountsViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AccountsViewModel(financeRepository, userPreferencesDataStore) as T
    }
}