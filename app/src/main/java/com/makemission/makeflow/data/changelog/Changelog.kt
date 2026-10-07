package com.makemission.makeflow.data.changelog

data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<String>
)

val CURRENT_CHANGELOG_VERSION = "1.0"

val changelogEntries = listOf(
    ChangelogEntry(
        version = "1.0",
        date = "2026-07-16",
        changes = listOf(
            "Track and categorize income & expenses across multiple accounts",
            "Recurring transactions for bills, subscriptions, and regular income",
            "Budget management with category-level spending limits and progress tracking",
            "Savings goals with feasibility guidance based on spending patterns",
            "M-PESA transaction import via share intent with auto-parsing",
            "Smart category suggestions that learn from your corrections over time",
            "Spending insights with interactive charts and configurable time ranges",
            "Multi-currency support: KES, USD, EUR, GBP, UGX, TZS",
            "Data backup and restore via JSON file export/import",
            "App lock with biometric / PIN authentication",
            "Home screen widget for quick account balance glance",
            "A unified ivory, sage, and chartreuse design with light, dark, and AMOLED modes",
            "Dark theme optimized for low-light use",
            "Privacy-first: all data stored securely on your device"
        )
    )
)
