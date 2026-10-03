package com.makemission.makeflow.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
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

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) {
            loadQuickActionData(context)
        }
        provideContent {
            QuickActionContent(data)
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
    val totalBalance = accounts.sumOf { it.balance }

    val prefsDataStore = com.makemission.makeflow.data.local.UserPreferencesDataStore(
        context.applicationContext.userPrefDataStore
    )
    val currencyCode = prefsDataStore.selectedCurrency.first()

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
private fun QuickActionContent(data: QuickActionData) {
    val bgColor = ColorProvider(com.makemission.makeflow.R.color.widget_bg)
    val textColor = ColorProvider(com.makemission.makeflow.R.color.widget_text)
    val mutedColor = ColorProvider(com.makemission.makeflow.R.color.widget_muted)
    val accentColor = ColorProvider(com.makemission.makeflow.R.color.widget_accent)
    val expenseColor = ColorProvider(com.makemission.makeflow.R.color.widget_warning)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "Quick Actions",
            style = TextStyle(
                color = mutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )

        if (data.hasAccounts) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = CurrencyFormatter.format(data.balance, data.currencyCode),
                style = TextStyle(
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(32.dp)
                .background(expenseColor)
                .clickable(actionRunCallback<ExpenseShortcutCallback>()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "− Expense",
                style = TextStyle(
                    color = ColorProvider(com.makemission.makeflow.R.color.widget_bg),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(32.dp)
                .background(accentColor)
                .clickable(actionRunCallback<IncomeShortcutCallback>()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ Income",
                style = TextStyle(
                    color = ColorProvider(com.makemission.makeflow.R.color.widget_bg),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
