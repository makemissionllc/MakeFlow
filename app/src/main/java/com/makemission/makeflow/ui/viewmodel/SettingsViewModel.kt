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
import com.makemission.makeflow.core.haptics.HapticStrength
import com.makemission.makeflow.data.repository.CorrectionRepository
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
import com.makemission.makeflow.data.repository.ExchangeRateRepository
import com.makemission.makeflow.core.format.CurrencyFormatter
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val correctionRepository: CorrectionRepository? = null,
    private val backupManager: BackupManager? = null,
    private val exchangeRateRepository: ExchangeRateRepository? = null
) : ViewModel() {

    val selectedCurrency: StateFlow<String> = userPreferencesDataStore.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    val hapticLevel: StateFlow<HapticStrength> = userPreferencesDataStore.hapticLevel
        .map(HapticStrength::fromStorage)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HapticStrength.STANDARD)

    val hapticsEnabled: StateFlow<Boolean> = hapticLevel
        .map { it != HapticStrength.OFF }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appLockEnabled: StateFlow<Boolean> = userPreferencesDataStore.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val smartCategorizationEnabled: StateFlow<Boolean> = userPreferencesDataStore.smartCategorizationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val logMpesaCosts: StateFlow<Boolean> = userPreferencesDataStore.logMpesaCosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val mpesaAutoImportEnabled: StateFlow<Boolean> = userPreferencesDataStore.mpesaAutoImportEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode: StateFlow<String> = userPreferencesDataStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val amoledMode: StateFlow<Boolean> = userPreferencesDataStore.amoledMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasPromptedNotificationPermission: StateFlow<Boolean> = userPreferencesDataStore.hasPromptedNotificationPermission
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currencySymbol: StateFlow<String> = selectedCurrency.map { code ->
        CurrencyFormatter.symbolFor(code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

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

    fun saveHapticLevel(level: HapticStrength) {
        viewModelScope.launch {
            userPreferencesDataStore.saveHapticLevel(level.storageValue)
        }
    }

    fun saveAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.saveAppLockEnabled(enabled)
        }
    }

    fun saveSmartCategorizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.saveSmartCategorizationEnabled(enabled)
        }
    }

    fun saveLogMpesaCosts(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.saveLogMpesaCosts(enabled)
        }
    }

    fun saveMpesaAutoImportEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.saveMpesaAutoImportEnabled(enabled)
        }
    }

    fun saveThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferencesDataStore.saveThemeMode(mode)
        }
    }

    fun saveAmoledMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.saveAmoledMode(enabled)
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

            val fileName = "Mindful_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"

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
            val transactions = financeRepository.getTransactionsInRange(startMillis, endMillis).first()
            val accounts = financeRepository.getAllAccounts().first()
            val exchangeRatesList = exchangeRateRepository?.getAllRatesOnce() ?: emptyList()
            val exchangeRates = exchangeRatesList.associateBy { it.currencyCode }
            val accountMap = accounts.associateBy { it.id }
            val displayCurrency = selectedCurrency.value

            val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val headerDateFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

            val dateRangeLabel = "${headerDateFmt.format(Date(startMillis))} - ${headerDateFmt.format(Date(endMillis))}"

            val exportData = PdfExportDataBuilder.build(
                transactions = transactions,
                accountMap = accountMap,
                exchangeRates = exchangeRates,
                displayCurrency = displayCurrency
            ) { ts -> dateFmt.format(Date(ts)) }

            val rows = exportData.rows
            val summary = exportData.summary

            val document = PdfDocument()
            val pageWidth = 612
            val pageHeight = 792
            var pageNumber = 1

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
            val transferDetailPaint = Paint().apply {
                color = Color.GRAY
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 0.5f
            }
            val currencySymbol = CurrencyFormatter.symbolFor(displayCurrency)

            val leftMargin = 40f
            val rightMargin = 572f
            val colWidths = floatArrayOf(90f, 140f, 100f, 100f, 102f)
            val rowHeight = 16f
            val transferRowHeight = 26f

            fun drawHeader(c: Canvas, yStart: Float): Float {
                var yy = yStart
                val headers = listOf("Date", "Description", "Category", "Account", "Amount")
                var x = leftMargin
                headers.forEachIndexed { i, h ->
                    c.drawText(h, x, yy, headerPaint)
                    x += colWidths[i]
                }
                yy += 6f
                c.drawLine(leftMargin, yy, rightMargin, yy, linePaint)
                yy += 14f
                return yy
            }

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            var y = 40f
            canvas.drawText("Mindful Wallet Statement", leftMargin, y, titlePaint)
            y += 24f
            canvas.drawText("Date Range: $dateRangeLabel", leftMargin, y, rowPaint)
            y += 18f
            canvas.drawText("Display Currency: $displayCurrency ($currencySymbol)", leftMargin, y, rowPaint)
            y += 24f

            y = drawHeader(canvas, y)

            for (row in rows) {
                val rh = if (row.isTransfer) transferRowHeight else rowHeight

                if (y + rh > 750f) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f
                    y = drawHeader(canvas, y)
                }

                val cols = listOf(row.date, row.description, row.category, row.account, row.amount)
                var x = leftMargin
                cols.forEachIndexed { i, text ->
                    canvas.drawText(text, x, y, rowPaint)
                    x += colWidths[i]
                }

                if (row.isTransfer && row.transferDetail.isNotEmpty()) {
                    canvas.drawText(row.transferDetail, leftMargin, y + 12f, transferDetailPaint)
                }

                y += rh
            }

            y += 12f
            canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
            y += 20f

            val summaryPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("Total Income: $currencySymbol${summary.totalIncome}", leftMargin, y, summaryPaint)
            y += 16f
            canvas.drawText("Total Expense: $currencySymbol${summary.totalExpense}", leftMargin, y, summaryPaint)
            y += 16f
            canvas.drawText("Net: $currencySymbol${summary.net}", leftMargin, y, summaryPaint)

            document.finishPage(page)

            val fileName = "Mindful_Statement_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"

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

    suspend fun exportBackup(): String? {
        val mgr = backupManager ?: return null
        return mgr.serializeBackup()
    }

    suspend fun importBackup(json: String): String? {
        val mgr = backupManager ?: return "Backup manager not available"
        return mgr.restoreBackup(json)
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

    fun resetPersonalization() {
        viewModelScope.launch {
            correctionRepository?.resetCorrections()
            correctionRepository?.clearCorrectionMemory()
        }
    }
}

class SettingsViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val correctionRepository: CorrectionRepository? = null,
    private val backupManager: BackupManager? = null,
    private val exchangeRateRepository: ExchangeRateRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(financeRepository, userPreferencesDataStore, correctionRepository, backupManager, exchangeRateRepository) as T
    }
}
