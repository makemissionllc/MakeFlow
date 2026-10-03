package com.makemission.makeflow.data.backup

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.data.local.entity.RecurringPatternEntity
import com.makemission.makeflow.data.local.entity.RecurringTransactionEntity
import com.makemission.makeflow.data.local.entity.TaskEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.local.entity.TransactionTemplateEntity

const val BACKUP_SCHEMA_VERSION = 1

data class BackupData(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val recurringPatterns: List<RecurringPatternEntity> = emptyList(),
    val recurringTransactions: List<RecurringTransactionEntity> = emptyList(),
    val transactionTemplates: List<TransactionTemplateEntity> = emptyList(),
    val exchangeRates: List<ExchangeRateEntity> = emptyList()
    // card_interactions and queued_notifications are intentionally absent:
    // both are transient device-local state (dashboard personalization and
    // DND notification queue) that should not survive backup/restore.
)
