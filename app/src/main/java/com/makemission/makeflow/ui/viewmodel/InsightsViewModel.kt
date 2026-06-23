package com.makemission.makeflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    val categorySpending: StateFlow<List<CategorySpendingItem>> = combine(
        allTransactions, allCategories, selectedMonth.asStateFlow()
    ) { txs, cats, month ->
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
        items.sortedByDescending { it.amount }
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
        when (code) {
            "USD" -> "$"
            "EUR" -> "\u20AC"
            "GBP" -> "\u00A3"
            "UGX" -> "UGX"
            "TZS" -> "TZS"
            else -> "KES"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

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
