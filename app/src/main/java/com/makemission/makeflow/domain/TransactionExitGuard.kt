package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity

object TransactionExitGuard {
    data class FormState(
        val amountInput: String,
        val type: String,
        val categoryId: String?,
        val accountId: String,
        val note: String,
        val timestamp: Long,
        val fromAccountId: String? = null,
        val toAccountId: String? = null,
        val hasPendingCalculation: Boolean = false
    )

    fun hasUnsavedChanges(current: FormState, original: TransactionEntity? = null): Boolean {
        if (current.hasPendingCalculation) return true
        val amount = normalizeAmount(current.amountInput)
        if (original == null) {
            return amount != "0" || current.note.isNotBlank()
        }
        if (amount != normalizeAmount(original.amount.toString()) ||
            current.type != original.type || current.note != original.note ||
            current.timestamp != original.timestamp
        ) return true

        return if (current.type == "TRANSFER") {
            (current.fromAccountId ?: current.accountId) != (original.fromAccountId ?: original.accountId) ||
                current.toAccountId != original.toAccountId
        } else {
            current.accountId != original.accountId || current.categoryId != original.categoryId
        }
    }

    private fun normalizeAmount(input: String): String {
        if (input.isBlank()) return "0"
        return input.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: input
    }
}
