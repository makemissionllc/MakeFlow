package com.makemission.makeflow.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.makemission.makeflow.MainActivity
import com.makemission.makeflow.R
import com.makemission.makeflow.core.format.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val compactWidgetSizes = setOf(
    DpSize(110.dp, 60.dp),
    DpSize(220.dp, 110.dp),
    DpSize(250.dp, 220.dp)
)

class BalanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(compactWidgetSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) { loadQuickActionData(context) }
        provideContent { BalanceWidgetContent(data, LocalSize.current) }
    }
}

class MonthlySummaryWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(220.dp, 110.dp), DpSize(250.dp, 220.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) { loadWidgetData(context) }
        provideContent { MonthlySummaryWidgetContent(data, LocalSize.current) }
    }
}

class ExpenseEntryWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(220.dp, 110.dp), DpSize(250.dp, 220.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            QuickEntryWidgetContent(
                label = "Add expense",
                symbol = "-",
                action = actionRunCallback<ExpenseShortcutCallback>(),
                size = LocalSize.current
            )
        }
    }
}

class IncomeEntryWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(220.dp, 110.dp), DpSize(250.dp, 220.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            QuickEntryWidgetContent(
                label = "Add income",
                symbol = "+",
                action = actionRunCallback<IncomeShortcutCallback>(),
                size = LocalSize.current
            )
        }
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}

class MonthlySummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthlySummaryWidget()
}

class ExpenseEntryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseEntryWidget()
}

class IncomeEntryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IncomeEntryWidget()
}

@androidx.compose.runtime.Composable
private fun BalanceWidgetContent(data: QuickActionData, size: DpSize) {
    val small = size.width < WidgetTokens.compactWidth || size.height < 90.dp
    val short = size.height < WidgetTokens.expandedHeight
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetSurface()
            .padding(widgetPadding(small))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        if (short) {
            Column(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = "Balance",
                    style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.smallLabelSize)
                )
                Text(
                    text = CurrencyFormatter.format(data.balance, data.currencyCode),
                    style = TextStyle(color = WidgetPalette.text, fontSize = WidgetTokens.compactAmountSize, fontWeight = androidx.glance.text.FontWeight.Bold),
                    maxLines = 1
                )
            }
        } else {
            Text(
                text = "Balance",
                style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.bodySize)
            )
            Text(
                text = CurrencyFormatter.format(data.balance, data.currencyCode),
                style = TextStyle(
                    color = WidgetPalette.text,
                    fontSize = if (small) WidgetTokens.compactAmountSize else WidgetTokens.amountSize,
                    fontWeight = androidx.glance.text.FontWeight.Bold
                ),
                maxLines = 1
            )
            if (!small && !short) {
                Spacer(GlanceModifier.height(WidgetTokens.compactSpacing))
                Text(
                    text = if (data.hasAccounts) "Across your accounts" else "Add an account to begin",
                    style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.bodySize),
                    maxLines = 1
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MonthlySummaryWidgetContent(data: WidgetData, size: DpSize) {
    val small = size.width < WidgetTokens.compactWidth
    val short = size.height < WidgetTokens.expandedHeight
    val expanded = size.width >= WidgetTokens.expandedWidth && size.height >= WidgetTokens.expandedHeight
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetSurface()
            .padding(widgetPadding(small))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "This month",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = WidgetPalette.text,
                    fontSize = if (short) WidgetTokens.bodySize else WidgetTokens.titleSize,
                    fontWeight = androidx.glance.text.FontWeight.Bold
                )
            )
            Text(
                text = data.currencyCode,
                style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.labelSize)
            )
        }
        Spacer(GlanceModifier.height(if (short) WidgetTokens.compactSpacing else WidgetTokens.sectionSpacing))
        if (small) {
            MonthlyAmount("Income", data.monthIncome, data.currencyCode, WidgetPalette.accent)
            Spacer(GlanceModifier.height(WidgetTokens.compactSpacing))
            MonthlyAmount("Spent", data.monthExpenses, data.currencyCode, WidgetPalette.expense)
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                MonthlyAmount("Income", data.monthIncome, data.currencyCode, WidgetPalette.accent, GlanceModifier.defaultWeight())
                MonthlyAmount("Spent", data.monthExpenses, data.currencyCode, WidgetPalette.expense, GlanceModifier.defaultWeight())
            }
        }
        if (expanded) {
            Spacer(GlanceModifier.height(WidgetTokens.sectionSpacing))
            Text(
                text = "Net this month · ${CurrencyFormatter.format(data.monthIncome - data.monthExpenses, data.currencyCode)}",
                style = TextStyle(color = WidgetPalette.text, fontSize = WidgetTokens.bodySize, fontWeight = androidx.glance.text.FontWeight.Medium),
                maxLines = 1
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun MonthlyAmount(
    label: String,
    amount: Double,
    currencyCode: String,
    color: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(modifier = modifier) {
        Text(
                text = label,
                style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.labelSize, fontWeight = androidx.glance.text.FontWeight.Medium)
        )
        Text(
            text = CurrencyFormatter.format(amount, currencyCode),
            style = TextStyle(color = color, fontSize = WidgetTokens.titleSize, fontWeight = androidx.glance.text.FontWeight.Bold),
            maxLines = 1
        )
    }
}

@androidx.compose.runtime.Composable
private fun QuickEntryWidgetContent(label: String, symbol: String, action: Action, size: DpSize) {
    val small = size.width < WidgetTokens.compactWidth
    val short = size.height < WidgetTokens.expandedHeight
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetSurface()
            .padding(widgetPadding(small))
            .clickable(action),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        if (!small && !short) {
            Text(
                text = if (symbol == "+") "INCOME" else "EXPENSE",
                style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.labelSize, fontWeight = androidx.glance.text.FontWeight.Bold),
                maxLines = 1
            )
            Spacer(GlanceModifier.height(WidgetTokens.compactSpacing))
        }
        WidgetActionPill(
            label = "$symbol $label",
            action = action,
            primary = symbol == "+",
            modifier = GlanceModifier.fillMaxWidth(),
            height = WidgetTokens.actionHeight
        )
        if (!small && !short) {
            Spacer(GlanceModifier.height(WidgetTokens.compactSpacing))
            Text(
                text = "Tap to log",
                style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.labelSize)
            )
        }
    }
}
