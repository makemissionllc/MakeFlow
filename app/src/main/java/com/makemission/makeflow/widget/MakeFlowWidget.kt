package com.makemission.makeflow.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.makemission.makeflow.MainActivity
import com.makemission.makeflow.R
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.data.RecurringPattern
import com.makemission.makeflow.data.local.userPrefDataStore
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.RecurringPatternEntity
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.domain.CashFlowProjection
import com.makemission.makeflow.domain.CashFlowProjector
import com.makemission.makeflow.domain.CurrencyConverter
import com.makemission.makeflow.domain.CurrencyRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MakeFlowWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(180.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 220.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) {
            loadWidgetData(context)
        }
        provideContent {
            WidgetContent(data, LocalSize.current)
        }
    }
}

data class WidgetData(
    val content: WidgetContent,
    val currencyCode: String,
    val currentBalance: Double,
    val monthIncome: Double,
    val monthExpenses: Double
)

internal suspend fun loadWidgetData(context: Context): WidgetData {
    val db = AppDatabase.getDatabase(context)
    val accounts = db.accountDao().getAllAccounts().first()
    val allTransactions = db.transactionDao().getAllTransactions().first()
    val budgets = db.budgetDao().getAllBudgetsFlow().first()
    val recurringEntities = db.recurringPatternDao().getAll().first()

    val prefsDataStore = com.makemission.makeflow.data.local.UserPreferencesDataStore(
        context.applicationContext.userPrefDataStore
    )
    val currencyCode = prefsDataStore.selectedCurrency.first()
    val rates = db.exchangeRateDao().getAllRatesOnce().toCurrencyRates()
    val totalBalance = accounts.sumOf { account ->
        val accountCurrency = account.currencyCode ?: currencyCode
        CurrencyConverter.convert(account.balance, accountCurrency, currencyCode, rates) ?: 0.0
    }
    val accountCurrencies = accounts.associate { it.id to (it.currencyCode ?: currencyCode) }

    val recurringPatterns = recurringEntities.map { it.toDomain() }
    val hasAccounts = accounts.isNotEmpty()
    val hasTransactions = allTransactions.isNotEmpty()

    val today = LocalDate.now()
    val thisMonthTxs = allTransactions.filter {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
        cal.get(java.util.Calendar.YEAR) == today.year &&
            cal.get(java.util.Calendar.MONTH) + 1 == today.monthValue
    }
    val displayCurrencyTransactions = thisMonthTxs.mapNotNull { transaction ->
        val transactionCurrency = accountCurrencies[transaction.accountId] ?: currencyCode
        CurrencyConverter.convert(transaction.amount, transactionCurrency, currencyCode, rates)
            ?.let { convertedAmount -> transaction.copy(amount = convertedAmount) }
    }
    val monthIncome = displayCurrencyTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val monthExpenses = displayCurrencyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val projection = if (today.dayOfMonth >= 3) {
        CashFlowProjector.project(totalBalance, displayCurrencyTransactions, recurringPatterns, today)
    } else null

    val content = WidgetContentResolver.resolve(
        upcomingBills = recurringPatterns,
        budgets = budgets,
        currentBalance = totalBalance,
        projection = projection,
        hasAnyAccounts = hasAccounts,
        hasAnyTransactions = hasTransactions
    )

    return WidgetData(
        content = content,
        currencyCode = currencyCode,
        currentBalance = totalBalance,
        monthIncome = monthIncome,
        monthExpenses = monthExpenses
    )
}

