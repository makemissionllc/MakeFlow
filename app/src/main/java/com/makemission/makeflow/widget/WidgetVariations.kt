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
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
                color = ColorProvider(R.color.widget_warning),
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
                color = ColorProvider(R.color.widget_accent),
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
    val small = size.width < 180.dp
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.widget_bg))
            .padding(if (small) 10.dp else 14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "TOTAL BALANCE",
            style = TextStyle(
                color = ColorProvider(R.color.widget_muted),
                fontSize = if (small) 9.sp else 11.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        Text(
            text = CurrencyFormatter.format(data.balance, data.currencyCode),
            style = TextStyle(
                color = ColorProvider(R.color.widget_text),
                fontSize = if (small) 16.sp else 23.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        if (!small) {
            Text(
                text = if (data.hasAccounts) "Across your accounts" else "Add an account to begin",
                style = TextStyle(color = ColorProvider(R.color.widget_muted), fontSize = 11.sp),
                maxLines = 1
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun MonthlySummaryWidgetContent(data: WidgetData, size: DpSize) {
    val small = size.width < 180.dp
    val expanded = size.width >= 220.dp && size.height >= 180.dp
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.widget_bg))
            .padding(if (small) 10.dp else 14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        if (expanded) {
            Text(
                text = "THIS MONTH",
                style = TextStyle(color = ColorProvider(R.color.widget_muted), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(GlanceModifier.height(8.dp))
        }
        if (small) {
            MonthlyAmount("IN", data.monthIncome, data.currencyCode, R.color.widget_accent)
            Spacer(GlanceModifier.height(8.dp))
            MonthlyAmount("OUT", data.monthExpenses, data.currencyCode, R.color.widget_warning)
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                MonthlyAmount("IN", data.monthIncome, data.currencyCode, R.color.widget_accent, GlanceModifier.defaultWeight())
                MonthlyAmount("OUT", data.monthExpenses, data.currencyCode, R.color.widget_warning, GlanceModifier.defaultWeight())
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MonthlyAmount(
    label: String,
    amount: Double,
    currencyCode: String,
    colorRes: Int,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = TextStyle(color = ColorProvider(R.color.widget_muted), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        )
        Text(
            text = CurrencyFormatter.format(amount, currencyCode),
            style = TextStyle(color = ColorProvider(colorRes), fontSize = 16.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

@androidx.compose.runtime.Composable
private fun QuickEntryWidgetContent(label: String, symbol: String, action: Action, color: ColorProvider, size: DpSize) {
    val small = size.width < 180.dp
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.widget_bg))
            .padding(if (small) 10.dp else 14.dp)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            Text(
                text = symbol,
                style = TextStyle(color = color, fontSize = if (small) 25.sp else 34.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = label,
                style = TextStyle(color = ColorProvider(R.color.widget_text), fontSize = if (small) 12.sp else 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            if (!small) {
                Text(
                    text = "Tap to log",
                    style = TextStyle(color = ColorProvider(R.color.widget_muted), fontSize = 11.sp)
                )
            }
        }
    }
}
