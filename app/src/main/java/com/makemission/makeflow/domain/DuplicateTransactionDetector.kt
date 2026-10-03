package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity
import kotlin.math.abs
import kotlin.math.roundToLong

data class DuplicateCheckResult(
    val matchingTransaction: TransactionEntity,
    val timeDeltaMinutes: Long,
    val reason: String
)

object DuplicateTransactionDetector {
    private const val DUPLICATE_TIME_WINDOW_MINUTES = 5L

    fun check(
        candidate: TransactionEntity,
        recentTransactions: List<TransactionEntity>
    ): DuplicateCheckResult? {
        for (existing in recentTransactions) {
            if (existing.id == candidate.id) continue

            val timeDelta = abs(candidate.timestamp - existing.timestamp)
            val timeDeltaMinutes = (timeDelta / 60_000.0).roundToLong()
            if (timeDeltaMinutes > DUPLICATE_TIME_WINDOW_MINUTES) continue

            if (candidate.amount != existing.amount) continue

            if (candidate.type == "TRANSFER" && existing.type == "TRANSFER") {
                if (candidate.fromAccountId != existing.fromAccountId) continue
                if (candidate.toAccountId != existing.toAccountId) continue
            } else {
                if (candidate.type != existing.type) continue
                if (candidate.accountId != existing.accountId) continue
            }

            val minutes = if (timeDeltaMinutes < 1) 1 else timeDeltaMinutes
            val why = buildString {
                append("duplicate of the ${formatAmount(candidate.amount)} ")
                append(displayType(candidate.type))
                if (existing.note.isNotBlank()) {
                    append(" \"${existing.note}\"")
                }
                append(" you logged $minutes minute${if (minutes > 1) "s" else ""} ago")
            }

            return DuplicateCheckResult(
                matchingTransaction = existing,
                timeDeltaMinutes = minutes,
                reason = why
            )
        }

        return null
    }

    private fun formatAmount(amount: Double): String {
        val whole = amount.toLong()
        val cents = ((amount - whole) * 100).roundToLong()
        return if (cents == 0L) "$$whole" else "$$whole.${cents.toString().padStart(2, '0')}"
    }

    private fun displayType(type: String): String = when (type) {
        "TRANSFER" -> "transfer"
        "INCOME" -> "deposit"
        "EXPENSE" -> "payment"
        else -> "transaction"
    }
}
