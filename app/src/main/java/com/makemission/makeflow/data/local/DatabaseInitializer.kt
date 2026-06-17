package com.makemission.makeflow.data.local

import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity

object DatabaseInitializer {

    suspend fun populate(db: AppDatabase) {
        val accountDao = db.accountDao()
        val categoryDao = db.categoryDao()
        val transactionDao = db.transactionDao()

        accountDao.insertAccount(
            AccountEntity(name = "Virtual Card", balance = 24562.00, type = "Virtual")
        )
        accountDao.insertAccount(
            AccountEntity(name = "Savings Account", balance = 15000.00, type = "Savings")
        )

        val housing = CategoryEntity(
            name = "Housing",
            iconName = "Home",
            colorHex = "#E53935",
            monthlyBudget = 2000.00
        )
        val food = CategoryEntity(
            name = "Food & Dining",
            iconName = "ShoppingCart",
            colorHex = "#FB8C00",
            monthlyBudget = 800.00
        )
        val transport = CategoryEntity(
            name = "Transportation",
            iconName = "LocationOn",
            colorHex = "#3949AB",
            monthlyBudget = 300.00
        )
        val entertainment = CategoryEntity(
            name = "Entertainment",
            iconName = "Star",
            colorHex = "#8E24AA",
            monthlyBudget = 400.00
        )
        val electronics = CategoryEntity(
            name = "Electronics",
            iconName = "ShoppingCart",
            colorHex = "#00897B",
            monthlyBudget = 500.00
        )
        val income = CategoryEntity(
            name = "Income",
            iconName = "Check",
            colorHex = "#43A047",
            monthlyBudget = 0.0
        )

        categoryDao.insertCategory(housing)
        categoryDao.insertCategory(food)
        categoryDao.insertCategory(transport)
        categoryDao.insertCategory(entertainment)
        categoryDao.insertCategory(electronics)
        categoryDao.insertCategory(income)

        val now = System.currentTimeMillis()
        transactionDao.insertTransaction(
            TransactionEntity(amount = 4250.00, type = "INCOME", note = "Salary Deposit", timestamp = now - 3600_000, categoryId = income.id)
        )
        transactionDao.insertTransaction(
            TransactionEntity(amount = 999.00, type = "EXPENSE", note = "Apple Store", timestamp = now - 7200_000, categoryId = electronics.id)
        )
        transactionDao.insertTransaction(
            TransactionEntity(amount = 24.50, type = "EXPENSE", note = "Uber Ride", timestamp = now - 10800_000, categoryId = transport.id)
        )
        transactionDao.insertTransaction(
            TransactionEntity(amount = 15.99, type = "EXPENSE", note = "Netflix", timestamp = now - 14400_000, categoryId = entertainment.id)
        )
        transactionDao.insertTransaction(
            TransactionEntity(amount = 156.78, type = "EXPENSE", note = "Whole Foods", timestamp = now - 18000_000, categoryId = food.id)
        )
    }
}
