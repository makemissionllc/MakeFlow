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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(
    private val financeRepository: FinanceRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    val selectedCurrency: StateFlow<String> = userPreferencesDataStore.selectedCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KES")

    val selectedTheme: StateFlow<String> = userPreferencesDataStore.selectedTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "forest")

    val hapticsEnabled: StateFlow<Boolean> = userPreferencesDataStore.hapticsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hasPromptedNotificationPermission: StateFlow<Boolean> = userPreferencesDataStore.hasPromptedNotificationPermission
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currencySymbol: StateFlow<String> = selectedCurrency.map { code ->
        when (code) {
            "USD" -> "$"
            "EUR" -> "\u20AC"
            "GBP" -> "\u00A3"
            "UGX" -> "UGX"
            "TZS" -> "TZS"
            else -> "KES"
        }
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
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(financeRepository, userPreferencesDataStore) as T
    }
}
