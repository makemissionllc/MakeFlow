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
        version = BuildConfig.VERSION_NAME,
        date = "2026-10-09",
        changes = listOf(
            "Open a summary of the transactions matching your History search and filters",
            "See income, spending, and net change separately for each currency",
            "Keep transfers out of income and spending totals, with incomplete records clearly marked"
        )
    ),
    ChangelogEntry(
        version = "2.0.7-community",
        date = "2026-10-09",
        changes = listOf(
            "Keep editing or discard when leaving an unfinished transaction",
            "Protect quick-add entries from accidental close, back, and swipe-dismiss actions",
            "Open existing transactions without automatically replacing their recorded category"
        )
    ),
    ChangelogEntry(
        version = "2.0.6-community",
        date = "2026-10-09",
        changes = listOf(
            "Choose a date range for CSV exports and share the saved spreadsheet",
            "Export account and category names, currencies, transfer details, and M-PESA references",
            "More reliable CSV saving, with properly escaped notes and Android 9 support"
        )
    ),
    ChangelogEntry(
        version = "2.0.5-community",
        date = "2026-10-09",
        changes = listOf(
            "Filter History by today, the last 7 days, this month, or last month",
            "Combine date ranges with search, transaction type, and the selected account",
            "Clear search and filters together without changing the selected account"
        )
    ),
    ChangelogEntry(
        version = "2.0.4-community",
        date = "2026-10-08",
        changes = listOf(
            "Log a transaction again with its details filled in and today's date",
            "Repeated entries keep duplicate protection and use current transfer exchange rates"
        )
    ),
    ChangelogEntry(
        version = "2.0.3-community",
        date = "2026-10-08",
        changes = listOf(
            "Find transactions quickly by searching notes, amounts, accounts, categories, or references",
            "Clear an empty search and type filter in one tap",
            "Export readable, paginated PDF statements with friendly account and category names",
            "Save statements with a date-based filename and share them from the Android share menu"
        )
    ),
    ChangelogEntry(
        version = "2.0.1-community",
        date = "2026-10-08",
        changes = listOf(
            "A clear Community permission setup for M-PESA review and notifications",
            "More reliable Settings scrolling and haptic choices"
        )
    ),
    ChangelogEntry(
        version = "2.0.0",
        date = "2026-10-07",
        changes = listOf(
            "A four-step onboarding flow with an optional name and a skippable app tour",
            "A one-time offer to pin a Mindful widget after setup",
            "Optional weekly reminders for a finance check-in",
            "A direct link to the public privacy policy"
        )
    ),
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
