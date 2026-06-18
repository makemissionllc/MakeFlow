package com.makemission.makeflow.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.ui.state.AnalyticsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    private val _selectedFilter = MutableStateFlow(0)
    val selectedFilter: StateFlow<Int> = _selectedFilter.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepositoryImpl(db.categoryDao(), db.transactionDao(), db.taskDao())
        observeAnalytics()
    }

    fun onFilterChange(index: Int) {
        _selectedFilter.value = index
    }

    private fun observeAnalytics() {
        viewModelScope.launch {
            combine(
                repository.getAllTransactions(),
                _selectedFilter
            ) { transactions, filter ->
                val (start, end) = filterTimeRange(filter)
                val filtered = transactions.filter { it.timestamp in start..end }
                val chartData = buildChartPoints(filtered, filter)
                val totalInc = chartData.sumOf { it.second.first }
                val totalExp = chartData.sumOf { it.second.second }
                AnalyticsUiState(
                    isLoading = false,
                    totalIncome = totalInc,
                    totalExpense = totalExp,
                    netSavings = totalInc - totalExp,
                    incomeChartPoints = chartData.map { it.first to it.second.first },
                    expenseChartPoints = chartData.map { it.first to it.second.second },
                    selectedFilter = filter
                )
            }
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    _uiState.value = AnalyticsUiState(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load analytics"
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    private data class ChartBucket(val label: String, val income: Double, val expense: Double)

    private fun buildChartPoints(
        transactions: List<TransactionEntity>,
        filter: Int
    ): List<Pair<String, Pair<Double, Double>>> {
        val buckets = when (filter) {
            0 -> groupByDayOfWeek(transactions)
            1 -> groupByDayOfMonth(transactions)
            else -> groupByMonth(transactions)
        }
        return buckets.map { it.label to (it.income to it.expense) }
    }

    private fun groupByDayOfWeek(transactions: List<TransactionEntity>): List<ChartBucket> {
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val map = linkedMapOf<String, MutableList<TransactionEntity>>()
        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val label = dayFormat.format(cal.time)
            map[label] = mutableListOf()
        }
        val cal2 = Calendar.getInstance()
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000L
        transactions
            .filter { it.timestamp in sevenDaysAgo..now }
            .forEach { tx ->
                cal2.timeInMillis = tx.timestamp
                val label = dayFormat.format(cal2.time)
                map[label]?.add(tx)
            }
        return map.map { (label, list) ->
            ChartBucket(label, sumIncome(list), sumExpense(list))
        }
    }

    private fun groupByDayOfMonth(transactions: List<TransactionEntity>): List<ChartBucket> {
        val dayFormat = SimpleDateFormat("d", Locale.US)
        val cal = Calendar.getInstance()
        val (monthStart, monthEnd) = currentMonthRange()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val map = linkedMapOf<String, MutableList<TransactionEntity>>()
        for (d in 1..daysInMonth) {
            map["$d"] = mutableListOf()
        }
        val cal2 = Calendar.getInstance()
        transactions
            .filter { it.timestamp in monthStart..monthEnd }
            .forEach { tx ->
                cal2.timeInMillis = tx.timestamp
                val label = dayFormat.format(cal2.time)
                map[label]?.add(tx)
            }
        return map.map { (label, list) ->
            ChartBucket(label, sumIncome(list), sumExpense(list))
        }
    }

    private fun groupByMonth(transactions: List<TransactionEntity>): List<ChartBucket> {
        val monthFormat = SimpleDateFormat("MMM", Locale.US)
        val yearFormat = SimpleDateFormat("yyyy", Locale.US)
        val cal = Calendar.getInstance()
        val currentYear = yearFormat.format(cal.time)
        val map = linkedMapOf<String, MutableList<TransactionEntity>>()
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        for (m in 0 until 12) {
            cal.set(Calendar.MONTH, m)
            map[monthFormat.format(cal.time)] = mutableListOf()
        }
        val cal2 = Calendar.getInstance()
        transactions
            .filter { yearFormat.format(Date(it.timestamp)) == currentYear }
            .forEach { tx ->
                cal2.timeInMillis = tx.timestamp
                val label = monthFormat.format(cal2.time)
                map[label]?.add(tx)
            }
        return map.map { (label, list) ->
            ChartBucket(label, sumIncome(list), sumExpense(list))
        }
    }

    private fun sumIncome(list: List<TransactionEntity>): Double =
        list.filter { it.type == "INCOME" }.sumOf { it.amount }

    private fun sumExpense(list: List<TransactionEntity>): Double =
        list.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    private fun filterTimeRange(filter: Int): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return when (filter) {
            0 -> Pair(now - 7 * 24 * 60 * 60 * 1000L, now)
            1 -> currentMonthRange()
            else -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.YEAR, -1)
                Pair(cal.timeInMillis, now)
            }
        }
    }

    private fun currentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return Pair(start, cal.timeInMillis)
    }
}
