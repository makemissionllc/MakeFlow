package com.makemission.makeflow.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
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
import androidx.glance.layout.Box
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
import com.makemission.makeflow.QuickAddTransactionActivity
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.userPrefDataStore
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
    val rates = db.exchangeRateDao().getAllRatesOnce()
        .associate { it.currencyCode to it.rateToDisplayCurrency }
    val totalBalance = accounts.sumOf { account ->
        val accountCurrency = account.currencyCode ?: currencyCode
        if (accountCurrency == currencyCode) account.balance
        else rates[accountCurrency]?.let { account.balance * it } ?: account.balance
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
    val bgColor = ColorProvider(com.makemission.makeflow.R.color.widget_bg)
    val textColor = ColorProvider(com.makemission.makeflow.R.color.widget_text)
    val mutedColor = ColorProvider(com.makemission.makeflow.R.color.widget_muted)
    val accentColor = ColorProvider(com.makemission.makeflow.R.color.widget_accent)
    val expenseColor = ColorProvider(com.makemission.makeflow.R.color.widget_warning)
    val compact = size.width < 180.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .padding(if (compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (compact) "Quick add" else "Mindful Wallet",
                style = TextStyle(color = mutedColor, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight()
            )
            if (!compact && data.hasAccounts) {
                Text(
                    text = CurrencyFormatter.format(data.balance, data.currencyCode),
                    style = TextStyle(color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }

        if (compact) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            QuickActionButton(
                label = "Expense",
                buttonColor = expenseColor,
                callback = actionRunCallback<ExpenseShortcutCallback>(),
                height = 28.dp
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            QuickActionButton(
                label = "Income",
                buttonColor = accentColor,
                callback = actionRunCallback<IncomeShortcutCallback>(),
                height = 28.dp
            )
        } else {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionButton(
                    label = "- Expense",
                    buttonColor = expenseColor,
                    callback = actionRunCallback<ExpenseShortcutCallback>(),
                    height = 40.dp,
                    modifier = GlanceModifier.defaultWeight(),
                    fillWidth = false
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                QuickActionButton(
                    label = "+ Income",
                    buttonColor = accentColor,
                    callback = actionRunCallback<IncomeShortcutCallback>(),
                    height = 40.dp,
                    modifier = GlanceModifier.defaultWeight(),
                    fillWidth = false
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    buttonColor: ColorProvider,
    callback: Action,
    height: Dp,
    modifier: GlanceModifier = GlanceModifier,
    fillWidth: Boolean = true
) {
    Box(
        modifier = modifier
            .then(if (fillWidth) GlanceModifier.fillMaxWidth() else GlanceModifier)
            .height(height)
            .background(buttonColor)
            .clickable(callback),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(com.makemission.makeflow.R.color.widget_bg),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
