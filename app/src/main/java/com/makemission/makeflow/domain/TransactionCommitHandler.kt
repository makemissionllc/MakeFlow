package com.makemission.makeflow.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.data.local.entity.TransactionEntity
import kotlinx.coroutines.delay

class TransactionCommitHandler(
    val onCheckDuplicate: (suspend (TransactionEntity) -> DuplicateCheckResult?)?,
    val onAnomalyCheck: (suspend (Double, String?) -> AnomalyResult?)?,
    val hapticManager: HapticManager
) {
    var anomalyResult by mutableStateOf<AnomalyResult?>(null)
    var duplicateWarning by mutableStateOf<DuplicateCheckResult?>(null)
    var pendingEntity by mutableStateOf<TransactionEntity?>(null)

    /**
     * Attempts to save a new transaction. If a duplicate is detected, the save
     * is deferred and [duplicateWarning] / [pendingEntity] are set. The caller
     * should show a dialog and call [saveIgnoringDuplicate] if the user
     * confirms. If no duplicate, the save proceeds immediately.
     */
    suspend fun prepareSave(
        entity: TransactionEntity,
        onSave: suspend (TransactionEntity) -> Unit,
        onComplete: () -> Unit = {}
    ) {
        if (onCheckDuplicate != null) {
            val result = onCheckDuplicate(entity)
            if (result != null) {
                duplicateWarning = result
                pendingEntity = entity
                return
            }
        }
        saveAndCheckAnomaly(entity, onSave)
        onComplete()
    }

    /**
     * Saves the pending entity (user confirmed "Save Anyway" on duplicate
     * warning) and checks for anomalies on the saved transaction.
     */
    suspend fun saveIgnoringDuplicate(
        entity: TransactionEntity,
        onSave: suspend (TransactionEntity) -> Unit,
        onComplete: () -> Unit = {}
    ) {
        saveAndCheckAnomaly(entity, onSave)
        duplicateWarning = null
        pendingEntity = null
        onComplete()
    }

    /**
     * Updates an existing transaction (edit mode). Skips duplicate check.
     * Only checks anomaly when the amount changed.
     */
    suspend fun update(
        entity: TransactionEntity,
        originalTransaction: TransactionEntity,
        onUpdate: suspend (TransactionEntity) -> Unit
    ) {
        onUpdate(entity)
        hapticManager.fire(HapticEvent.TRANSACTION_SAVED)
        if (entity.type == "EXPENSE" && entity.categoryId != null && onAnomalyCheck != null
            && entity.amount != originalTransaction.amount) {
            val result = onAnomalyCheck(entity.amount, entity.categoryId!!)
            if (result != null && result.severity != AnomalySeverity.NONE) {
                anomalyResult = result
                hapticManager.fire(HapticEvent.ANOMALY_DETECTED)
                delay(2500)
                anomalyResult = null
            }
        }
    }

    private suspend fun saveAndCheckAnomaly(
        entity: TransactionEntity,
        onSave: suspend (TransactionEntity) -> Unit
    ) {
        onSave(entity)
        hapticManager.fire(HapticEvent.TRANSACTION_SAVED)
        if (entity.type == "EXPENSE" && entity.categoryId != null && onAnomalyCheck != null) {
            val result = onAnomalyCheck(entity.amount, entity.categoryId!!)
            if (result != null && result.severity != AnomalySeverity.NONE) {
                anomalyResult = result
                hapticManager.fire(HapticEvent.ANOMALY_DETECTED)
                delay(2500)
                anomalyResult = null
            }
        }
    }

    fun clearAlerts() {
        anomalyResult = null
        duplicateWarning = null
        pendingEntity = null
    }
}
