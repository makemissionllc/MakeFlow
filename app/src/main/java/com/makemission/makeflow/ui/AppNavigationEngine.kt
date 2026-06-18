package com.makemission.makeflow.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.ui.screens.OnboardingScreen
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AppNavigationEngine() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("make_flow_prefs", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()

    var isOnboardingComplete by remember {
        mutableStateOf(sharedPrefs.getBoolean("onboarding_complete", false))
    }
    var savedName by remember { mutableStateOf(sharedPrefs.getString("user_name", "Jeremy") ?: "Jeremy") }
    var savedCurrency by remember { mutableStateOf(sharedPrefs.getString("currency", "KES") ?: "KES") }
    var savedDeposit by remember { mutableStateOf(sharedPrefs.getFloat("initial_deposit", 0.0f).toDouble()) }

    if (!isOnboardingComplete) {
        OnboardingScreen(
            onOnboardingComplete = { name, currency, initialBalance ->
                sharedPrefs.edit().apply {
                    putBoolean("onboarding_complete", true)
                    putString("user_name", name)
                    putString("currency", currency)
                    putFloat("initial_deposit", initialBalance.toFloat())
                    apply()
                }
                savedName = name
                savedCurrency = currency
                savedDeposit = initialBalance

                coroutineScope.launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        db.clearAllTables()
                        
                        // Seed default categories matching expected types
                        val salary = CategoryEntity(id = "salary", name = "Salary", type = "INCOME")
                        val allowance = CategoryEntity(id = "allowance", name = "Allowance", type = "INCOME")
                        val gifts = CategoryEntity(id = "gifts", name = "Gifts", type = "INCOME")
                        val freelance = CategoryEntity(id = "freelance", name = "Freelance", type = "INCOME")
                        
                        val church = CategoryEntity(id = "church", name = "Church", type = "EXPENSE")
                        val generosity = CategoryEntity(id = "generosity", name = "Generosity", type = "EXPENSE")
                        val foodDrink = CategoryEntity(id = "food_drink", name = "Food & Drink", type = "EXPENSE")
                        val transport = CategoryEntity(id = "transport", name = "Transport", type = "EXPENSE")
                        val rent = CategoryEntity(id = "rent", name = "Rent", type = "EXPENSE")

                        db.categoryDao().insertCategory(salary)
                        db.categoryDao().insertCategory(allowance)
                        db.categoryDao().insertCategory(gifts)
                        db.categoryDao().insertCategory(freelance)
                        db.categoryDao().insertCategory(church)
                        db.categoryDao().insertCategory(generosity)
                        db.categoryDao().insertCategory(foodDrink)
                        db.categoryDao().insertCategory(transport)
                        db.categoryDao().insertCategory(rent)

                        // Create a default Cash account
                        val defaultAccount = AccountEntity(
                            id = UUID.randomUUID().toString(),
                            name = "Cash",
                            balance = initialBalance,
                            initialBalance = initialBalance,
                            colorHex = "#43A047"
                        )
                        db.accountDao().insertAccount(defaultAccount)

                        // If there is an initial balance, record an initial deposit transaction
                        if (initialBalance > 0.0) {
                            val seedCategory = salary
                            val tx = TransactionEntity(
                                amount = initialBalance,
                                type = "INCOME",
                                categoryId = seedCategory.id,
                                note = "Initial Account Seed",
                                timestamp = System.currentTimeMillis()
                            )
                            db.transactionDao().insertTransaction(tx)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MakeFlowDebug", "Failed to seed onboarding database", e)
                    }
                }
            },
            onCompleted = {
                isOnboardingComplete = true
            }
        )
    } else {
        MainScreen(
            onboardingName = savedName,
            currency = savedCurrency,
            initialDeposit = savedDeposit,
            onLogout = {
                sharedPrefs.edit().clear().apply()
                isOnboardingComplete = false
            }
        )
    }
}
