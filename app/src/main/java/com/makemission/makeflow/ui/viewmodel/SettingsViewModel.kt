package com.makemission.makeflow.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.makemission.makeflow.data.backup.BackupManager
import com.makemission.makeflow.core.format.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val backupManager: BackupManager? = null
) : ViewModel() {

    val selectedCurrency: StateFlow<String> = userPreferencesDataStore.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    val selectedTheme: StateFlow<String> = userPreferencesDataStore.selectedTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "forest")

    val hapticsEnabled: StateFlow<Boolean> = userPreferencesDataStore.hapticsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appLockEnabled: StateFlow<Boolean> = userPreferencesDataStore.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasPromptedNotificationPermission: StateFlow<Boolean> = userPreferencesDataStore.hasPromptedNotificationPermission
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currencySymbol: StateFlow<String> = selectedCurrency.map { code ->
        CurrencyFormatter.symbolFor(code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    fun saveTheme(theme: String) {
        viewModelScope.launch {
            userPreferencesDataStore.saveTheme(theme)
        }
    }

    fun saveCurrency(currency: String) {
        viewModelScope.launch {
            userPreferencesDataStore.saveCurrency(currency)
        }
    }

    fun saveHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.saveHapticsEnabled(enabled)
        }
    }

    fun saveAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.saveAppLockEnabled(enabled)
        }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val transactions = financeRepository.getAllTransactions().stateIn(
                viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
            ).value
            val accounts = financeRepository.getAllAccounts().stateIn(
                viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
            ).value
            val accountMap = accounts.associateBy { it.id }

            val csv = buildString {
                appendLine("Date,Type,Amount,Account,Category,Note")
                transactions.forEach { tx ->
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(tx.timestamp))
                    val accountName = accountMap[tx.accountId]?.name ?: tx.accountId
                    val categoryName = tx.categoryId ?: ""
                    appendLine("$date,${tx.type},${tx.amount},$accountName,$categoryName,${tx.note ?: ""}")
                }
            }

            val fileName = "MakeFlow_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { output ->
                        output.write(csv.toByteArray())
                    }
                }
            }
        }
    }

    fun exportPdf(context: Context, startMillis: Long, endMillis: Long, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            val transactions = financeRepository.getTransactionsInRange(startMillis, endMillis)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()).value
            val accounts = financeRepository.getAllAccounts()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()).value
            val accountMap = accounts.associateBy { it.id }
            val displayCurrency = selectedCurrency.value

            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val headerDateFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

            val dateRangeLabel = "${headerDateFmt.format(Date(startMillis))} - ${headerDateFmt.format(Date(endMillis))}"

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create() // US Letter
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
            }
            val headerPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
            }
            val rowPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 11f
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 0.5f
            }
            val currencySymbol = CurrencyFormatter.symbolFor(displayCurrency)

            var y = 40f
            val leftMargin = 40f
            val rightMargin = 572f
            val colWidths = floatArrayOf(100f, 130f, 100f, 100f, 102f)

            canvas.drawText("MakeFlow Statement", leftMargin, y, titlePaint)
            y += 24f
            canvas.drawText("Date Range: $dateRangeLabel", leftMargin, y, rowPaint)
            y += 18f
            canvas.drawText("Display Currency: $displayCurrency ($currencySymbol)", leftMargin, y, rowPaint)
            y += 24f

            val headers = listOf("Date", "Description", "Category", "Account", "Amount")
            var x = leftMargin
            headers.forEachIndexed { i, h ->
                canvas.drawText(h, x, y, headerPaint)
                x += colWidths[i]
            }
            y += 6f
            canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
            y += 14f

            var totalIncome = 0.0
            var totalExpense = 0.0

            for (tx in transactions) {
                val date = dateFmt.format(Date(tx.timestamp))
                val accountName = accountMap[tx.accountId]?.name ?: tx.accountId
                val categoryName = tx.categoryId ?: ""
                val amountStr = "${tx.amount}"
                val note = tx.note ?: ""

                if (tx.type == "INCOME") totalIncome += tx.amount
                else totalExpense += tx.amount

                val cols = listOf(date, note, categoryName, accountName, amountStr)
                x = leftMargin
                cols.forEachIndexed { i, text ->
                    canvas.drawText(text, x, y, rowPaint)
                    x += colWidths[i]
                }
                y += 16f

                if (y > 750f) {
                    document.finishPage(page)
                    break
                }
            }

            y += 12f
            canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
            y += 20f

            val net = totalIncome - totalExpense
            val summaryPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("Total Income: $currencySymbol$totalIncome", leftMargin, y, summaryPaint)
            y += 16f
            canvas.drawText("Total Expense: $currencySymbol$totalExpense", leftMargin, y, summaryPaint)
            y += 16f
            canvas.drawText("Net: $currencySymbol$net", leftMargin, y, summaryPaint)

            document.finishPage(page)

            val fileName = "MakeFlow_Statement_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { output ->
                        document.writeTo(output)
                    }
                }
            }
            document.close()
            onComplete(fileName)
        }
    }

    fun exportBackup(block: suspend (String) -> Unit) {
        viewModelScope.launch {
            val mgr = backupManager ?: return@launch
            val json = mgr.serializeBackup()
            block(json)
        }
    }

    fun importBackup(json: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val mgr = backupManager ?: run { onResult("Backup manager not available"); return@launch }
            val error = mgr.restoreBackup(json)
            onResult(error)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            financeRepository.clearAllTransactions()
            financeRepository.clearAllBudgets()
            financeRepository.clearAllAccounts()
            financeRepository.clearAllCategories()
            userPreferencesDataStore.clearProfile()
        }
    }
}

class SettingsViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val backupManager: BackupManager? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(financeRepository, userPreferencesDataStore, backupManager) as T
    }
}
