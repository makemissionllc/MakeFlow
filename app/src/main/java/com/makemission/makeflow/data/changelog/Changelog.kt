package com.makemission.makeflow.data.changelog

import com.makemission.makeflow.BuildConfig

data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<String>
)

val CURRENT_CHANGELOG_VERSION = BuildConfig.VERSION_NAME

val changelogEntries = listOf(
    ChangelogEntry(
        version = "1.1.0",
        date = "2026-10-07",
        changes = listOf(
            "A simpler first setup: choose a currency and accounts, with an optional name",
            "Choose haptic feedback strength: off, soft, standard, or strong",
            "A clearer privacy view, with automatic device backup turned off",
            "M-PESA messages can still be shared or pasted for review; background SMS detection is not included in the Google Play build"
        )
    ),
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
