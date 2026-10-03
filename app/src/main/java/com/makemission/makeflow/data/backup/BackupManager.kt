package com.makemission.makeflow.data.backup

import com.google.gson.GsonBuilder
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.dao.AccountDao
import com.makemission.makeflow.data.local.dao.BudgetDao
import com.makemission.makeflow.data.local.dao.CategoryDao
import com.makemission.makeflow.data.local.dao.ExchangeRateDao
import com.makemission.makeflow.data.local.dao.RecurringPatternDao
import com.makemission.makeflow.data.local.dao.RecurringTransactionDao
import com.makemission.makeflow.data.local.dao.TaskDao
import com.makemission.makeflow.data.local.dao.TransactionDao
import com.makemission.makeflow.data.local.dao.TransactionTemplateDao

class BackupManager(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val taskDao: TaskDao,
    private val recurringPatternDao: RecurringPatternDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val transactionTemplateDao: TransactionTemplateDao,
    private val exchangeRateDao: ExchangeRateDao,
    private val database: AppDatabase
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun serializeBackup(): String {
        val data = BackupData(
            accounts = accountDao.getAllAccountsOnce(),
            categories = categoryDao.getAllCategoriesOnce(),
            transactions = transactionDao.getAllTransactionsOnce(),
            budgets = budgetDao.getAllBudgetsOnce(),
            tasks = taskDao.getAllOnce(),
            recurringPatterns = recurringPatternDao.getAllOnce(),
            recurringTransactions = recurringTransactionDao.getAllOnce(),
            transactionTemplates = transactionTemplateDao.getAllOnce(),
            exchangeRates = exchangeRateDao.getAllRatesOnce()
        )
        return gson.toJson(data)
    }

    fun validateBackup(json: String): ValidationResult {
        val data = try {
            gson.fromJson(json, BackupData::class.java)
        } catch (e: Exception) {
            return ValidationResult(false, "Invalid JSON format: ${e.message}")
        }
        if (data == null) return ValidationResult(false, "Backup file is empty or malformed")
        if (data.schemaVersion != BACKUP_SCHEMA_VERSION) {
            return ValidationResult(
                false,
                "Unsupported backup schema version ${data.schemaVersion}. Expected version $BACKUP_SCHEMA_VERSION."
            )
        }
        return ValidationResult(true, null)
    }

    suspend fun restoreBackup(json: String): String? {
        val validation = validateBackup(json)
        if (!validation.valid) return validation.error

        val data = gson.fromJson(json, BackupData::class.java)
        database.atomicRestore(
            accounts = data.accounts,
            categories = data.categories,
            transactions = data.transactions,
            budgets = data.budgets,
            tasks = data.tasks,
            recurringPatterns = data.recurringPatterns,
            recurringTransactions = data.recurringTransactions,
            transactionTemplates = data.transactionTemplates,
            exchangeRates = data.exchangeRates
        )
        return null
    }

    data class ValidationResult(val valid: Boolean, val error: String?)
}