private fun List<ExchangeRateEntity>.toCurrencyRates(): List<CurrencyRate> = mapNotNull { rate ->
    rate.baseCurrencyCode?.let { base ->
        CurrencyRate(rate.currencyCode, base, rate.rateToDisplayCurrency, rate.rateDate)
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(data: WidgetData, size: DpSize) {
    val compact = size.width < WidgetTokens.compactWidth
    val expanded = size.width >= WidgetTokens.expandedWidth && size.height >= WidgetTokens.expandedHeight

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetSurface()
            .padding(widgetPadding(compact))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        WidgetHeader("MINDFUL", trailing = data.currencyCode)
        Spacer(GlanceModifier.height(WidgetTokens.compactSpacing))
        Text(
            text = "Balance",
            style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.bodySize, fontWeight = FontWeight.Medium)
        )
        Text(
            text = CurrencyFormatter.format(data.currentBalance, data.currencyCode),
            style = TextStyle(
                color = WidgetPalette.text,
                fontSize = if (compact) WidgetTokens.compactAmountSize else WidgetTokens.amountSize,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )

        if (expanded) {
            Spacer(GlanceModifier.height(WidgetTokens.sectionSpacing))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WidgetMonthAmount("IN THIS MONTH", data.monthIncome, data.currencyCode, WidgetPalette.accent, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(WidgetTokens.sectionSpacing))
                WidgetMonthAmount("OUT THIS MONTH", data.monthExpenses, data.currencyCode, WidgetPalette.expense, GlanceModifier.defaultWeight())
            }
            Spacer(GlanceModifier.height(WidgetTokens.sectionSpacing))
        } else {
            Spacer(GlanceModifier.height(WidgetTokens.compactSpacing))
        }

        when (val content = data.content) {
            is WidgetContent.UpcomingBill -> {
                val dueLabel = when (content.daysUntilDue) {
                    0 -> "Due today"
                    1 -> "Due tomorrow"
                    else -> "Due in ${content.daysUntilDue} days"
                }
                WidgetSpotlight(label = dueLabel, value = content.description, amount = content.amount, currencyCode = data.currencyCode, compact = compact)
            }
            is WidgetContent.BudgetWarning -> {
                WidgetSpotlight(
                    label = if (content.isExceeded) "Budget passed" else "Budget alert",
                    value = content.categoryName,
                    amountLabel = "${CurrencyFormatter.format(content.spentAmount, data.currencyCode)} / ${CurrencyFormatter.format(content.budgetAmount, data.currencyCode)}",
                    compact = compact
                )
            }
            is WidgetContent.BalanceSummary -> content.projectedEndOfMonth?.let { projected ->
                Text(
                    text = "Projected · ${CurrencyFormatter.format(projected, data.currencyCode)}",
                    style = TextStyle(color = WidgetPalette.accent, fontSize = WidgetTokens.bodySize, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
            }
            is WidgetContent.NoData -> {
                Text(
                    text = "Start with an account or your first transaction",
                    style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.bodySize),
                    maxLines = if (compact) 2 else 1
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetMonthAmount(label: String, amount: Double, currencyCode: String, color: ColorProvider, modifier: GlanceModifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.smallLabelSize, fontWeight = FontWeight.Medium),
            maxLines = 1
        )
        Text(
            text = CurrencyFormatter.format(amount, currencyCode),
            style = TextStyle(color = color, fontSize = WidgetTokens.titleSize, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

@androidx.compose.runtime.Composable
private fun WidgetHeader(title: String, trailing: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.labelSize, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        Text(
            text = trailing,
            style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.labelSize, fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}

@androidx.compose.runtime.Composable
private fun WidgetSpotlight(
    label: String,
    value: String,
    currencyCode: String? = null,
    amount: Double? = null,
    amountLabel: String? = null,
    compact: Boolean
) {
    Column {
        Text(
            text = label.uppercase(),
            style = TextStyle(color = WidgetPalette.accent, fontSize = WidgetTokens.smallLabelSize, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = WidgetPalette.text, fontSize = if (compact) WidgetTokens.bodySize else WidgetTokens.titleSize, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            val formattedAmount = when {
                amountLabel != null -> amountLabel
                amount != null && currencyCode != null -> CurrencyFormatter.format(amount, currencyCode)
                else -> null
            }
            formattedAmount?.let {
                Text(
                    text = it,
                    style = TextStyle(color = WidgetPalette.text, fontSize = WidgetTokens.bodySize, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }
    }
}

private fun RecurringPatternEntity.toDomain() = RecurringPattern(
    normalizedDescription = normalizedDescription,
    avgAmount = avgAmount,
    intervalDays = intervalDays,
    lastSeenDate = lastSeenDate,
    nextExpectedDate = nextExpectedDate,
    confidence = confidence,
    categoryId = categoryId,
    transactionType = transactionType
)
