package com.makemission.makeflow.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
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
import android.net.Uri
import com.makemission.makeflow.data.backup.BackupManager
import com.makemission.makeflow.data.repository.ExchangeRateRepository
import com.makemission.makeflow.core.format.CurrencyFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.makemission.makeflow.core.logging.AppLogger
import java.io.File
import java.io.IOException
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

    val financeReviewRemindersEnabled: StateFlow<Boolean> = userPreferencesDataStore.financeReviewRemindersEnabled
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

    fun saveFinanceReviewRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.saveFinanceReviewRemindersEnabled(enabled)
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

    fun exportPdf(
        context: Context,
        startMillis: Long,
        endMillis: Long,
        onError: () -> Unit = {},
        onComplete: (Uri, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val exported = withContext(Dispatchers.IO) {
                    val transactions = financeRepository.getTransactionsInRange(startMillis, endMillis).first()
                    val accounts = financeRepository.getAllAccounts().first()
                    val categories = financeRepository.getAllCategories().first()
                    val exchangeRates = exchangeRateRepository?.getAllRatesOnce()
                        ?.associateBy { it.currencyCode }
                        .orEmpty()
                    val displayCurrency = userPreferencesDataStore.selectedCurrency.first()
                    val isAllTime = startMillis <= 0L && endMillis == Long.MAX_VALUE
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val dateRangeLabel = if (isAllTime) {
                        "All time through ${dateFormat.format(Date())}"
                    } else {
                        val inclusiveEnd = if (endMillis > startMillis) endMillis - 1 else endMillis
                        "${dateFormat.format(Date(startMillis))} - ${dateFormat.format(Date(inclusiveEnd))}"
                    }
                    val transactionDateFormat = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())
                    val exportData = PdfExportDataBuilder.build(
                        transactions = transactions,
                        accountMap = accounts.associateBy { it.id },
                        categoryMap = categories.associateBy { it.id },
                        exchangeRates = exchangeRates,
                        displayCurrency = displayCurrency
                    ) { timestamp -> transactionDateFormat.format(Date(timestamp)) }
                    val fileName = PdfStatementFileName.forRange(startMillis, endMillis)
                    val document = PdfStatementRenderer.render(
                        rows = exportData.rows,
                        summary = exportData.summary,
                        currencyCode = displayCurrency,
                        dateRangeLabel = dateRangeLabel
                    )
                    try {
                        savePdf(context, document, fileName) to fileName
                    } finally {
                        document.close()
                    }
                }
                onComplete(exported.first, exported.second)
            } catch (error: Exception) {
                AppLogger.error("PdfExport", "Could not create the statement PDF", error)
                onError()
            }
        }
    }

    private fun savePdf(context: Context, document: android.graphics.pdf.PdfDocument, fileName: String): Uri {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MakeFlow")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Unable to create a PDF file")
            try {
                val output = resolver.openOutputStream(uri)
                    ?: throw IOException("Unable to open the PDF file")
                output.use(document::writeTo)
                val publishedValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                resolver.update(uri, publishedValues, null, null)
                return uri
            } catch (error: Exception) {
                resolver.delete(uri, null, null)
                throw error
            }
        }

        val documentsDirectory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, Environment.DIRECTORY_DOCUMENTS)
        val outputDirectory = File(documentsDirectory, "MakeFlow")
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw IOException("Unable to create the statement folder")
        }
        val outputFile = File(outputDirectory, fileName)
        outputFile.outputStream().use(document::writeTo)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile
        )
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
