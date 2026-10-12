package com.makemission.makeflow.data.changelog

import com.makemission.makeflow.BuildConfig
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureGuideTest {
    @Test fun guideHasUniqueNonemptySectionsAndDescriptions() {
        assertEquals(featureGuideEntries.size, featureGuideEntries.map { it.title }.distinct().size)
        assertTrue(featureGuideEntries.all { it.title.isNotBlank() && it.features.isNotEmpty() })
        assertTrue(featureGuideEntries.flatMap { it.features }.all { it.isNotBlank() })
    }

    @Test fun guideCoversEveryMajorWorkflow() {
        assertEquals(setOf("Accounts and currencies", "Transaction entry", "History and summaries",
            "Tablet workspaces", "Budgets and savings goals", "Tablet budget workspace", "Recurring payments and reminders",
            "M-PESA and on-device assistance", "Insights, reviews, and exports", "Widgets and quick access",
            "Privacy, backup, and preferences"), featureGuideEntries.map { it.title }.toSet())
    }

    @Test fun guideDescribesExpandedWidgetRepeatAndRememberedAccount() {
        val widgetFeatures = featureGuideEntries.first { it.title == "Widgets and quick access" }.features
        val tabletFeatures = featureGuideEntries.first { it.title == "Tablet workspaces" }.features

        assertTrue(widgetFeatures.any { it.contains("last-used account") })
        assertTrue(widgetFeatures.any { it.contains("repeat your most recent income or expense") })
        assertTrue(tabletFeatures.any { it.contains("expanded Quick Action widget") })
    }

    @Test fun releaseNotesRetainUniqueVersionsAndCurrentReleaseFirst() {
        assertEquals(BuildConfig.VERSION_NAME, changelogEntries.first().version)
        assertEquals(changelogEntries.size, changelogEntries.map { it.version }.distinct().size)
        assertTrue(changelogEntries.all { it.changes.isNotEmpty() && it.changes.all(String::isNotBlank) })
        val dates = changelogEntries.map { LocalDate.parse(it.date) }
        assertEquals(dates.sortedDescending(), dates)
    }
}
