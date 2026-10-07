package com.makemission.makeflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.haptics.rememberHapticManager
import com.makemission.makeflow.core.ml.CategoryClassifier
import com.makemission.makeflow.core.ml.TfliteCategoryClassifier
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.userPrefDataStore
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.repository.FinanceRepositoryImpl
import com.makemission.makeflow.data.repository.TransactionTemplateRepositoryImpl
import com.makemission.makeflow.data.repository.ExchangeRateRepositoryImpl
import com.makemission.makeflow.domain.AnomalyDetector
import com.makemission.makeflow.domain.PastTransactionForSuggestion
import com.makemission.makeflow.domain.TransactionCommitHandler
import com.makemission.makeflow.ui.screens.AddTransactionBottomSheet
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import com.makemission.makeflow.widget.WidgetUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickAddTransactionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialType = intent.getStringExtra("shortcut_type") ?: "INCOME"

        enableEdgeToEdge()

        setContent {
            MakeFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = applicationContext
                    val scope = rememberCoroutineScope()

                    val db = remember { AppDatabase.getDatabase(context) }
                    val repository = remember {
                        FinanceRepositoryImpl(
                            categoryDao = db.categoryDao(),
                            transactionDao = db.transactionDao(),
                            taskDao = db.taskDao(),
                            accountDao = db.accountDao(),
                            budgetDao = db.budgetDao(),
                            recurringPatternDao = db.recurringPatternDao(),
                            recurringTransactionDao = db.recurringTransactionDao()
                        )
                    }
                    val templateRepository = remember { TransactionTemplateRepositoryImpl(db.transactionTemplateDao()) }
                    val exchangeRateRepository = remember { ExchangeRateRepositoryImpl(db.exchangeRateDao()) }
                    val exchangeRates by exchangeRateRepository.getAllRates().collectAsState(initial = emptyList())
                    val templates by templateRepository.getAllTemplates().collectAsState(initial = emptyList())
                    val userPrefDataStore = remember { UserPreferencesDataStore(context.userPrefDataStore) }
                    val accounts by repository.getAllAccounts().collectAsState(initial = emptyList())
                    val categories by repository.getAllCategories().collectAsState(initial = emptyList())
                    val currencyCode by userPrefDataStore.selectedCurrency.collectAsState(initial = "KES")
                    val smartCategorizationEnabled by userPrefDataStore.smartCategorizationEnabled.collectAsState(initial = true)

                    val hapticManager = rememberHapticManager()

                    var categoryClassifier by remember { mutableStateOf<CategoryClassifier?>(null) }
                    LaunchedEffect(Unit) {
                        categoryClassifier = TfliteCategoryClassifier.create(context)
                    }

                    val commitHandler = remember {
                        TransactionCommitHandler(
                            onCheckDuplicate = { entity ->
                                repository.checkDuplicate(entity)
                            },
                            onAnomalyCheck = { amount, categoryId ->
                                if (categoryId == null) return@TransactionCommitHandler null
                                val history = repository.getRecentAmountsForCategory(categoryId)
                                AnomalyDetector.analyze(amount, history)
                            },
                            hapticManager = hapticManager
                        )
                    }

                    AddTransactionBottomSheet(
                        initialType = initialType,
                        accounts = accounts,
                        categories = categories,
                        currencyCode = currencyCode,
                        exchangeRates = exchangeRates,
                        hapticManager = hapticManager,
                        templates = templates,
                        commitHandler = commitHandler,
                        onSaveAsTemplate = { name, amount, type, categoryId, accountId, note ->
                            scope.launch(Dispatchers.IO) {
                                templateRepository.insertTemplate(
                                    com.makemission.makeflow.data.local.entity.TransactionTemplateEntity(
                                        name = name,
                                        amount = amount,
                                        type = type,
                                        categoryId = categoryId,
                                        accountId = accountId,
                                        note = note
                                    )
                                )
                            }
                        },
                        onGetPastTransactions = {
                            val pairs = repository.getRecentDescriptionsAndCategories()
                            pairs.map { PastTransactionForSuggestion(it.description, it.categoryId) }
                        },
                        categoryClassifier = categoryClassifier,
                        smartCategorizationEnabled = smartCategorizationEnabled,
                        onDismiss = {
                            commitHandler.clearAlerts()
                            finish()
                        },
                        onSave = { amount, type, categoryId, accountId, note, timestamp, fromAccountId, toAccountId, destinationAmount ->
                            val entity = TransactionEntity(
                                amount = amount,
                                type = type,
                                categoryId = categoryId,
                                accountId = accountId,
                                fromAccountId = fromAccountId,
                                toAccountId = toAccountId,
                                note = note,
                                timestamp = timestamp,
                                destinationAmount = destinationAmount
                            )
                            withContext(Dispatchers.IO) {
                                repository.insertTransaction(entity)
                                WidgetUpdateHelper.updateWidgets(context)
                            }
                        }
                    )

                    DisposableEffect(Unit) {
                        onDispose {
                            commitHandler.clearAlerts()
                        }
                    }
                }
            }
        }
    }
}
