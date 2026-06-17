package com.makemission.makeflow.ui.state

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 5,
    val username: String = "",
    val baseCurrency: String = "USD",
    val startingBalance: String = "",
    val activeCategories: Set<String> = defaultActiveCategories,
    val isCompleting: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
) {
    val canContinue: Boolean
        get() = when (currentPage) {
            0 -> username.isNotBlank()
            1 -> baseCurrency.isNotBlank()
            2 -> startingBalance.toDoubleOrNull() != null && (startingBalance.toDoubleOrNull() ?: 0.0) >= 0.0
            3 -> activeCategories.isNotEmpty()
            4 -> true
            else -> false
        }

    companion object {
        val categoryTemplates = listOf(
            CategoryTemplate("Housing", "Home", "#5E35B1"),
            CategoryTemplate("Food & Dining", "ShoppingCart", "#E53935"),
            CategoryTemplate("Transportation", "LocationOn", "#1E88E5"),
            CategoryTemplate("Entertainment", "Star", "#FDD835"),
            CategoryTemplate("Electronics", "ShoppingCart", "#00ACC1"),
            CategoryTemplate("Income", "Check", "#43A047"),
        )

        val defaultActiveCategories = categoryTemplates.map { it.name }.toSet()

        val availableCurrencies = listOf(
            CurrencyInfo("USD", "$", "United States Dollar"),
            CurrencyInfo("EUR", "\u20AC", "Euro"),
            CurrencyInfo("GBP", "\u00A3", "British Pound"),
            CurrencyInfo("KES", "KSh", "Kenyan Shilling"),
            CurrencyInfo("UGX", "USh", "Ugandan Shilling"),
            CurrencyInfo("TZS", "TSh", "Tanzanian Shilling"),
            CurrencyInfo("NGN", "\u20A6", "Nigerian Naira"),
            CurrencyInfo("ZAR", "R", "South African Rand"),
            CurrencyInfo("GHS", "GH\u20B5", "Ghanaian Cedi"),
            CurrencyInfo("XOF", "CFA", "West African CFA"),
        )
    }
}

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String
)

data class CategoryTemplate(
    val name: String,
    val iconName: String,
    val colorHex: String
)
