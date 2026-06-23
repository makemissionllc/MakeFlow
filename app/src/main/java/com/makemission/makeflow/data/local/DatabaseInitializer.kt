package com.makemission.makeflow.data.local

import com.makemission.makeflow.data.local.entity.CategoryEntity
import java.util.UUID

object DatabaseInitializer {

    suspend fun populate(db: AppDatabase) {
        val categoryDao = db.categoryDao()

        if (categoryDao.getCount() > 0) return

        // ── Expense Parents ──────────────────────────────────────────
        val foodDrinks = CategoryEntity(id = UUID.randomUUID().toString(), name = "Food & Drinks", type = "EXPENSE", parentCategoryId = null, iconName = "local_dining", colorHex = "#FF7043")
        val generosity = CategoryEntity(id = UUID.randomUUID().toString(), name = "Generosity", type = "EXPENSE", parentCategoryId = null, iconName = "volunteer_activism", colorHex = "#EC407A")
        val shopping = CategoryEntity(id = UUID.randomUUID().toString(), name = "Shopping", type = "EXPENSE", parentCategoryId = null, iconName = "shopping_bag", colorHex = "#AB47BC")
        val housing = CategoryEntity(id = UUID.randomUUID().toString(), name = "Housing", type = "EXPENSE", parentCategoryId = null, iconName = "home", colorHex = "#42A5F5")
        val bills = CategoryEntity(id = UUID.randomUUID().toString(), name = "Bills", type = "EXPENSE", parentCategoryId = null, iconName = "receipt_long", colorHex = "#26C6DA")
        val transport = CategoryEntity(id = UUID.randomUUID().toString(), name = "Transport", type = "EXPENSE", parentCategoryId = null, iconName = "directions_transit", colorHex = "#66BB6A")
        val vehicle = CategoryEntity(id = UUID.randomUUID().toString(), name = "Vehicle", type = "EXPENSE", parentCategoryId = null, iconName = "directions_car", colorHex = "#D4E157")
        val leisure = CategoryEntity(id = UUID.randomUUID().toString(), name = "Leisure", type = "EXPENSE", parentCategoryId = null, iconName = "celebration", colorHex = "#FFA726")
        val otherExpense = CategoryEntity(id = UUID.randomUUID().toString(), name = "Other", type = "EXPENSE", parentCategoryId = null, iconName = "more_horiz", colorHex = "#9E9E9E")

        categoryDao.insertCategory(foodDrinks)
        categoryDao.insertCategory(generosity)
        categoryDao.insertCategory(shopping)
        categoryDao.insertCategory(housing)
        categoryDao.insertCategory(bills)
        categoryDao.insertCategory(transport)
        categoryDao.insertCategory(vehicle)
        categoryDao.insertCategory(leisure)
        categoryDao.insertCategory(otherExpense)

        // ── Expense Children ────────────────────────────────────────
        categoryDao.insertCategory(CategoryEntity(name = "Restaurant", type = "EXPENSE", parentCategoryId = foodDrinks.id, iconName = "restaurant", colorHex = "#FF8A65"))
        categoryDao.insertCategory(CategoryEntity(name = "Delivery", type = "EXPENSE", parentCategoryId = foodDrinks.id, iconName = "delivery_dining", colorHex = "#FFA726"))
        categoryDao.insertCategory(CategoryEntity(name = "Snacks", type = "EXPENSE", parentCategoryId = foodDrinks.id, iconName = "lunch_dining", colorHex = "#FFCC02"))

        categoryDao.insertCategory(CategoryEntity(name = "Gifts", type = "EXPENSE", parentCategoryId = generosity.id, iconName = "card_giftcard", colorHex = "#F06292"))

        categoryDao.insertCategory(CategoryEntity(name = "Groceries", type = "EXPENSE", parentCategoryId = shopping.id, iconName = "local_grocery_store", colorHex = "#BA68C8"))
        categoryDao.insertCategory(CategoryEntity(name = "Clothing", type = "EXPENSE", parentCategoryId = shopping.id, iconName = "checkroom", colorHex = "#CE93D8"))
        categoryDao.insertCategory(CategoryEntity(name = "Electronics", type = "EXPENSE", parentCategoryId = shopping.id, iconName = "devices", colorHex = "#7E57C2"))
        categoryDao.insertCategory(CategoryEntity(name = "Kids", type = "EXPENSE", parentCategoryId = shopping.id, iconName = "child_care", colorHex = "#9575CD"))
        categoryDao.insertCategory(CategoryEntity(name = "Pets", type = "EXPENSE", parentCategoryId = shopping.id, iconName = "pets", colorHex = "#80CBC4"))

        categoryDao.insertCategory(CategoryEntity(name = "Utilities", type = "EXPENSE", parentCategoryId = housing.id, iconName = "electrical_services", colorHex = "#64B5F6"))
        categoryDao.insertCategory(CategoryEntity(name = "Mortgage", type = "EXPENSE", parentCategoryId = housing.id, iconName = "account_balance", colorHex = "#1E88E5"))
        categoryDao.insertCategory(CategoryEntity(name = "Rent", type = "EXPENSE", parentCategoryId = housing.id, iconName = "key", colorHex = "#1565C0"))
        categoryDao.insertCategory(CategoryEntity(name = "Maintenance", type = "EXPENSE", parentCategoryId = housing.id, iconName = "build", colorHex = "#0288D1"))

        categoryDao.insertCategory(CategoryEntity(name = "Phone", type = "EXPENSE", parentCategoryId = bills.id, iconName = "phone_android", colorHex = "#4DD0E1"))
        categoryDao.insertCategory(CategoryEntity(name = "Internet", type = "EXPENSE", parentCategoryId = bills.id, iconName = "wifi", colorHex = "#00ACC1"))
        categoryDao.insertCategory(CategoryEntity(name = "Subscriptions", type = "EXPENSE", parentCategoryId = bills.id, iconName = "subscriptions", colorHex = "#00838F"))
        categoryDao.insertCategory(CategoryEntity(name = "Healthcare", type = "EXPENSE", parentCategoryId = bills.id, iconName = "local_hospital", colorHex = "#00897B"))
        categoryDao.insertCategory(CategoryEntity(name = "Taxes", type = "EXPENSE", parentCategoryId = bills.id, iconName = "account_balance_wallet", colorHex = "#558B2F"))

        categoryDao.insertCategory(CategoryEntity(name = "Public", type = "EXPENSE", parentCategoryId = transport.id, iconName = "directions_bus", colorHex = "#81C784"))
        categoryDao.insertCategory(CategoryEntity(name = "Private", type = "EXPENSE", parentCategoryId = transport.id, iconName = "directions_car", colorHex = "#A5D6A7"))
        categoryDao.insertCategory(CategoryEntity(name = "Long Distance", type = "EXPENSE", parentCategoryId = transport.id, iconName = "flight", colorHex = "#43A047"))

        categoryDao.insertCategory(CategoryEntity(name = "Fuel", type = "EXPENSE", parentCategoryId = vehicle.id, iconName = "local_gas_station", colorHex = "#CDDC39"))
        categoryDao.insertCategory(CategoryEntity(name = "Parking", type = "EXPENSE", parentCategoryId = vehicle.id, iconName = "local_parking", colorHex = "#AFB42B"))
        categoryDao.insertCategory(CategoryEntity(name = "Maintenance", type = "EXPENSE", parentCategoryId = vehicle.id, iconName = "build_circle", colorHex = "#F9A825"))
        categoryDao.insertCategory(CategoryEntity(name = "Insurance", type = "EXPENSE", parentCategoryId = vehicle.id, iconName = "security", colorHex = "#FF8F00"))

        categoryDao.insertCategory(CategoryEntity(name = "Entertainment", type = "EXPENSE", parentCategoryId = leisure.id, iconName = "movie", colorHex = "#FFB74D"))
        categoryDao.insertCategory(CategoryEntity(name = "Sports", type = "EXPENSE", parentCategoryId = leisure.id, iconName = "sports_soccer", colorHex = "#FF7043"))
        categoryDao.insertCategory(CategoryEntity(name = "Hobbies", type = "EXPENSE", parentCategoryId = leisure.id, iconName = "palette", colorHex = "#8D6E63"))
        categoryDao.insertCategory(CategoryEntity(name = "Vacations", type = "EXPENSE", parentCategoryId = leisure.id, iconName = "beach_access", colorHex = "#78909C"))

        // ── Income Parents ──────────────────────────────────────────
        val salary = CategoryEntity(name = "Salary", type = "INCOME", parentCategoryId = null, iconName = "work", colorHex = "#4CAF50")
        val interest = CategoryEntity(name = "Interest", type = "INCOME", parentCategoryId = null, iconName = "trending_up", colorHex = "#26A69A")
        val giftsIncome = CategoryEntity(name = "Gifts", type = "INCOME", parentCategoryId = null, iconName = "card_giftcard", colorHex = "#EC407A")
        val otherIncome = CategoryEntity(name = "Other", type = "INCOME", parentCategoryId = null, iconName = "more_horiz", colorHex = "#9E9E9E")

        categoryDao.insertCategory(salary)
        categoryDao.insertCategory(interest)
        categoryDao.insertCategory(giftsIncome)
        categoryDao.insertCategory(otherIncome)
    }
}
