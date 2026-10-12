package com.makemission.makeflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import com.makemission.makeflow.domain.HistoryPeriod
import com.makemission.makeflow.domain.HistorySummary
import com.makemission.makeflow.domain.MpesaFeeMatcher
import java.time.ZonedDateTime
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val currentTime: () -> ZonedDateTime = { ZonedDateTime.now() }
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
        CurrencyFormatter.symbolFor(code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    val selectedFilter = MutableStateFlow("ALL")
    val searchQuery = MutableStateFlow("")
    val selectedPeriod = MutableStateFlow(HistoryPeriod.ALL_TIME)
    private val _calendarNow = MutableStateFlow(currentTime())
    val calendarNow = _calendarNow.asStateFlow()

    private val dateWindow = combine(selectedPeriod, calendarNow) { period, now ->
        period.timestampRange(now.toLocalDate(), now.zone)
    }

    val focusedAccountId: StateFlow<String?> = userPreferencesDataStore.focusedAccountId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _focusedAccountReady = MutableStateFlow(false)
    val focusedAccountReady: StateFlow<Boolean> = _focusedAccountReady.asStateFlow()

    private val _focusedAccountState = MutableStateFlow<FocusedAccountState>(FocusedAccountState.Loading)
    val focusedAccountState: StateFlow<FocusedAccountState> = _focusedAccountState.asStateFlow()

    val focusedAccount: StateFlow<AccountEntity?> = combine(
        accounts, focusedAccountId
    ) { accts, focusedId ->
        if (focusedId != null) accts.find { it.id == focusedId } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val typeAndAccountFilteredTransactions = combine(
        _transactions, selectedFilter.asStateFlow(), focusedAccountId
    ) { allTxs, filter, focusedId ->
        val typeFiltered = when (filter) {
            "INCOME" -> allTxs.filter { it.type == "INCOME" }
            "EXPENSE" -> allTxs.filter { it.type == "EXPENSE" }
            "FEES" -> MpesaFeeMatcher.feeTransactions(allTxs)
            else -> allTxs
        }
        if (focusedId != null) {
            typeFiltered.filter { tx ->
                tx.accountId == focusedId ||
                    tx.fromAccountId == focusedId ||
                    tx.toAccountId == focusedId
            }
        } else {
            typeFiltered
        }
    }

    private val dateFilteredTransactions = combine(
        typeAndAccountFilteredTransactions, dateWindow
    ) { filtered, range ->
        if (range == null) filtered else filtered.filter { it.timestamp in range }
    }

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        dateFilteredTransactions,
        searchQuery.asStateFlow(),
        accounts,
        categories
    ) { filtered, query, accountList, categoryList ->
        val normalizedQuery = query.normalizeTransactionSearchText()
        if (normalizedQuery.isEmpty()) {
            filtered
        } else {
            val accountNames = accountList.associate { it.id to it.name }
            val categoryNames = categoryList.associate { it.id to it.name }
            filtered.filter { transaction ->
                listOf(
                    transaction.note,
                    transaction.type,
                    transaction.amount.toString(),
                    transaction.destinationAmount?.toString().orEmpty(),
                    transaction.externalReference.orEmpty(),
                    transaction.categoryId?.let(categoryNames::get).orEmpty(),
                    accountNames[transaction.accountId].orEmpty(),
                    transaction.fromAccountId?.let(accountNames::get).orEmpty(),
                    transaction.toAccountId?.let(accountNames::get).orEmpty()
                ).joinToString(" ").normalizeTransactionSearchText().contains(normalizedQuery)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val resultSummary: StateFlow<HistorySummary.Result> = combine(
        filteredTransactions, accounts, selectedCurrency
    ) { filtered, accountList, currency ->
        HistorySummary.summarize(filtered, accountList, currency)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistorySummary.Result())

    val recentlyDeleted = MutableStateFlow<TransactionEntity?>(null)

    init {
        viewModelScope.launch {
            userPreferencesDataStore.focusedAccountId.collect { id ->
                if (!_focusedAccountReady.value) _focusedAccountReady.value = true
                _focusedAccountState.value = if (id != null) {
                    FocusedAccountState.Focused(id)
                } else {
                    FocusedAccountState.ShowAll
                }
            }
        }
    }

    fun refreshDateFilters() {
        _calendarNow.value = currentTime()
    }

    fun selectAccount(accountId: String?) {
        viewModelScope.launch {
            if (accountId == null || financeRepository.getAllAccounts().first().any { it.id == accountId }) {
                userPreferencesDataStore.saveFocusedAccountId(accountId)
            }
        }
    }

    fun clearFilters() {
        searchQuery.value = ""
        selectedFilter.value = "ALL"
        selectedPeriod.value = HistoryPeriod.ALL_TIME
    }

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

private fun String.normalizeTransactionSearchText(): String =
    lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

class HistoryViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(financeRepository, userPreferencesDataStore) as T
    }
}
