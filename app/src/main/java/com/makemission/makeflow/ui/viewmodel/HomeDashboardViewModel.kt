package com.makemission.makeflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.data.RecurringPattern
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.RecurringTransactionEntity
import com.makemission.makeflow.data.local.dao.CardHourCount
import com.makemission.makeflow.data.local.dao.CardInteractionDao
import com.makemission.makeflow.data.local.dao.CategorySpending
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CardInteractionEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import com.makemission.makeflow.data.repository.ExchangeRateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.makemission.makeflow.domain.CashFlowProjection
import com.makemission.makeflow.domain.CashFlowProjector
import com.makemission.makeflow.domain.CurrencyConverter
import com.makemission.makeflow.domain.CurrencyRate
import com.makemission.makeflow.domain.DashboardCardRanker
import com.makemission.makeflow.domain.DashboardCardType
import com.makemission.makeflow.domain.SpendingVelocityTracker
import com.makemission.makeflow.domain.VelocityAlert
import com.makemission.makeflow.domain.WeeklyBudgetCalculator
import com.makemission.makeflow.domain.WeeklyBudgetStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

data class ParentCategorySpending(
    val categoryId: String,
    val categoryName: String,
    val color: String,
    val amount: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeDashboardViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val cardInteractionDao: CardInteractionDao,
    private val exchangeRateRepository: ExchangeRateRepository? = null
) : ViewModel() {

    val selectedCurrency: StateFlow<String> = userPreferencesDataStore.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    val dashboardBalanceHidden: StateFlow<Boolean> = userPreferencesDataStore.dashboardBalanceHidden
        // Keep the first frame private until DataStore publishes the saved preference.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val accounts: StateFlow<List<AccountEntity>> = financeRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _focusedAccountReady = MutableStateFlow(false)
    val focusedAccountReady: StateFlow<Boolean> = _focusedAccountReady.asStateFlow()

    val focusedAccountId: StateFlow<String?> = userPreferencesDataStore.focusedAccountId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val monthRange = getCurrentMonthRange()
    private val monthStart = monthRange.first
    private val monthEnd = monthRange.second

    private val allTransactions = financeRepository.getAllTransactions()

    internal val thisMonthTransactions: StateFlow<List<TransactionEntity>> =
        focusedAccountId.flatMapLatest { focusedId ->
            if (focusedId != null) {
                financeRepository.getTransactionsInRangeForAccount(monthStart, monthEnd, focusedId)
            } else {
                financeRepository.getTransactionsInRange(monthStart, monthEnd)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val currencyRates: StateFlow<List<CurrencyRate>> =
        (exchangeRateRepository?.getAllRates() ?: flowOf(emptyList()))
            .map { savedRates ->
                savedRates.mapNotNull { rate ->
                    rate.baseCurrencyCode?.let { base ->
                        CurrencyRate(rate.currencyCode, base, rate.rateToDisplayCurrency, rate.rateDate)
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _focusedAccountState = MutableStateFlow<FocusedAccountState>(FocusedAccountState.Loading)
    val focusedAccountState: StateFlow<FocusedAccountState> = _focusedAccountState.asStateFlow()

    val focusedAccount: StateFlow<AccountEntity?> = combine(
        accounts, focusedAccountId
    ) { accts, focusedId ->
        if (focusedId != null) accts.find { it.id == focusedId } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val balanceCurrencyCode: StateFlow<String> = combine(
        accounts, focusedAccountId, selectedCurrency
    ) { accts, focusedId, displayCurrency ->
        if (focusedId != null) {
            val focused = accts.find { it.id == focusedId }
            focused?.currencyCode ?: displayCurrency
        } else {
            displayCurrency
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    val totalBalance: StateFlow<Double> = combine(
        accounts,
        focusedAccountId,
        selectedCurrency,
        currencyRates
    ) { accts, focusedId, displayCurrency, rates ->
        if (focusedId != null) {
            val focused = accts.find { it.id == focusedId }
            focused?.balance ?: 0.0
        } else {
            accts.sumOf { account ->
                val acctCurrency = account.currencyCode ?: displayCurrency
                CurrencyConverter.convert(account.balance, acctCurrency, displayCurrency, rates) ?: 0.0
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categories: StateFlow<List<CategoryEntity>> = financeRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currencySymbol: StateFlow<String> = selectedCurrency.map { code ->
        CurrencyFormatter.symbolFor(code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    private val monthlyTotals: StateFlow<Pair<Double, Double>> = combine(
        thisMonthTransactions,
        accounts,
        selectedCurrency,
        currencyRates
    ) { transactions, accts, displayCurrency, rates ->
        val accountCurrencies = accts.associate { it.id to (it.currencyCode ?: displayCurrency) }
        val converted = transactions.mapNotNull { transaction ->
            if (transaction.type != "INCOME" && transaction.type != "EXPENSE") return@mapNotNull null
            val sourceCurrency = accountCurrencies[transaction.accountId] ?: displayCurrency
            CurrencyConverter.convert(transaction.amount, sourceCurrency, displayCurrency, rates)
                ?.let { transaction.type to it }
        }
        converted.filter { it.first == "INCOME" }.sumOf { it.second } to
            converted.filter { it.first == "EXPENSE" }.sumOf { it.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0 to 0.0)

    val monthlyIncome: StateFlow<Double> = monthlyTotals
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyExpenses: StateFlow<Double> = monthlyTotals
        .map { it.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categorySpending: StateFlow<List<CategorySpending>> = financeRepository.getMonthlySpendingByCategory(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        focusedAccountId
    ) { transactions, accountId ->
        transactions.asSequence()
            .filter { transaction ->
                accountId == null || transaction.accountId == accountId ||
                    transaction.fromAccountId == accountId || transaction.toAccountId == accountId
            }
            .take(3)
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val parentCategorySpending: StateFlow<List<ParentCategorySpending>> = combine(
        thisMonthTransactions, categories, accounts, selectedCurrency, currencyRates
    ) { txs, cats, accts, displayCurrency, rates ->
        val accountCurrencies = accts.associate { it.id to (it.currencyCode ?: displayCurrency) }
        val childToParent = cats.filter { it.parentCategoryId != null }
            .associate { it.id to it.parentCategoryId!! }
        val parents = cats.filter { it.parentCategoryId == null }.associateBy { it.id }
        val grouped = txs.asSequence()
            .filter { it.type == "EXPENSE" }
            .mapNotNull { tx ->
                val sourceCurrency = accountCurrencies[tx.accountId] ?: displayCurrency
                val amount = CurrencyConverter.convert(tx.amount, sourceCurrency, displayCurrency, rates)
                    ?: return@mapNotNull null
                (childToParent[tx.categoryId] ?: tx.categoryId ?: "") to amount
            }
            .groupBy({ it.first }, { it.second })
        grouped.map { (parentId, parentTxs) ->
            val parent = parents[parentId]
            ParentCategorySpending(
                categoryId = parentId,
                categoryName = parent?.name ?: "Uncategorized",
                color = parent?.colorHex ?: "#9E9E9E",
                amount = parentTxs.sum()
            )
        }.sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = financeRepository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyBudgetStatus: StateFlow<WeeklyBudgetStatus> = combine(
        allTransactions, budgets, categories, focusedAccountId
    ) { transactions, budgetList, categoryList, accountId ->
        val weekStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        val weekEnd = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }
        WeeklyBudgetCalculator.calculate(
            budgets = budgetList,
            transactions = transactions,
            categories = categoryList,
            focusedAccountId = accountId,
            weekStartMillis = weekStart.timeInMillis,
            weekEndMillis = weekEnd.timeInMillis
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklyBudgetStatus(false, 0.0, 0.0))

    private val upcomingThreshold: Long
        get() = System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000

    val recurringPatterns: StateFlow<List<RecurringPattern>> = financeRepository
        .getUpcomingRecurring(System.currentTimeMillis())
        .map { patterns ->
            patterns.filter { it.nextExpectedDate <= upcomingThreshold }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRecurringTransactions: StateFlow<List<RecurringTransactionEntity>> = financeRepository
        .getDueRecurringTransactions(System.currentTimeMillis())
        .map { items -> items.filter { !it.autoConfirm && it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val defaultCardOrder = listOf(
        DashboardCardType.INSIGHTS_SUMMARY,
        DashboardCardType.ACCOUNTS,
        DashboardCardType.BUDGET_PROGRESS,
        DashboardCardType.RECURRING
    )

    private val _cardOrder = MutableStateFlow(defaultCardOrder)
    val cardOrder: StateFlow<List<DashboardCardType>> = _cardOrder

    val cashFlowProjection: StateFlow<CashFlowProjection?> = combine(
        totalBalance,
        thisMonthTransactions,
        recurringPatterns
    ) { balance, txs, patterns ->
        val today = LocalDate.now()
        if (today.dayOfMonth < 3) return@combine null
        CashFlowProjector.project(
            currentBalance = balance,
            allTransactionsThisMonth = txs,
            recurringPatterns = patterns,
            today = today
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val velocityAlerts: StateFlow<List<VelocityAlert>> = combine(
        allTransactions, budgets, categories
    ) { txs, budgetList, cats ->
        val childToParent = cats.filter { it.parentCategoryId != null }
            .associate { it.id to it.parentCategoryId!! }
        val now = System.currentTimeMillis()
        budgetList.mapNotNull { budget ->
            val catId = budget.categoryId ?: return@mapNotNull null
            val catTxs = txs.filter { tx ->
                val effectiveCat = childToParent[tx.categoryId] ?: tx.categoryId ?: ""
                tx.type == "EXPENSE" && effectiveCat == catId
            }
            SpendingVelocityTracker.compute(
                categoryId = catId,
                categoryName = budget.categoryName,
                categoryTransactions = catTxs,
                now = now
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFocusedAccountId(accountId: String?) {
        _focusedAccountState.value = if (accountId != null) {
            FocusedAccountState.Focused(accountId)
        } else {
            FocusedAccountState.ShowAll
        }
        if (!_focusedAccountReady.value) _focusedAccountReady.value = true
        viewModelScope.launch {
            userPreferencesDataStore.saveFocusedAccountId(accountId)
        }
    }

    fun setDashboardBalanceHidden(hidden: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.saveDashboardBalanceHidden(hidden)
        }
    }

    fun confirmRecurringTransaction(entity: RecurringTransactionEntity) {
        viewModelScope.launch {
            if (entity.autoConfirm) return@launch
            val tx = TransactionEntity(
                amount = entity.amount,
                type = entity.type,
                categoryId = entity.categoryId,
                accountId = entity.accountId,
                note = entity.description,
                timestamp = System.currentTimeMillis()
            )
            financeRepository.insertTransaction(tx)
            financeRepository.skipRecurringTransaction(entity.id)
        }
    }

    fun skipRecurringTransaction(entity: RecurringTransactionEntity) {
        viewModelScope.launch {
            financeRepository.skipRecurringTransaction(entity.id)
        }
    }

    fun saveManualCardOrder(order: List<DashboardCardType>) {
        viewModelScope.launch {
            userPreferencesDataStore.saveManualCardOrder(order.map { it.name })
        }
    }

    fun clearManualCardOrder() {
        viewModelScope.launch {
            userPreferencesDataStore.clearManualCardOrder()
        }
    }

    fun logCardInteraction(cardType: DashboardCardType) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            cardInteractionDao.insert(
                CardInteractionEntity(
                    cardType = cardType.name,
                    hourOfDay = hour,
                    timestamp = now
                )
            )
        }
    }

    private val _manualOrderOverride = MutableStateFlow<List<DashboardCardType>?>(null)

    init {
        viewModelScope.launch {
            financeRepository.refreshRecurringPatterns()
        }
        viewModelScope.launch {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val counts = cardInteractionDao.getInteractionCounts()
            val ranked = DashboardCardRanker.rank(currentHour, counts, defaultCardOrder)
            _manualOrderOverride.value = null
            userPreferencesDataStore.manualCardOrder.collect { rawNames ->
                if (rawNames != null) {
                    val parsed = rawNames.mapNotNull { name ->
                        try { DashboardCardType.valueOf(name) } catch (_: IllegalArgumentException) { null }
                    }
                    if (parsed.size == defaultCardOrder.size) {
                        _cardOrder.value = parsed
                        return@collect
                    }
                }
                _cardOrder.value = ranked
            }
        }
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
        viewModelScope.launch {
            combine(accounts, focusedAccountId) { accts, focusedId ->
                if (accts.isNotEmpty() && focusedId != null && accts.none { it.id == focusedId }) focusedId else null
            }.collect { staleId ->
                if (staleId != null) {
                    _focusedAccountState.value = FocusedAccountState.ShowAll
                    userPreferencesDataStore.saveFocusedAccountId(null)
                }
            }
        }
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis
        return start to end
    }
}

class HomeDashboardViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val cardInteractionDao: CardInteractionDao,
    private val exchangeRateRepository: ExchangeRateRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeDashboardViewModel(financeRepository, userPreferencesDataStore, cardInteractionDao, exchangeRateRepository) as T
    }
}
