package com.makemission.makeflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import com.makemission.makeflow.data.repository.ExchangeRateRepository
import com.makemission.makeflow.domain.CurrencyConverter
import com.makemission.makeflow.domain.CurrencyRate
import com.makemission.makeflow.domain.MonthlyFinanceReview
import com.makemission.makeflow.domain.MonthlyFinanceReviewCalculator
import com.makemission.makeflow.domain.BudgetForecast
import com.makemission.makeflow.domain.BudgetForecaster
import com.makemission.makeflow.domain.CashFlowProjector
import com.makemission.makeflow.domain.ExpenseSeasonalityDetector
import com.makemission.makeflow.domain.FrugalStreak
import com.makemission.makeflow.domain.FrugalStreakDetector
import com.makemission.makeflow.domain.MonthlySurplus
import com.makemission.makeflow.domain.SavingsCapacityDetector
import com.makemission.makeflow.domain.SavingsCapacitySuggestion
import com.makemission.makeflow.domain.SeasonalPattern
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class CategorySpendingItem(
    val categoryId: String,
    val categoryName: String,
    val color: String,
    val amount: Double,
    val percentage: Float
)

data class MonthlyBarItem(
    val label: String,
    val income: Double,
    val expense: Double
)

data class MomComparisonItem(
    val label: String,
    val currentAmount: Double,
    val priorAmount: Double,
    val percentChange: Double?
)

data class DailyAverageSpend(
    val categoryName: String,
    val color: String,
    val dailyAverage: Double
)

private data class MonthlyAnalyticsInputs(
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val month: YearMonth,
    val accounts: List<AccountEntity>
)

class InsightsViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    exchangeRateRepository: ExchangeRateRepository? = null
) : ViewModel() {

    val selectedMonth = MutableStateFlow(YearMonth.now())

    val selectedCurrency: StateFlow<String> = userPreferencesDataStore.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    val currencySymbol: StateFlow<String> = selectedCurrency.map { code ->
        CurrencyFormatter.symbolFor(code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    private val exchangeRates = exchangeRateRepository?.getAllRates() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    private val conversionRates = exchangeRates.map { rates ->
        rates.mapNotNull { rate ->
            rate.baseCurrencyCode?.let { base ->
                CurrencyRate(rate.currencyCode, base, rate.rateToDisplayCurrency, rate.rateDate)
            }
        }
    }

    private val allTransactions = financeRepository.getAllTransactions()
    private val allCategories = financeRepository.getAllCategories()

    private val _focusedAccountReady = MutableStateFlow(false)
    val focusedAccountReady: StateFlow<Boolean> = _focusedAccountReady.asStateFlow()

    private val _focusedAccountState = MutableStateFlow<FocusedAccountState>(FocusedAccountState.Loading)
    val focusedAccountState: StateFlow<FocusedAccountState> = _focusedAccountState.asStateFlow()

    val focusedAccountId: StateFlow<String?> = userPreferencesDataStore.focusedAccountId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accounts: StateFlow<List<AccountEntity>> = financeRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusedAccount: StateFlow<AccountEntity?> = combine(
        accounts, focusedAccountId
    ) { accts, focusedId ->
        if (focusedId != null) accts.find { it.id == focusedId } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val accountFilteredTransactions = combine(
        allTransactions, focusedAccountId
    ) { txs, focusedId ->
        if (focusedId != null) {
            txs.filter { tx ->
                tx.accountId == focusedId ||
                    tx.fromAccountId == focusedId ||
                    tx.toAccountId == focusedId
            }
        } else {
            txs
        }
    }

    private val monthlyAnalyticsInputs = combine(
        accountFilteredTransactions, allCategories, selectedMonth.asStateFlow(), accounts
    ) { txs, cats, month, accountList ->
        MonthlyAnalyticsInputs(txs, cats, month, accountList)
    }

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

    private fun getMonthRangeMillis(month: YearMonth): Pair<Long, Long> {
        val start = month.atDay(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val end = month.atEndOfMonth()
            .atTime(23, 59, 59, 999999999)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return start to end
    }

    private fun computeCategorySpending(
        txs: List<com.makemission.makeflow.data.local.entity.TransactionEntity>,
        cats: List<com.makemission.makeflow.data.local.entity.CategoryEntity>,
        month: YearMonth,
        accountList: List<AccountEntity>,
        displayCurrency: String,
        rates: List<CurrencyRate>
    ): List<CategorySpendingItem> {
        val (monthStart, monthEnd) = getMonthRangeMillis(month)
        val expenseTxs = txs.filter {
            it.type == "EXPENSE" && it.timestamp in monthStart..monthEnd
        }
        val accountCurrencies = accountList.associate { it.id to (it.currencyCode ?: displayCurrency) }
        val convertedExpenses = expenseTxs.mapNotNull { tx ->
            CurrencyConverter.convert(
                tx.amount,
                accountCurrencies[tx.accountId] ?: displayCurrency,
                displayCurrency,
                rates
            )?.let { tx to it }
        }
        val totalExpenses = convertedExpenses.sumOf { it.second }
        val childToParent = cats.filter { it.parentCategoryId != null }
            .associate { it.id to it.parentCategoryId!! }
        val parents = cats.filter { it.parentCategoryId == null }.associateBy { it.id }
        val grouped = convertedExpenses.groupBy { (tx, _) ->
            childToParent[tx.categoryId] ?: tx.categoryId ?: ""
        }
        val items = grouped.map { (parentId, parentTxs) ->
            val amount = parentTxs.sumOf { it.second }
            val parent = parents[parentId]
            CategorySpendingItem(
                categoryId = parentId,
                categoryName = parent?.name ?: "Uncategorized",
                color = parent?.colorHex ?: "#9E9E9E",
                amount = amount,
                percentage = if (totalExpenses > 0) (amount / totalExpenses * 100).toFloat() else 0f
            )
        }
        return items.sortedByDescending { it.amount }
    }

    val categorySpending: StateFlow<List<CategorySpendingItem>> = combine(
        monthlyAnalyticsInputs, selectedCurrency, conversionRates
    ) { inputs, displayCurrency, rates ->
        computeCategorySpending(inputs.transactions, inputs.categories, inputs.month, inputs.accounts, displayCurrency, rates)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyBarData: StateFlow<List<MonthlyBarItem>> = combine(
        accountFilteredTransactions, selectedMonth.asStateFlow(), accounts, selectedCurrency, conversionRates
    ) { txs, month, accountList, displayCurrency, rates ->
        val accountCurrencies = accountList.associate { it.id to (it.currencyCode ?: displayCurrency) }
        (0L until 6L).map { offset ->
            val m = month.minusMonths(offset)
            val (start, end) = getMonthRangeMillis(m)
            val monthTxs = txs.filter { it.timestamp in start..end }
            val income = monthTxs.filter { it.type == "INCOME" }.sumOf { tx ->
                CurrencyConverter.convert(tx.amount, accountCurrencies[tx.accountId] ?: displayCurrency, displayCurrency, rates) ?: 0.0
            }
            val expense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { tx ->
                CurrencyConverter.convert(tx.amount, accountCurrencies[tx.accountId] ?: displayCurrency, displayCurrency, rates) ?: 0.0
            }
            MonthlyBarItem(
                label = m.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                income = income,
                expense = expense
            )
        }.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = financeRepository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalExpenses: StateFlow<Double> = categorySpending.map { items ->
        items.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = combine(
        accountFilteredTransactions, selectedMonth.asStateFlow(), accounts, selectedCurrency, conversionRates
    ) { txs, month, accountList, displayCurrency, rates ->
        val (start, end) = getMonthRangeMillis(month)
        val accountCurrencies = accountList.associate { it.id to (it.currencyCode ?: displayCurrency) }
        txs.filter { it.type == "INCOME" && it.timestamp in start..end }.sumOf { tx ->
            CurrencyConverter.convert(tx.amount, accountCurrencies[tx.accountId] ?: displayCurrency, displayCurrency, rates) ?: 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val projectedMonthlyNet: StateFlow<Double> = combine(
        totalIncome, totalExpenses, selectedMonth.asStateFlow()
    ) { income, expenses, month ->
        val today = java.time.LocalDate.now()
        val elapsedDays = if (month == YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()
        CashFlowProjector.projectMonthlyNet(
            incomeSoFar = income,
            expensesSoFar = expenses,
            elapsedDays = elapsedDays,
            daysInMonth = month.lengthOfMonth()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val momItems: StateFlow<List<MomComparisonItem>> = combine(
        monthlyAnalyticsInputs, selectedCurrency, conversionRates
    ) { inputs, displayCurrency, rates ->
        val txs = inputs.transactions
        val cats = inputs.categories
        val month = inputs.month
        val accountList = inputs.accounts
        val priorMonth = month.minusMonths(1)
        val accountCurrencies = accountList.associate { it.id to (it.currencyCode ?: displayCurrency) }

        fun convertedTotal(type: String, range: LongRange): Double = txs
            .filter { it.type == type && it.timestamp in range }
            .sumOf { tx ->
                CurrencyConverter.convert(tx.amount, accountCurrencies[tx.accountId] ?: displayCurrency, displayCurrency, rates) ?: 0.0
            }

        val currentIncome = convertedTotal("INCOME", getMonthRangeMillis(month).let { it.first..it.second })
        val priorIncome = convertedTotal("INCOME", getMonthRangeMillis(priorMonth).let { it.first..it.second })
        val currentExpense = convertedTotal("EXPENSE", getMonthRangeMillis(month).let { it.first..it.second })
        val priorExpense = convertedTotal("EXPENSE", getMonthRangeMillis(priorMonth).let { it.first..it.second })

        val topCategories = computeCategorySpending(txs, cats, month, accountList, displayCurrency, rates).take(3)

        val categoryComparisons = topCategories.map { item ->
            val priorRange = getMonthRangeMillis(priorMonth)
            val childToParent = cats.filter { it.parentCategoryId != null }
                .associate { it.id to it.parentCategoryId!! }
            val priorAmount = txs.filter { tx ->
                val effectiveCat = childToParent[tx.categoryId] ?: tx.categoryId ?: ""
                tx.type == "EXPENSE" && effectiveCat == item.categoryId
                        && tx.timestamp in priorRange.first..priorRange.second
            }.sumOf { tx ->
                CurrencyConverter.convert(tx.amount, accountCurrencies[tx.accountId] ?: displayCurrency, displayCurrency, rates) ?: 0.0
            }
            MomComparisonItem(
                label = item.categoryName,
                currentAmount = item.amount,
                priorAmount = priorAmount,
                percentChange = pctChange(item.amount, priorAmount)
            )
        }

        listOfNotNull(
            MomComparisonItem("Income", currentIncome, priorIncome, pctChange(currentIncome, priorIncome)),
            MomComparisonItem("Expenses", currentExpense, priorExpense, pctChange(currentExpense, priorExpense))
        ) + categoryComparisons
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyFinanceReview: StateFlow<MonthlyFinanceReview> = combine(
        monthlyAnalyticsInputs, selectedCurrency, conversionRates
    ) { inputs, displayCurrency, rates ->
        MonthlyFinanceReviewCalculator.calculate(
            inputs.transactions, inputs.accounts, inputs.categories, inputs.month, displayCurrency, rates
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyFinanceReviewCalculator.calculate(emptyList(), emptyList(), emptyList(), YearMonth.now(), "KES", emptyList())
    )

    val budgetForecasts: StateFlow<List<BudgetForecast>> = combine(
        budgets, selectedMonth.asStateFlow()
    ) { budgetList, month ->
        val (start, end) = getMonthRangeMillis(month)
        val now = System.currentTimeMillis()
        budgetList.map { budget ->
            BudgetForecaster.forecast(budget, start, end, now)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyAverageSpends: StateFlow<List<DailyAverageSpend>> = combine(
        categorySpending, selectedMonth.asStateFlow()
    ) { items, month ->
        val daysInMonth = month.lengthOfMonth()
        items.map { item ->
            DailyAverageSpend(
                categoryName = item.categoryName,
                color = item.color,
                dailyAverage = if (daysInMonth > 0) item.amount / daysInMonth else 0.0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsSuggestion: StateFlow<SavingsCapacitySuggestion?> = combine(
        accountFilteredTransactions, accounts, selectedCurrency, conversionRates, userPreferencesDataStore.dismissedSuggestions
    ) { txs, accountList, displayCurrency, rates, dismissed ->
        if ("savings_capacity" in dismissed) null
        else {
            val accountCurrencies = accountList.associate { it.id to (it.currencyCode ?: displayCurrency) }
            val now = YearMonth.now()
            val surpluses = (0L until 6L).mapNotNull { offset ->
                val m = now.minusMonths(offset)
                val (start, end) = getMonthRangeMillis(m)
                val income = txs.filter { it.type == "INCOME" && it.timestamp in start..end }.sumOf { tx ->
                    CurrencyConverter.convert(tx.amount, accountCurrencies[tx.accountId] ?: displayCurrency, displayCurrency, rates) ?: 0.0
                }
                val expense = txs.filter { it.type == "EXPENSE" && it.timestamp in start..end }.sumOf { tx ->
                    CurrencyConverter.convert(tx.amount, accountCurrencies[tx.accountId] ?: displayCurrency, displayCurrency, rates) ?: 0.0
                }
                if (income == 0.0 && expense == 0.0) null else MonthlySurplus(income, expense)
            }
            SavingsCapacityDetector.compute(surpluses)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val frugalStreaks: StateFlow<List<FrugalStreak>> = combine(
        accountFilteredTransactions, allCategories, userPreferencesDataStore.dismissedSuggestions
    ) { txs, cats, dismissed ->
        val expenseCategories = cats.filter { it.parentCategoryId == null }
        val childToParent = cats.filter { it.parentCategoryId != null }
            .associate { it.id to it.parentCategoryId!! }

        expenseCategories.mapNotNull { category ->
            val dismissKey = "frugal_streak_${category.id}"
            if (dismissKey in dismissed) return@mapNotNull null

            val categoryTxns = txs.filter { tx ->
                val effectiveCat = childToParent[tx.categoryId] ?: tx.categoryId ?: ""
                effectiveCat == category.id
            }

            FrugalStreakDetector.detect(
                categoryId = category.id,
                categoryName = category.name,
                categoryTransactions = categoryTxns
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingSeasonalExpenses: StateFlow<List<SeasonalPattern>> = combine(
        accountFilteredTransactions, allCategories
    ) { txs, cats ->
        val expenseCategories = cats.filter { it.parentCategoryId == null }
        val childToParent = cats.filter { it.parentCategoryId != null }
            .associate { it.id to it.parentCategoryId!! }

        val now = YearMonth.now()
        val upcomingMonths = (0..2).map { now.plusMonths(it.toLong()).monthValue }.toSet()

        expenseCategories.flatMap { category ->
            val categoryTxns = txs.filter { tx ->
                val effectiveCat = childToParent[tx.categoryId] ?: tx.categoryId ?: ""
                effectiveCat == category.id
            }

            ExpenseSeasonalityDetector.detect(
                categoryId = category.id,
                categoryName = category.name,
                categoryTransactions = categoryTxns
            ).filter { pattern ->
                pattern.peakMonths.any { it in upcomingMonths }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissSavingsSuggestion() {
        viewModelScope.launch {
            userPreferencesDataStore.dismissSuggestion("savings_capacity")
        }
    }

    fun dismissFrugalStreak(categoryId: String) {
        viewModelScope.launch {
            userPreferencesDataStore.dismissSuggestion("frugal_streak_$categoryId")
        }
    }

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        val next = selectedMonth.value.plusMonths(1)
        if (!next.isAfter(YearMonth.now())) {
            selectedMonth.value = next
        }
    }

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
}

class InsightsViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val exchangeRateRepository: ExchangeRateRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InsightsViewModel(financeRepository, userPreferencesDataStore, exchangeRateRepository) as T
    }
}

internal fun pctChange(current: Double, prior: Double): Double? =
    if (prior != 0.0) ((current - prior) / prior * 100.0) else null
