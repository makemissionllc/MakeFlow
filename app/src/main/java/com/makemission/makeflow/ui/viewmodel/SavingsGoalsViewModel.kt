package com.makemission.makeflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.SavingsGoalEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepository
import com.makemission.makeflow.data.repository.GoalRepository
import com.makemission.makeflow.domain.CategorySpendSummary
import com.makemission.makeflow.domain.GoalFeasibilityCalculator
import com.makemission.makeflow.domain.GoalFeasibilityResult
import com.makemission.makeflow.domain.MonthlySurplus
import com.makemission.makeflow.domain.SavingsGoalWithProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class GoalWithFeasibility(
    val goalWithProgress: SavingsGoalWithProgress,
    val feasibility: GoalFeasibilityResult?
)

class SavingsGoalsViewModel(
    private val goalRepository: GoalRepository,
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val goalsWithProgress: StateFlow<List<SavingsGoalWithProgress>> = combine(
        goalRepository.getAllGoals(),
        financeRepository.getAllAccounts()
    ) { goals, accounts ->
        val accountMap = accounts.associateBy { it.id }
        goals.map { goal ->
            val balance = goal.linkedAccountId?.let { accountMap[it]?.balance } ?: 0.0
            SavingsGoalWithProgress(
                goal = goal,
                progress = balance,
                percentage = if (goal.targetAmount > 0) {
                    (balance / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                } else 0f
            )
        }.sortedByDescending { it.goal.createdDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goalsWithFeasibility: StateFlow<List<GoalWithFeasibility>> = combine(
        goalsWithProgress,
        financeRepository.getAllTransactions(),
        financeRepository.getAllCategories()
    ) { goals, txs, cats ->
        val monthlySurpluses = computeMonthlySurpluses(txs)
        val categorySpending = computeAverageCategorySpending(txs, cats)
        goals.map { gwp ->
            val feasibility = GoalFeasibilityCalculator.assess(
                goal = gwp.goal,
                currentProgress = gwp.progress,
                monthlySurpluses = monthlySurpluses,
                categorySpending = categorySpending,
                nowMillis = System.currentTimeMillis()
            )
            GoalWithFeasibility(gwp, feasibility)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            combine(goalRepository.getActiveGoals(), financeRepository.getAllAccounts()) { goals, accounts ->
                goals to accounts
            }.collect { (goals, accounts) ->
                val accountMap = accounts.associateBy { it.id }
                for (goal in goals) {
                    val balance = goal.linkedAccountId?.let { accountMap[it]?.balance } ?: 0.0
                    if (balance >= goal.targetAmount) {
                        goalRepository.updateStatus(goal.id, "ACHIEVED")
                    }
                }
            }
        }
    }

    fun addGoal(name: String, targetAmount: Double, targetDate: Long?, linkedAccountId: String) {
        viewModelScope.launch {
            goalRepository.insert(
                SavingsGoalEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    targetAmount = targetAmount,
                    targetDate = targetDate,
                    linkedAccountId = linkedAccountId,
                    status = "ACTIVE"
                )
            )
        }
    }

    fun updateGoal(goal: SavingsGoalEntity, name: String, targetAmount: Double, targetDate: Long?, linkedAccountId: String?) {
        viewModelScope.launch {
            goalRepository.update(
                goal.copy(
                    name = name,
                    targetAmount = targetAmount,
                    targetDate = targetDate,
                    linkedAccountId = linkedAccountId
                )
            )
        }
    }

    fun deleteGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            goalRepository.delete(goal)
        }
    }

    fun abandonGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            goalRepository.updateStatus(goal.id, "ABANDONED")
        }
    }

    suspend fun getActiveGoalCountForAccount(accountId: String): Int {
        return goalRepository.getActiveGoalCountForAccount(accountId)
    }

    private fun computeMonthlySurpluses(txs: List<TransactionEntity>): List<MonthlySurplus> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        val results = mutableListOf<MonthlySurplus>()
        for (i in 0 until 6) {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val monthStart = Calendar.getInstance().apply {
                set(year, month, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            calendar.add(Calendar.MONTH, -1)
            val monthEnd = calendar.timeInMillis
            val monthTxs = txs.filter { it.timestamp in monthEnd until monthStart }
            val income = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            results.add(MonthlySurplus(income, expense))
        }
        return results
    }

    private fun computeAverageCategorySpending(
        txs: List<TransactionEntity>,
        cats: List<CategoryEntity>
    ): List<CategorySpendSummary> {
        val now = System.currentTimeMillis()
        val childToParent = cats.filter { it.parentCategoryId != null }
            .associate { it.id to it.parentCategoryId!! }
        val parents = cats.filter { it.parentCategoryId == null }.associateBy { it.id }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now

        val monthBuckets = mutableListOf<List<TransactionEntity>>()
        for (i in 0 until 3) {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val monthStart = Calendar.getInstance().apply {
                set(year, month, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            calendar.add(Calendar.MONTH, -1)
            val monthEnd = calendar.timeInMillis
            monthBuckets.add(txs.filter {
                it.type == "EXPENSE" && it.timestamp in monthEnd until monthStart
            })
        }

        if (monthBuckets.all { it.isEmpty() }) return emptyList()

        val catTotals = mutableMapOf<String, Double>()
        var monthCount = 0
        for (bucket in monthBuckets) {
            if (bucket.isEmpty()) continue
            monthCount++
            val grouped = bucket.groupBy { tx ->
                childToParent[tx.categoryId] ?: tx.categoryId ?: ""
            }
            grouped.forEach { (parentId, parentTxs) ->
                catTotals[parentId] = (catTotals[parentId] ?: 0.0) + parentTxs.sumOf { it.amount }
            }
        }

        if (monthCount == 0) return emptyList()

        return catTotals.map { (parentId, total) ->
            val parent = parents[parentId]
            CategorySpendSummary(
                categoryName = parent?.name ?: "Uncategorized",
                monthlySpend = total / monthCount
            )
        }
    }
}

class SavingsGoalsViewModelFactory(
    private val goalRepository: GoalRepository,
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SavingsGoalsViewModel(goalRepository, financeRepository, userPreferencesDataStore) as T
    }
}
