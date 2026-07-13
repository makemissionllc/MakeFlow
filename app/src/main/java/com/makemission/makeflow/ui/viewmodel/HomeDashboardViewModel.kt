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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import com.makemission.makeflow.domain.CashFlowProjection
import com.makemission.makeflow.domain.CashFlowProjector
import com.makemission.makeflow.domain.DashboardCardRanker
import com.makemission.makeflow.domain.DashboardCardType
import com.makemission.makeflow.domain.SpendingVelocityTracker
import com.makemission.makeflow.domain.VelocityAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

class HomeDashboardViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val cardInteractionDao: CardInteractionDao,
    private val exchangeRateRepository: ExchangeRateRepository? = null
) : ViewModel() {

    val selectedCurrency: StateFlow<String> = userPreferencesDataStore.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    val totalBalance: StateFlow<Double> = combine(
        financeRepository.getAllAccounts(),
        selectedCurrency,
        exchangeRateRepository?.getAllRates() ?: flowOf(emptyList())
    ) { accts, displayCurrency, rates ->
        val rateMap = rates.associate { it.currencyCode to it.rateToDisplayCurrency }
        accts.sumOf { account ->
            val acctCurrency = account.currencyCode ?: displayCurrency
            if (acctCurrency == displayCurrency) account.balance
            else rateMap[acctCurrency]?.let { account.balance * it } ?: account.balance
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val accounts: StateFlow<List<AccountEntity>> = financeRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = financeRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currencySymbol: StateFlow<String> = selectedCurrency.map { code ->
        CurrencyFormatter.symbolFor(code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    private val monthRange = getCurrentMonthRange()
    private val monthStart = monthRange.first
    private val monthEnd = monthRange.second

    val monthlyIncome: StateFlow<Double> = financeRepository.getMonthlyTotalIncome(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyExpenses: StateFlow<Double> = financeRepository.getMonthlyTotalExpense(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categorySpending: StateFlow<List<CategorySpending>> = financeRepository.getMonthlySpendingByCategory(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allTransactions = financeRepository.getAllTransactions()

    val parentCategorySpending: StateFlow<List<ParentCategorySpending>> = combine(
        allTransactions, categories
    ) { txs, cats ->
        val childToParent = cats.filter { it.parentCategoryId != null }
            .associate { it.id to it.parentCategoryId!! }
        val parents = cats.filter { it.parentCategoryId == null }.associateBy { it.id }
        val expenseTxs = txs.filter {
            it.type == "EXPENSE" && it.timestamp in monthStart..monthEnd
        }
        val grouped = expenseTxs.groupBy { tx ->
            childToParent[tx.categoryId] ?: tx.categoryId ?: ""
        }
        grouped.map { (parentId, parentTxs) ->
            val parent = parents[parentId]
            ParentCategorySpending(
                categoryId = parentId,
                categoryName = parent?.name ?: "Uncategorized",
                color = parent?.colorHex ?: "#9E9E9E",
                amount = parentTxs.sumOf { it.amount }
            )
        }.sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = financeRepository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val thisMonthTransactions: StateFlow<List<TransactionEntity>> =
        financeRepository.getTransactionsInRange(monthStart, monthEnd)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
