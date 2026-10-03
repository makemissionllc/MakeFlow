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
import com.makemission.makeflow.domain.CashFlowProjection
import com.makemission.makeflow.domain.CashFlowProjector
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

private suspend fun loadWidgetData(context: Context): WidgetData {
    val db = AppDatabase.getDatabase(context)
    val accounts = db.accountDao().getAllAccounts().first()
    val allTransactions = db.transactionDao().getAllTransactions().first()
    val budgets = db.budgetDao().getAllBudgetsFlow().first()
    val recurringEntities = db.recurringPatternDao().getAll().first()

    val prefsDataStore = com.makemission.makeflow.data.local.UserPreferencesDataStore(
        context.applicationContext.userPrefDataStore
    )
    val currencyCode = prefsDataStore.selectedCurrency.first()
    val rates = db.exchangeRateDao().getAllRatesOnce()
        .associate { it.currencyCode to it.rateToDisplayCurrency }
    val totalBalance = accounts.sumOf { account ->
        val accountCurrency = account.currencyCode ?: currencyCode
        if (accountCurrency == currencyCode) account.balance
        else rates[accountCurrency]?.let { account.balance * it } ?: account.balance
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
    val displayCurrencyTransactions = thisMonthTxs.map { transaction ->
        val transactionCurrency = accountCurrencies[transaction.accountId] ?: currencyCode
        val convertedAmount = if (transactionCurrency == currencyCode) {
            transaction.amount
        } else {
            transaction.amount * (rates[transactionCurrency] ?: 1.0)
        }
        transaction.copy(amount = convertedAmount)
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

@androidx.compose.runtime.Composable
private fun WidgetContent(data: WidgetData, size: DpSize) {
    val bgColor = ColorProvider(R.color.widget_bg)
    val textColor = ColorProvider(R.color.widget_text)
    val mutedColor = ColorProvider(R.color.widget_muted)
    val accentColor = ColorProvider(R.color.widget_accent)
    val warningColor = ColorProvider(R.color.widget_warning)
    val amberColor = ColorProvider(R.color.widget_amber)
    val compact = size.width < 180.dp
    val expanded = size.width >= 220.dp && size.height >= 180.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .padding(if (compact) 10.dp else 14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (compact) "Mindful" else "Mindful Wallet",
                style = TextStyle(
                    color = mutedColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        if (expanded) {
            Spacer(modifier = GlanceModifier.height(10.dp))
            Text(
                text = "TOTAL BALANCE",
                style = TextStyle(color = mutedColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            )
            Text(
                text = CurrencyFormatter.format(data.currentBalance, data.currencyCode),
                style = TextStyle(color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("IN THIS MONTH", style = TextStyle(color = mutedColor, fontSize = 9.sp))
                    Text(
                        "+${CurrencyFormatter.format(data.monthIncome, data.currencyCode)}",
                        style = TextStyle(color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
                Spacer(modifier = GlanceModifier.width(10.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("OUT THIS MONTH", style = TextStyle(color = mutedColor, fontSize = 9.sp))
                    Text(
                        "-${CurrencyFormatter.format(data.monthExpenses, data.currencyCode)}",
                        style = TextStyle(color = warningColor, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = GlanceModifier.height(10.dp))
        } else {
            Spacer(modifier = GlanceModifier.height(if (compact) 4.dp else 8.dp))
        }

        when (val c = data.content) {
            is WidgetContent.UpcomingBill -> {
                val dayLabel = when (c.daysUntilDue) {
                    0 -> "Due today"
                    1 -> "Due tomorrow"
                    else -> "Due in ${c.daysUntilDue} days"
                }
                Text(
                    text = dayLabel,
                    style = TextStyle(color = warningColor, fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = c.description,
                    style = TextStyle(color = textColor, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = CurrencyFormatter.format(c.amount, data.currencyCode),
                    style = TextStyle(color = warningColor, fontSize = if (compact) 17.sp else 20.sp, fontWeight = FontWeight.Bold)
                )
            }
            is WidgetContent.BudgetWarning -> {
                Text(
                    text = "Budget Alert",
                    style = TextStyle(color = amberColor, fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = c.categoryName,
                    style = TextStyle(color = textColor, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = "${CurrencyFormatter.format(c.spentAmount, data.currencyCode)} / ${CurrencyFormatter.format(c.budgetAmount, data.currencyCode)}",
                    style = TextStyle(color = if (c.isExceeded) warningColor else amberColor, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold)
                )
            }
            is WidgetContent.BalanceSummary -> {
                if (!expanded) {
                    Text(
                        text = "Balance",
                        style = TextStyle(color = mutedColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = CurrencyFormatter.format(c.currentBalance, data.currencyCode),
                        style = TextStyle(color = textColor, fontSize = if (compact) 18.sp else 22.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
                c.projectedEndOfMonth?.let { projected ->
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "Projected EOM: ${CurrencyFormatter.format(projected, data.currencyCode)}",
                        style = TextStyle(color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }
            is WidgetContent.NoData -> {
                Text(
                    text = if (compact) "Welcome" else "Welcome to Mindful Wallet",
                    style = TextStyle(color = textColor, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = if (compact) "Tap to get started" else "Open to add your first transaction",
                    style = TextStyle(color = mutedColor, fontSize = if (compact) 11.sp else 13.sp),
                    maxLines = 2
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
