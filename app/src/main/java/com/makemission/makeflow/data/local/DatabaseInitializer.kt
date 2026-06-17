package com.makemission.makeflow.data.local

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity

object DatabaseInitializer {

    suspend fun populate(db: AppDatabase) {
        val accountDao = db.accountDao()
        val categoryDao = db.categoryDao()
        val transactionDao = db.transactionDao()

        val virtualCardId = accountDao.insertAccount(
            AccountEntity(name = "Virtual Card", balance = 24562.00, type = "Virtual")
        )
        accountDao.insertAccount(
            AccountEntity(name = "Savings Account", balance = 15000.00, type = "Savings")
        )

        val housingId = categoryDao.insertCategory(
            CategoryEntity(name = "Housing", budgetLimit = 2000.00)
        )
        val foodId = categoryDao.insertCategory(
            CategoryEntity(name = "Food & Dining", budgetLimit = 800.00)
        )
        val transportId = categoryDao.insertCategory(
            CategoryEntity(name = "Transportation", budgetLimit = 300.00)
        )
        val entertainmentId = categoryDao.insertCategory(
            CategoryEntity(name = "Entertainment", budgetLimit = 400.00)
        )
        val electronicsId = categoryDao.insertCategory(
            CategoryEntity(name = "Electronics", budgetLimit = 500.00)
        )
        val incomeId = categoryDao.insertCategory(
            CategoryEntity(name = "Income", budgetLimit = 0.0)
        )

        val now = System.currentTimeMillis()
        transactionDao.insertTransaction(
            TransactionEntity(amount = 4250.00, type = "INCOME", note = "Salary Deposit", timestamp = now - 3600_000, accountId = virtualCardId, categoryId = incomeId)
        )
        transactionDao.insertTransaction(
            TransactionEntity(amount = 999.00, type = "EXPENSE", note = "Apple Store", timestamp = now - 7200_000, accountId = virtualCardId, categoryId = electronicsId)
        )
        transactionDao.insertTransaction(
            TransactionEntity(amount = 24.50, type = "EXPENSE", note = "Uber Ride", timestamp = now - 10800_000, accountId = virtualCardId, categoryId = transportId)
        )
        transactionDao.insertTransaction(
            TransactionEntity(amount = 15.99, type = "EXPENSE", note = "Netflix", timestamp = now - 14400_000, accountId = virtualCardId, categoryId = entertainmentId)
        )
        transactionDao.insertTransaction(
            TransactionEntity(amount = 156.78, type = "EXPENSE", note = "Whole Foods", timestamp = now - 18000_000, accountId = virtualCardId, categoryId = foodId)
        )
    }
}
