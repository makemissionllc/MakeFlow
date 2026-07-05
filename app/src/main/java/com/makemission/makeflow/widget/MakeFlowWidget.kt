package com.makemission.makeflow.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.makemission.makeflow.MainActivity
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

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) {
            loadWidgetData(context)
        }
        provideContent {
            WidgetContent(data)
        }
    }
}

data class WidgetData(
    val content: WidgetContent,
    val currencyCode: String
)

private suspend fun loadWidgetData(context: Context): WidgetData {
    val db = AppDatabase.getDatabase(context)
    val accounts = db.accountDao().getAllAccounts().first()
    val allTransactions = db.transactionDao().getAllTransactions().first()
    val budgets = db.budgetDao().getAllBudgetsFlow().first()
    val recurringEntities = db.recurringPatternDao().getAll().first()

    val recurringPatterns = recurringEntities.map { it.toDomain() }
    val totalBalance = accounts.sumOf { it.balance }
    val hasAccounts = accounts.isNotEmpty()
    val hasTransactions = allTransactions.isNotEmpty()

    val today = LocalDate.now()
    val thisMonthTxs = allTransactions.filter {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
        cal.get(java.util.Calendar.YEAR) == today.year &&
            cal.get(java.util.Calendar.MONTH) + 1 == today.monthValue
    }
    val projection = if (today.dayOfMonth >= 3) {
        CashFlowProjector.project(totalBalance, thisMonthTxs, recurringPatterns, today)
    } else null

    val content = WidgetContentResolver.resolve(
        upcomingBills = recurringPatterns,
        budgets = budgets,
        currentBalance = totalBalance,
        projection = projection,
        hasAnyAccounts = hasAccounts,
        hasAnyTransactions = hasTransactions
    )

    val prefsDataStore = com.makemission.makeflow.data.local.UserPreferencesDataStore(
        context.applicationContext.userPrefDataStore
    )
    val currencyCode = prefsDataStore.selectedCurrency.first()

    return WidgetData(content = content, currencyCode = currencyCode)
}

@androidx.compose.runtime.Composable
private fun WidgetContent(data: WidgetData) {
    val bgColor = ColorProvider(com.makemission.makeflow.R.color.widget_bg)
    val textColor = ColorProvider(com.makemission.makeflow.R.color.widget_text)
    val mutedColor = ColorProvider(com.makemission.makeflow.R.color.widget_muted)
    val accentColor = ColorProvider(com.makemission.makeflow.R.color.widget_accent)
    val warningColor = ColorProvider(com.makemission.makeflow.R.color.widget_warning)
    val amberColor = ColorProvider(com.makemission.makeflow.R.color.widget_amber)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MakeFlow",
                style = TextStyle(
                    color = mutedColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        when (val c = data.content) {
            is WidgetContent.UpcomingBill -> {
                val dayLabel = when (c.daysUntilDue) {
                    0 -> "Due today"
                    1 -> "Due tomorrow"
                    else -> "Due in ${c.daysUntilDue} days"
                }
                Text(
                    text = dayLabel,
                    style = TextStyle(color = warningColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = c.description,
                    style = TextStyle(color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = CurrencyFormatter.format(c.amount, data.currencyCode),
                    style = TextStyle(color = warningColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
            }
            is WidgetContent.BudgetWarning -> {
                Text(
                    text = "Budget Alert",
                    style = TextStyle(color = amberColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = c.categoryName,
                    style = TextStyle(color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = "${CurrencyFormatter.format(c.spentAmount, data.currencyCode)} / ${CurrencyFormatter.format(c.budgetAmount, data.currencyCode)}",
                    style = TextStyle(color = if (c.isExceeded) warningColor else amberColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
            }
            is WidgetContent.BalanceSummary -> {
                Text(
                    text = "Balance",
                    style = TextStyle(color = mutedColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = CurrencyFormatter.format(c.currentBalance, data.currencyCode),
                    style = TextStyle(color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
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
                    text = "Welcome to MakeFlow",
                    style = TextStyle(color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Add your first transaction to get started",
                    style = TextStyle(color = mutedColor, fontSize = 13.sp),
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
