package com.makemission.makeflow.data.backup

import org.junit.Assert.*
import org.junit.Test

class BackupRestoreContractTest {

    /**
     * User-data tables that MUST appear in BackupData (and hence in backup/restore).
     * These contain user-created content that must survive backup/restore.
     */
    private val requiredBackupFields = setOf(
        "schemaVersion", "exportedAt",
        "accounts", "categories", "transactions",
        "budgets", "tasks", "recurringPatterns",
        "recurringTransactions", "transactionTemplates", "exchangeRates"
    )

    /**
     * Transient device-local tables deliberately excluded from backup/restore.
     *
     * - cardInteractions: clickstream analytics for DashboardCardRanker (dashboard card
     *   personalization). Device-local usage patterns; restoring stale data would not
     *   reflect actual behavior on a new device. Has 90-day TTL purge.
     *
     * - queuedNotifications: bounded DND notification queue. Time-sensitive pending
     *   alerts; restoring stale entries would fire outdated notifications.
     */
    private val excludedTransientFields = setOf(
        "cardInteractions", "queuedNotifications"
    )

    @Test
    fun backupData_contains_all_required_fields() {
        val actualFields = BackupData::class.java.declaredFields.map { it.name }.toSet()
        for (field in requiredBackupFields) {
            assertTrue(
                "BackupData must include field '$field' — it is required for backup/restore of user data",
                field in actualFields
            )
        }
    }

    @Test
    fun backupData_excludes_transient_state_tables() {
        val actualFields = BackupData::class.java.declaredFields.map { it.name }.toSet()
        for (field in excludedTransientFields) {
            assertFalse(
                "BackupData must NOT include field '$field' — it represents transient " +
                        "device-local state that should not survive backup/restore " +
                        "(see atomicRestore() documentation for rationale)",
                field in actualFields
            )
        }
    }
}