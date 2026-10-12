package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity

object MpesaFeeMatcher {
    private fun isFeeCandidate(transaction: TransactionEntity): Boolean =
        transaction.type == "EXPENSE" &&
            transaction.externalReference?.endsWith("-fee") == true

    fun feeTransactions(transactions: List<TransactionEntity>): List<TransactionEntity> {
        val originalReferences = transactions.mapNotNull { transaction ->
            transaction.externalReference?.takeIf { it.isNotBlank() && !it.endsWith("-fee") }
        }.toSet()
        return transactions.asSequence()
            .filter(::isFeeCandidate)
            .filter { fee ->
                fee.externalReference?.removeSuffix("-fee")?.takeIf(String::isNotBlank)
                    ?.let(originalReferences::contains) == true
            }
            .toList()
    }

    fun byOriginalReference(transactions: List<TransactionEntity>): Map<String, TransactionEntity> =
        feeTransactions(transactions).asSequence()
            .mapNotNull { fee ->
                fee.externalReference?.removeSuffix("-fee")
                    ?.takeIf(String::isNotBlank)
                    ?.let { originalReference -> originalReference to fee }
            }
            .toMap()
}
