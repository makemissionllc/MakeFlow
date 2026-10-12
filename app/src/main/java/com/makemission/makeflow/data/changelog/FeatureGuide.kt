package com.makemission.makeflow.data.changelog

data class FeatureGuideEntry(val title: String, val features: List<String>)

val featureGuideEntries = listOf(
    FeatureGuideEntry("Accounts and currencies", listOf(
        "Create and edit accounts with individual currencies and opening balances. Search by name or currency code; clearing search restores the list without changing net worth.",
        "Net worth converts balances to your default currency using available daily reference rates. Accounts with missing conversions are disclosed and excluded."
    )),
    FeatureGuideEntry("Transaction entry", listOf(
        "Record income, expenses, and transfers with the calculator, category, account, note, and date controls. Duplicate and unusual-amount checks help review entries.",
        "Use Log again in the editor to reuse an entry with today's date. Transfers use current available rates. Leaving an unfinished editor or quick-add form offers Keep editing or Discard."
    )),
    FeatureGuideEntry("History and summaries", listOf(
        "Search notes, amounts, accounts, categories, and references. Combine search with date presets, type, and account focus. Account focus is shared with Home; choose All accounts to clear it.",
        "Open Summary for matching income, spending, and net change grouped by currency. Transfers and incomplete records are disclosed separately. Swipe an entry to delete it; the brief Undo action restores it."
    )),
    FeatureGuideEntry("Tablet workspaces", listOf(
        "In expanded History, select an entry for side-by-side details. Previous and Next review filtered results; Page Up and Page Down work while the details pane is focused.",
        "In expanded Accounts, select an account for its live balance and latest 20 transactions. Transfer in and Transfer out use that account's own amount; unavailable amounts are labelled.",
        "Scroll details independently, select notes and references to copy, and open Edit from the pane. Selection survives resizing and saved-state restoration."
    )),
    FeatureGuideEntry("Budgets and savings goals", listOf(
        "Set category budgets, track spending progress, and review suggestions and spending alerts.",
        "Create savings goals, link accounts, and review feasibility estimates and spending trade-offs. Estimates depend on your recorded activity."
    )),
    FeatureGuideEntry("Recurring payments and reminders", listOf(
        "Create recurring income or expenses, edit schedules, and pause or resume rules. Due entries are processed by the app.",
        "Enable optional weekly finance-review reminders in Settings. Notification permission is required; reminders can be disabled again."
    )),
    FeatureGuideEntry("M-PESA and on-device assistance", listOf(
        "Share or paste M-PESA messages for review before saving. Community builds also offer an opt-in new-message listener after SMS permission and disclosure; the Play build does not include it.",
        "Category suggestions use history, corrections, and on-device classification. Supported devices may offer optional Gemini Nano message suggestions, receipt hints, and monthly reflections. Review suggestions before use."
    )),
    FeatureGuideEntry("Insights, reviews, and exports", listOf(
        "Explore charts, trends, forecasts, and cash-flow estimates in Insights. The shareable monthly finance review covers income, spending, top categories, and month-over-month context.",
        "Export date-range PDF statements or CSV spreadsheets from Settings. Files have readable names, save on your device, and open Android's share menu. Currencies and transfer amounts stay explicit."
    )),
    FeatureGuideEntry("Widgets and quick access", listOf(
        "Add snapshot, quick-action, balance, monthly-summary, quick-expense, or quick-income widgets from your launcher's widget picker. Sizes depend on the widget and launcher.",
        "Quick-add widgets and launcher shortcuts open entry directly. Setup offers a one-time widget-pin request on supported launchers."
    )),
    FeatureGuideEntry("Privacy, backup, and preferences", listOf(
        "Finance records are stored locally. JSON backup and restore are user initiated; keep backup and shared statement files private.",
        "Settings includes app lock, light/dark and AMOLED modes, haptic strength, privacy information, and What's New. Setup allows an optional name and skippable tour; motion respects reduced-motion preferences. Some features need permissions or supported device services."
    ))
)
