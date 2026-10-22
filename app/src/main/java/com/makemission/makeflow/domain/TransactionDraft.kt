package com.makemission.makeflow.domain

import java.nio.charset.StandardCharsets
import java.util.Base64

/** A device-local, uncommitted transaction that can be restored after the app is closed. */
data class TransactionDraft(
    val type: String,
    val accountId: String,
    val categoryId: String?,
    val note: String,
    val timestamp: Long,
    val fromAccountId: String,
    val toAccountId: String,
    val calculator: CalculatorEngine.Snapshot,
    val confirmedAmount: Double?
) {
    fun encode(): String = listOf(
        SCHEMA_VERSION,
        type,
        accountId,
        categoryId,
        note,
        timestamp.toString(),
        fromAccountId,
        toAccountId,
        calculator.displayValue,
        calculator.pendingOperator?.name,
        calculator.storedOperand.toString(),
        calculator.waitingForOperand.toString(),
        calculator.hasError.toString(),
        calculator.expression,
        confirmedAmount?.toString()
    ).mapIndexed { index, value ->
        if (index == 0) SCHEMA_VERSION else value?.let(::encodeField) ?: NULL_FIELD
    }.joinToString(FIELD_SEPARATOR)

    companion object {
        private const val SCHEMA_VERSION = "v1"
        private const val FIELD_SEPARATOR = "."
        private const val NULL_FIELD = "~"
        private const val MAX_ENCODED_LENGTH = 32_768
        private const val MAX_NOTE_LENGTH = 4_096

        fun decode(encoded: String?): TransactionDraft? {
            if (encoded.isNullOrBlank() || encoded.length > MAX_ENCODED_LENGTH) return null
            val fields = encoded.split(FIELD_SEPARATOR)
            if (fields.size != 15 || fields[0] != SCHEMA_VERSION) return null
            val values = fields.drop(1).map { field ->
                if (field == NULL_FIELD) null else decodeField(field) ?: return null
            }
            val type = values[0] ?: return null
            if (type !in setOf("INCOME", "EXPENSE", "TRANSFER")) return null
            val accountId = values[1] ?: return null
            val note = values[3] ?: return null
            if (note.length > MAX_NOTE_LENGTH) return null
            val timestamp = values[4]?.toLongOrNull()?.takeIf { it >= 0L } ?: return null
            val fromAccountId = values[5] ?: return null
            val toAccountId = values[6] ?: return null
            val displayValue = values[7] ?: return null
            val hasError = values[11]?.toBooleanStrictOrNull() ?: return null
            if (if (hasError) displayValue != "Error" else displayValue.toDoubleOrNull() == null) return null
            val operator = values[8]?.let { name ->
                CalculatorEngine.Operator.entries.firstOrNull { it.name == name } ?: return null
            }
            val storedOperand = values[9]?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
            val waitingForOperand = values[10]?.toBooleanStrictOrNull() ?: return null
            val confirmedAmount = values[13]?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
            if (values[13] != null && confirmedAmount == null) return null

            return TransactionDraft(
                type = type,
                accountId = accountId,
                categoryId = values[2],
                note = note,
                timestamp = timestamp,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                calculator = CalculatorEngine.Snapshot(
                    displayValue = displayValue,
                    pendingOperator = operator,
                    storedOperand = storedOperand,
                    waitingForOperand = waitingForOperand,
                    hasError = hasError,
                    expression = values[12]
                ),
                confirmedAmount = confirmedAmount
            )
        }

        private fun encodeField(value: String): String = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        private fun decodeField(value: String): String? = runCatching {
            String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
        }.getOrNull()
    }
}
