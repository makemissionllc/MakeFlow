package com.makemission.makeflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.data.repository.ExchangeRateRepository
import com.makemission.makeflow.domain.CurrencyConverter
import com.makemission.makeflow.domain.CurrencyRate
import java.util.UUID

class AccountsViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val exchangeRateRepository: ExchangeRateRepository? = null
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = financeRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCurrency: StateFlow<String> = userPreferencesDataStore.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    val exchangeRates: StateFlow<List<ExchangeRateEntity>> =
        (exchangeRateRepository?.getAllRates() ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val conversionRates = exchangeRates.map { rates ->
        rates.mapNotNull { rate ->
            rate.baseCurrencyCode?.let { base ->
                CurrencyRate(rate.currencyCode, base, rate.rateToDisplayCurrency, rate.rateDate)
            }
        }
    }

    val totalBalance: StateFlow<Double> = combine(
        accounts,
        selectedCurrency,
        conversionRates
    ) { accts, displayCurrency, rates ->
        accts.sumOf { account ->
            val acctCurrency = account.currencyCode ?: displayCurrency
            CurrencyConverter.convert(account.balance, acctCurrency, displayCurrency, rates) ?: 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val unconvertedAccountCount: StateFlow<Int> = combine(
        accounts,
        selectedCurrency,
        conversionRates
    ) { accts, displayCurrency, rates ->
        accts.count { account ->
            val acctCurrency = account.currencyCode ?: displayCurrency
            CurrencyConverter.convert(account.balance, acctCurrency, displayCurrency, rates) == null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currencySymbol: StateFlow<String> = selectedCurrency.map { code ->
        CurrencyFormatter.symbolFor(code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    suspend fun addAccount(name: String, balance: Double, colorHex: String, currencyCode: String? = null) {
        financeRepository.insertAccount(
            AccountEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                balance = balance,
                initialBalance = balance,
                colorHex = colorHex,
                currencyCode = currencyCode
            )
        )
    }

    suspend fun updateAccount(
        existing: AccountEntity,
        newName: String,
        newBalance: Double,
        newColorHex: String,
        newCurrencyCode: String? = existing.currencyCode
    ) {
        financeRepository.updateAccount(
            existing.copy(
                name = newName,
                balance = newBalance,
                colorHex = newColorHex,
                currencyCode = newCurrencyCode
            )
        )
    }

    suspend fun deleteAccount(account: AccountEntity) {
        financeRepository.deleteAccount(account)
    }
}

class AccountsViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val exchangeRateRepository: ExchangeRateRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AccountsViewModel(financeRepository, userPreferencesDataStore, exchangeRateRepository) as T
    }
}
