package com.makemission.makeflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.ui.components.ExpressiveNavDock
import com.makemission.makeflow.ui.components.MakeFlowTab
import com.makemission.makeflow.ui.screens.AnalyticsScreen
import com.makemission.makeflow.ui.screens.FocusDashboardView
import com.makemission.makeflow.ui.screens.GeneralSettingsBottomSheet
import com.makemission.makeflow.ui.screens.HistoryScreen
import com.makemission.makeflow.ui.screens.TransactionEntrySheetContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onboardingName: String = "Developer",
    currency: String = "KSh",
    initialDeposit: Double = 12500.00,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(MakeFlowTab.FOCUS) }
    var isSettingsSheetOpen by remember { mutableStateOf(false) }
    var isTransactionSheetOpen by remember { mutableStateOf(false) }

    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val transactionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = 80.dp)
        ) {
            when (activeTab) {
                MakeFlowTab.FOCUS -> FocusDashboardView(
                    onboardingName = onboardingName,
                    currency = currency,
                    initialDeposit = initialDeposit,
                    onSettingsClick = { isSettingsSheetOpen = true }
                )
                MakeFlowTab.HISTORY -> HistoryScreen(
                    currency = currency,
                    initialDeposit = initialDeposit
                )
                MakeFlowTab.INSIGHTS -> AnalyticsScreen(
                    currency = currency,
                    initialDeposit = initialDeposit
                )
            }
        }

        ExpressiveNavDock(
            currentTab = activeTab,
            onTabSelected = { activeTab = it },
            onFabClick = { isTransactionSheetOpen = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        if (isSettingsSheetOpen) {
            GeneralSettingsBottomSheet(
                sheetState = settingsSheetState,
                onDismissRequest = { isSettingsSheetOpen = false }
            )
        }

        if (isTransactionSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isTransactionSheetOpen = false },
                sheetState = transactionSheetState,
                containerColor = Color(0xFF0F1011),
                dragHandle = null // Content uses its custom close layout system natively
            ) {
                TransactionEntrySheetContent(
                    onDismiss = { isTransactionSheetOpen = false },
                    onSaveTransaction = { type, account, assetCategory, amount ->
                        coroutineScope.launch {
                            try {
                                val db = AppDatabase.getDatabase(context)
                                val mappedCategoryName = when (assetCategory) {
                                    "Food & Drink" -> "Food & Dining"
                                    "Transport" -> "Transportation"
                                    else -> assetCategory
                                }

                                var category = db.categoryDao().getCategoryByName(mappedCategoryName)
                                if (category == null) {
                                    category = CategoryEntity(
                                        name = mappedCategoryName,
                                        iconName = "Check",
                                        colorHex = "#9E9E9E",
                                        monthlyBudget = 0.0
                                    )
                                    db.categoryDao().insertCategory(category)
                                }

                                val tx = TransactionEntity(
                                    amount = amount,
                                    type = type.name, // "INCOME", "EXPENSE", "TRANSFER"
                                    categoryId = category.id,
                                    note = "Transaction via ${account.name} -> $assetCategory",
                                    timestamp = System.currentTimeMillis()
                                )
                                db.transactionDao().insertTransaction(tx)
                            } catch (e: Exception) {
                                android.util.Log.e("MakeFlowDebug", "Failed to save transaction", e)
                            }
                            isTransactionSheetOpen = false
                        }
                    }
                )
            }
        }
    }
}
