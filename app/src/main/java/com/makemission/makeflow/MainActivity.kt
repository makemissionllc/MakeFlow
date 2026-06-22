package com.makemission.makeflow

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.ui.navigation.AppNavigationEngine
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember {
                context.getSharedPreferences("make_flow_prefs", Context.MODE_PRIVATE)
            }

            val userExists = sharedPrefs.getBoolean("onboarding_complete", false)
            val savedName = sharedPrefs.getString("user_name", "") ?: ""

            MakeFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationEngine(
                        initialUserExists = userExists,
                        currentUsername = savedName,
                        onPersistProfileAndAccounts = { username, accounts ->
                            sharedPrefs.edit()
                                .putBoolean("onboarding_complete", true)
                                .putString("user_name", username)
                                .putFloat("initial_deposit", accounts.sumOf { it.balance }.toFloat())
                                .apply()

                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val db = AppDatabase.getDatabase(context)
                                    db.clearAllTables()

                                    val salary = CategoryEntity(
                                        id = "inc_salary", name = "Salary", type = "INCOME",
                                        iconName = "Check", colorHex = "#43A047"
                                    )
                                    db.categoryDao().insertCategory(salary)

                                    for (account in accounts) {
                                        val entity = account.copy(colorHex = account.colorHex.ifEmpty { "#43A047" })
                                        db.accountDao().insertAccount(entity)
                                        if (entity.balance > 0.0) {
                                            db.transactionDao().insertTransaction(
                                                TransactionEntity(
                                                    amount = entity.balance,
                                                    type = "INCOME",
                                                    categoryId = salary.id,
                                                    note = "Initial Account Seed (${entity.name})",
                                                    timestamp = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MakeFlowDebug", "Failed to seed onboarding database", e)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
