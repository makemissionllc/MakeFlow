package com.makemission.makeflow.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.Dispatchers
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
    var savedAccountName by remember { mutableStateOf(sharedPrefs.getString("account_name", "Cash") ?: "Cash") }

    LaunchedEffect(Unit) {
        try {
            val db = AppDatabase.getDatabase(context)
            if (db.categoryDao().getAnyCategory() == null) {
                // 1. Seed Expense Parent Categories
                val foodDrinks = CategoryEntity(id = "exp_food_drinks", name = "Food & Drinks", type = "EXPENSE", iconName = "ShoppingCart", colorHex = "#FB8C00")
                val generosity = CategoryEntity(id = "exp_generosity", name = "Generosity", type = "EXPENSE", iconName = "Star", colorHex = "#E91E63")
                val shopping = CategoryEntity(id = "exp_shopping", name = "Shopping", type = "EXPENSE", iconName = "ShoppingCart", colorHex = "#9C27B0")
                val housing = CategoryEntity(id = "exp_housing", name = "Housing", type = "EXPENSE", iconName = "Home", colorHex = "#E53935")
                val bills = CategoryEntity(id = "exp_bills", name = "Bills", type = "EXPENSE", iconName = "Check", colorHex = "#00BCD4")
                val transport = CategoryEntity(id = "exp_transport", name = "Transport", type = "EXPENSE", iconName = "LocationOn", colorHex = "#FFC107")
                val vehicle = CategoryEntity(id = "exp_vehicle", name = "Vehicle", type = "EXPENSE", iconName = "LocationOn", colorHex = "#795548")
                val leisure = CategoryEntity(id = "exp_leisure", name = "Leisure", type = "EXPENSE", iconName = "Star", colorHex = "#009688")
                val otherExpense = CategoryEntity(id = "exp_other", name = "Other", type = "EXPENSE", iconName = "Check", colorHex = "#9E9E9E")

                db.categoryDao().insertCategory(foodDrinks)
                db.categoryDao().insertCategory(generosity)
                db.categoryDao().insertCategory(shopping)
                db.categoryDao().insertCategory(housing)
                db.categoryDao().insertCategory(bills)
                db.categoryDao().insertCategory(transport)
                db.categoryDao().insertCategory(vehicle)
                db.categoryDao().insertCategory(leisure)
                db.categoryDao().insertCategory(otherExpense)

                suspend fun seedSubs(parent: CategoryEntity, subNames: List<String>) {
                    subNames.forEach { subName ->
                        val subId = "${parent.id}_${subName.lowercase().replace(" ", "_")}"
                        val sub = CategoryEntity(
                            id = subId,
                            name = subName,
                            type = "EXPENSE",
                            parentCategoryId = parent.id,
                            iconName = parent.iconName,
                            colorHex = parent.colorHex
                        )
                        db.categoryDao().insertCategory(sub)
                    }
                }

                seedSubs(foodDrinks, listOf("Restaurant", "Delievery", "Snacks"))
                seedSubs(generosity, listOf("Gifts"))
                seedSubs(shopping, listOf("Groceries", "Clothing", "Electronics", "Kids", "Pets"))
                seedSubs(housing, listOf("Utilities", "Mortagage", "Rent", "Maintenance"))
                seedSubs(bills, listOf("Phone", "Internet", "Subscriptions", "Healthcare", "Taxes"))
                seedSubs(transport, listOf("Public", "Private", "Long Distance"))
                seedSubs(vehicle, listOf("Fuel", "Parking", "Maintenance", "Insurance"))
                seedSubs(leisure, listOf("Entertainment", "Sports", "Hobbies", "Vacations"))

                val salary = CategoryEntity(id = "inc_salary", name = "Salary", type = "INCOME", iconName = "Check", colorHex = "#43A047")
                val interest = CategoryEntity(id = "inc_interest", name = "Interest", type = "INCOME", iconName = "Check", colorHex = "#8BC34A")
                val giftsIncome = CategoryEntity(id = "inc_gifts", name = "Gifts", type = "INCOME", iconName = "Star", colorHex = "#CDDC39")
                val otherIncome = CategoryEntity(id = "inc_other", name = "Other", type = "INCOME", iconName = "Check", colorHex = "#607D8B")

                db.categoryDao().insertCategory(salary)
                db.categoryDao().insertCategory(interest)
                db.categoryDao().insertCategory(giftsIncome)
                db.categoryDao().insertCategory(otherIncome)
            }
            if (db.accountDao().getAnyAccount() == null) {
                val defaultAccount = AccountEntity(
                    id = UUID.randomUUID().toString(),
                    name = savedAccountName,
                    balance = savedDeposit,
                    initialBalance = savedDeposit,
                    colorHex = "#43A047"
                )
                db.accountDao().insertAccount(defaultAccount)
                if (savedDeposit > 0.0) {
                    val tx = TransactionEntity(
                        amount = savedDeposit,
                        type = "INCOME",
                        categoryId = "inc_salary",
                        note = "Initial Account Seed",
                        timestamp = System.currentTimeMillis()
                    )
                    db.transactionDao().insertTransaction(tx)
                }
            }
        } catch (e: java.lang.Exception) {
            android.util.Log.e("MakeFlowDebug", "Failed to seed database at startup", e)
        }
    }

    if (!isOnboardingComplete) {
        OnboardingScreen(
            onOnboardingComplete = { name, currency, accountName, initialBalance ->
                sharedPrefs.edit().apply {
                    putBoolean("onboarding_complete", true)
                    putString("user_name", name)
                    putString("currency", currency)
                    putFloat("initial_deposit", initialBalance.toFloat())
                    putString("account_name", accountName)
                    apply()
                }
                savedName = name
                savedCurrency = currency
                savedDeposit = initialBalance
                savedAccountName = accountName

                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        db.clearAllTables()
                        
                        // Seed default categories matching expected types
                        val foodDrinks = CategoryEntity(id = "exp_food_drinks", name = "Food & Drinks", type = "EXPENSE", iconName = "ShoppingCart", colorHex = "#FB8C00")
                        val generosity = CategoryEntity(id = "exp_generosity", name = "Generosity", type = "EXPENSE", iconName = "Star", colorHex = "#E91E63")
                        val shopping = CategoryEntity(id = "exp_shopping", name = "Shopping", type = "EXPENSE", iconName = "ShoppingCart", colorHex = "#9C27B0")
                        val housing = CategoryEntity(id = "exp_housing", name = "Housing", type = "EXPENSE", iconName = "Home", colorHex = "#E53935")
                        val bills = CategoryEntity(id = "exp_bills", name = "Bills", type = "EXPENSE", iconName = "Check", colorHex = "#00BCD4")
                        val transport = CategoryEntity(id = "exp_transport", name = "Transport", type = "EXPENSE", iconName = "LocationOn", colorHex = "#FFC107")
                        val vehicle = CategoryEntity(id = "exp_vehicle", name = "Vehicle", type = "EXPENSE", iconName = "LocationOn", colorHex = "#795548")
                        val leisure = CategoryEntity(id = "exp_leisure", name = "Leisure", type = "EXPENSE", iconName = "Star", colorHex = "#009688")
                        val otherExpense = CategoryEntity(id = "exp_other", name = "Other", type = "EXPENSE", iconName = "Check", colorHex = "#9E9E9E")

                        db.categoryDao().insertCategory(foodDrinks)
                        db.categoryDao().insertCategory(generosity)
                        db.categoryDao().insertCategory(shopping)
                        db.categoryDao().insertCategory(housing)
                        db.categoryDao().insertCategory(bills)
                        db.categoryDao().insertCategory(transport)
                        db.categoryDao().insertCategory(vehicle)
                        db.categoryDao().insertCategory(leisure)
                        db.categoryDao().insertCategory(otherExpense)

                        suspend fun seedSubs(parent: CategoryEntity, subNames: List<String>) {
                            subNames.forEach { subName ->
                                val subId = "${parent.id}_${subName.lowercase().replace(" ", "_")}"
                                val sub = CategoryEntity(
                                    id = subId,
                                    name = subName,
                                    type = "EXPENSE",
                                    parentCategoryId = parent.id,
                                    iconName = parent.iconName,
                                    colorHex = parent.colorHex
                                )
                                db.categoryDao().insertCategory(sub)
                            }
                        }

                        seedSubs(foodDrinks, listOf("Restaurant", "Delievery", "Snacks"))
                        seedSubs(generosity, listOf("Gifts"))
                        seedSubs(shopping, listOf("Groceries", "Clothing", "Electronics", "Kids", "Pets"))
                        seedSubs(housing, listOf("Utilities", "Mortagage", "Rent", "Maintenance"))
                        seedSubs(bills, listOf("Phone", "Internet", "Subscriptions", "Healthcare", "Taxes"))
                        seedSubs(transport, listOf("Public", "Private", "Long Distance"))
                        seedSubs(vehicle, listOf("Fuel", "Parking", "Maintenance", "Insurance"))
                        seedSubs(leisure, listOf("Entertainment", "Sports", "Hobbies", "Vacations"))

                        val salary = CategoryEntity(id = "inc_salary", name = "Salary", type = "INCOME", iconName = "Check", colorHex = "#43A047")
                        val interest = CategoryEntity(id = "inc_interest", name = "Interest", type = "INCOME", iconName = "Check", colorHex = "#8BC34A")
                        val giftsIncome = CategoryEntity(id = "inc_gifts", name = "Gifts", type = "INCOME", iconName = "Star", colorHex = "#CDDC39")
                        val otherIncome = CategoryEntity(id = "inc_other", name = "Other", type = "INCOME", iconName = "Check", colorHex = "#607D8B")

                        db.categoryDao().insertCategory(salary)
                        db.categoryDao().insertCategory(interest)
                        db.categoryDao().insertCategory(giftsIncome)
                        db.categoryDao().insertCategory(otherIncome)

                        // Create a default user-configured account
                        val defaultAccount = AccountEntity(
                            id = UUID.randomUUID().toString(),
                            name = accountName,
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
