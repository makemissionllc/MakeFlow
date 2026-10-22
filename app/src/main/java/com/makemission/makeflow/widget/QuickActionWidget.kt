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
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.userPrefDataStore
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
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
    val hasAccounts: Boolean,
    val quickAddAccountName: String? = null,
    val latestRepeat: QuickRepeatTransaction? = null
)

data class QuickRepeatTransaction(
    val type: String,
    val amount: Double,
    val note: String,
    val categoryName: String?,
    val accountName: String?,
    val accountCurrencyCode: String
)

internal suspend fun loadQuickActionData(context: Context): QuickActionData {
    val db = AppDatabase.getDatabase(context)
    val accounts = db.accountDao().getAllAccounts().first()
    val transactions = db.transactionDao().getAllTransactionsOnce()
    val categories = db.categoryDao().getAllCategoriesOnce()
    val prefsDataStore = UserPreferencesDataStore(
        context.applicationContext.userPrefDataStore
    )
    val preferredAccountId = prefsDataStore.lastTransactionAccountId.first()
    val preferredAccount = accounts.firstOrNull { it.id == preferredAccountId }
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
        hasAccounts = accounts.isNotEmpty(),
        quickAddAccountName = (preferredAccount ?: accounts.firstOrNull())?.name,
        latestRepeat = transactions.firstOrNull { it.type == "EXPENSE" || it.type == "INCOME" }
            ?.toQuickRepeatTransaction(accounts, categories, currencyCode)
    )
}

private fun TransactionEntity.toQuickRepeatTransaction(
    accounts: List<com.makemission.makeflow.data.local.entity.AccountEntity>,
    categories: List<com.makemission.makeflow.data.local.entity.CategoryEntity>,
    fallbackCurrencyCode: String
) = QuickRepeatTransaction(
    type = type,
    amount = amount,
    note = note,
    categoryName = categories.firstOrNull { it.id == categoryId }?.name,
    accountName = accounts.firstOrNull { it.id == accountId }?.name,
    accountCurrencyCode = accounts.firstOrNull { it.id == accountId }?.currencyCode ?: fallbackCurrencyCode
)

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

class RepeatLastTransactionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val transaction = AppDatabase.getDatabase(context).transactionDao()
            .getAllTransactionsOnce()
            .firstOrNull { it.type == "EXPENSE" || it.type == "INCOME" }
            ?: return
        val intent = Intent(context, QuickAddTransactionActivity::class.java).apply {
            putExtra("shortcut_type", transaction.type)
            putExtra("initial_amount", transaction.amount.toString())
            putExtra("initial_note", transaction.note)
            putExtra("initial_account_id", transaction.accountId)
            putExtra("initial_category_id", transaction.categoryId)
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
                modifier = GlanceModifier.fillMaxWidth(),
                height = WidgetTokens.compactActionHeight
            )
        } else {
            if (expanded) {
                QuickActionHeader(data.currencyCode)

                data.quickAddAccountName?.let { accountName ->
                    Text(
                        text = "Using $accountName",
                        style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.labelSize),
                        maxLines = 1
                    )
                }

                Spacer(GlanceModifier.height(WidgetTokens.sectionSpacing))
            }
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

        if (expanded) {
            data.latestRepeat?.let { transaction ->
                Spacer(GlanceModifier.height(WidgetTokens.sectionSpacing))
                val summary = transaction.note.ifBlank { transaction.categoryName ?: "Last ${transaction.type.lowercase()}" }
                Text(
                    text = "Recent · $summary",
                    style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.labelSize),
                    maxLines = 1
                )
                Text(
                    text = "${transaction.accountName ?: "Account"} · ${CurrencyFormatter.format(transaction.amount, transaction.accountCurrencyCode)}",
                    style = TextStyle(color = WidgetPalette.muted, fontSize = WidgetTokens.labelSize),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(WidgetTokens.compactSpacing))
                WidgetActionPill(
                    label = "Repeat last transaction",
                    action = actionRunCallback<RepeatLastTransactionCallback>(),
                    primary = false,
                    modifier = GlanceModifier.fillMaxWidth(),
                    height = WidgetTokens.actionHeight
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
