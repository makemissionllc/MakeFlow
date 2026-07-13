package com.makemission.makeflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import com.makemission.makeflow.domain.BudgetForecast
import com.makemission.makeflow.domain.BudgetForecaster
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
    val percentChange: Double
)

data class DailyAverageSpend(
    val categoryName: String,
    val color: String,
    val dailyAverage: Double
)

class InsightsViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val selectedMonth = MutableStateFlow(YearMonth.now())

    private val allTransactions = financeRepository.getAllTransactions()
    private val allCategories = financeRepository.getAllCategories()

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
        month: YearMonth
    ): List<CategorySpendingItem> {
        val (monthStart, monthEnd) = getMonthRangeMillis(month)
        val expenseTxs = txs.filter {
            it.type == "EXPENSE" && it.timestamp in monthStart..monthEnd
        }
        val totalExpenses = expenseTxs.sumOf { it.amount }
        val childToParent = cats.filter { it.parentCategoryId != null }
            .associate { it.id to it.parentCategoryId!! }
        val parents = cats.filter { it.parentCategoryId == null }.associateBy { it.id }
        val grouped = expenseTxs.groupBy { tx ->
            childToParent[tx.categoryId] ?: tx.categoryId ?: ""
        }
        val items = grouped.map { (parentId, parentTxs) ->
            val amount = parentTxs.sumOf { it.amount }
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
        allTransactions, allCategories, selectedMonth.asStateFlow()
    ) { txs, cats, month ->
        computeCategorySpending(txs, cats, month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyBarData: StateFlow<List<MonthlyBarItem>> = combine(
        allTransactions, selectedMonth.asStateFlow()
    ) { txs, month ->
        (0L until 6L).map { offset ->
            val m = month.minusMonths(offset)
            val (start, end) = getMonthRangeMillis(m)
            val monthTxs = txs.filter { it.timestamp in start..end }
            val income = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
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
        allTransactions, selectedMonth.asStateFlow()
    ) { txs, month ->
        val (start, end) = getMonthRangeMillis(month)
        txs.filter { it.type == "INCOME" && it.timestamp in start..end }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val selectedCurrency: StateFlow<String> = userPreferencesDataStore.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    val currencySymbol: StateFlow<String> = selectedCurrency.map { code ->
        CurrencyFormatter.symbolFor(code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    val momItems: StateFlow<List<MomComparisonItem>> = combine(
        allTransactions, allCategories, selectedMonth.asStateFlow()
    ) { txs, cats, month ->
        val priorMonth = month.minusMonths(1)

        val currentIncome = txs.filter {
            it.type == "INCOME" && it.timestamp in getMonthRangeMillis(month).let { it.first..it.second }
        }.sumOf { it.amount }
        val priorIncome = txs.filter {
            it.type == "INCOME" && it.timestamp in getMonthRangeMillis(priorMonth).let { it.first..it.second }
        }.sumOf { it.amount }

        val currentExpense = txs.filter {
            it.type == "EXPENSE" && it.timestamp in getMonthRangeMillis(month).let { it.first..it.second }
        }.sumOf { it.amount }
        val priorExpense = txs.filter {
            it.type == "EXPENSE" && it.timestamp in getMonthRangeMillis(priorMonth).let { it.first..it.second }
        }.sumOf { it.amount }

        fun pctChange(current: Double, prior: Double): Double =
            if (prior != 0.0) ((current - prior) / prior * 100.0) else if (current != 0.0) 100.0 else 0.0

        val topCategories = computeCategorySpending(txs, cats, month).take(3)

        val categoryComparisons = topCategories.map { item ->
            val priorRange = getMonthRangeMillis(priorMonth)
            val childToParent = cats.filter { it.parentCategoryId != null }
                .associate { it.id to it.parentCategoryId!! }
            val priorAmount = txs.filter { tx ->
                val effectiveCat = childToParent[tx.categoryId] ?: tx.categoryId ?: ""
                tx.type == "EXPENSE" && effectiveCat == item.categoryId
                        && tx.timestamp in priorRange.first..priorRange.second
            }.sumOf { it.amount }
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
        allTransactions, userPreferencesDataStore.dismissedSuggestions
    ) { txs, dismissed ->
        if ("savings_capacity" in dismissed) null
        else {
            val now = YearMonth.now()
            val surpluses = (0L until 6L).mapNotNull { offset ->
                val m = now.minusMonths(offset)
                val (start, end) = getMonthRangeMillis(m)
                val income = txs.filter { it.type == "INCOME" && it.timestamp in start..end }.sumOf { it.amount }
                val expense = txs.filter { it.type == "EXPENSE" && it.timestamp in start..end }.sumOf { it.amount }
                if (income == 0.0 && expense == 0.0) null else MonthlySurplus(income, expense)
            }
            SavingsCapacityDetector.compute(surpluses)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val frugalStreaks: StateFlow<List<FrugalStreak>> = combine(
        allTransactions, allCategories, userPreferencesDataStore.dismissedSuggestions
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
        allTransactions, allCategories
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
}

class InsightsViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InsightsViewModel(financeRepository, userPreferencesDataStore) as T
    }
}
