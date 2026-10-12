package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity

object AccountActivity {
    enum class Direction { INCOME, EXPENSE, TRANSFER_IN, TRANSFER_OUT, UNAVAILABLE }

    data class Entry(val transaction: TransactionEntity, val direction: Direction, val amount: Double?)

    fun recent(transactions: List<TransactionEntity>, accountId: String, limit: Int = 20): List<Entry> {
        require(limit > 0)
        return transactions.mapNotNull { transaction ->
            val direction = if (transaction.type == "TRANSFER") {
                val source = transaction.fromAccountId ?: transaction.accountId
                when {
                    source == accountId && transaction.toAccountId == accountId -> Direction.UNAVAILABLE
                    source == accountId -> Direction.TRANSFER_OUT
                    transaction.toAccountId == accountId -> Direction.TRANSFER_IN
                    else -> return@mapNotNull null
                }
            } else {
                if (transaction.accountId != accountId) return@mapNotNull null
                when (transaction.type) {
                    "INCOME" -> Direction.INCOME
                    "EXPENSE" -> Direction.EXPENSE
                    else -> Direction.UNAVAILABLE
                }
            }
            val amount = when (direction) {
                Direction.TRANSFER_IN -> transaction.destinationAmount
                Direction.UNAVAILABLE -> null
                else -> transaction.amount
            }?.takeIf { it.isFinite() && it >= 0.0 }
            Entry(transaction, direction, amount)
        }.sortedWith(compareByDescending<Entry> { it.transaction.timestamp }.thenBy { it.transaction.id }).take(limit)
    }
}
