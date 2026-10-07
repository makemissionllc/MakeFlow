package com.makemission.makeflow.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import com.makemission.makeflow.QuickAddTransactionActivity
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.userPrefDataStore
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.domain.CurrencyConverter
import com.makemission.makeflow.domain.CurrencyRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class QuickActionWidget : GlanceAppWidget() {

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
            loadQuickActionData(context)
        }
        provideContent {
            QuickActionContent(data, LocalSize.current)
        }
    }
}

data class QuickActionData(
    val balance: Double,
    val currencyCode: String,
    val hasAccounts: Boolean
)

internal suspend fun loadQuickActionData(context: Context): QuickActionData {
    val db = AppDatabase.getDatabase(context)
    val accounts = db.accountDao().getAllAccounts().first()
    val prefsDataStore = com.makemission.makeflow.data.local.UserPreferencesDataStore(
        context.applicationContext.userPrefDataStore
    )
    val currencyCode = prefsDataStore.selectedCurrency.first()
    val rates = db.exchangeRateDao().getAllRatesOnce().mapNotNull { rate ->
        rate.baseCurrencyCode?.let { base ->
            CurrencyRate(rate.currencyCode, base, rate.rateToDisplayCurrency, rate.rateDate)
        }
    }
    val totalBalance = accounts.sumOf { account ->
        val accountCurrency = account.currencyCode ?: currencyCode
        CurrencyConverter.convert(account.balance, accountCurrency, currencyCode, rates) ?: 0.0
    }

    return QuickActionData(
        balance = totalBalance,
        currencyCode = currencyCode,
        hasAccounts = accounts.isNotEmpty()
    )
}

class ExpenseShortcutCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, QuickAddTransactionActivity::class.java).apply {
            putExtra("shortcut_type", "EXPENSE")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

class IncomeShortcutCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, QuickAddTransactionActivity::class.java).apply {
            putExtra("shortcut_type", "INCOME")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
private fun QuickActionContent(data: QuickActionData, size: DpSize) {
    val compact = size.width < WidgetTokens.compactWidth
    val expanded = size.width >= WidgetTokens.expandedWidth && size.height >= WidgetTokens.expandedHeight

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .widgetSurface()
            .padding(widgetPadding(compact)),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        QuickActionHeader(data.currencyCode)

        if (expanded && data.hasAccounts) {
            Spacer(GlanceModifier.height(WidgetTokens.compactSpacing))
            Text(
                text = "Balance",
                style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.bodySize, fontWeight = FontWeight.Medium)
            )
            Text(
                text = CurrencyFormatter.format(data.balance, data.currencyCode),
                style = TextStyle(color = WidgetPalette.text, fontSize = WidgetTokens.amountSize, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }

        Spacer(GlanceModifier.height(WidgetTokens.sectionSpacing))
        if (compact) {
            WidgetActionPill(
                label = "- Expense",
                action = actionRunCallback<ExpenseShortcutCallback>(),
                primary = false,
                modifier = GlanceModifier.fillMaxWidth(),
                height = WidgetTokens.compactActionHeight
            )
            Spacer(GlanceModifier.height(WidgetTokens.compactSpacing))
            WidgetActionPill(
                label = "+ Income",
                action = actionRunCallback<IncomeShortcutCallback>(),
                primary = true,
                height = WidgetTokens.compactActionHeight
            )
        } else {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WidgetActionPill(
                    label = "- Expense",
                    action = actionRunCallback<ExpenseShortcutCallback>(),
                    primary = false,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(GlanceModifier.width(WidgetTokens.compactSpacing))
                WidgetActionPill(
                    label = "+ Income",
                    action = actionRunCallback<IncomeShortcutCallback>(),
                    primary = true,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun QuickActionHeader(currencyCode: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Quick add",
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = WidgetPalette.muted,
                fontSize = WidgetTokens.bodySize,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        Text(
            text = currencyCode,
            style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.labelSize, fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}
