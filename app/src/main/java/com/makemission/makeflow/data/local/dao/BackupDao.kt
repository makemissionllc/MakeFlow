package com.makemission.makeflow.data.local.dao

import androidx.room.Transaction
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.data.local.entity.RecurringPatternEntity
import com.makemission.makeflow.data.local.entity.RecurringTransactionEntity
import com.makemission.makeflow.data.local.entity.TaskEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.local.entity.TransactionTemplateEntity

data class BackupBundle(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val tasks: List<TaskEntity>,
    val recurringPatterns: List<RecurringPatternEntity>,
    val recurringTransactions: List<RecurringTransactionEntity>,
    val transactionTemplates: List<TransactionTemplateEntity>,
    val exchangeRates: List<ExchangeRateEntity>
)
