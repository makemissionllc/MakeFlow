package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MpesaFeeMatcherTest {
    @Test fun mapsLoggedParserFeeToItsOriginalReference() {
        val fee = TransactionEntity(
            id = "fee", amount = 22.0, type = "EXPENSE", accountId = "mpesa",
            note = "M-PESA transaction cost for ABC123", externalReference = "ABC123-fee"
        )
        val original = TransactionEntity(
            id = "main", amount = 500.0, type = "EXPENSE", accountId = "mpesa",
            externalReference = "ABC123"
        )

        assertEquals(mapOf("ABC123" to fee), MpesaFeeMatcher.byOriginalReference(listOf(original, fee)))
    }

    @Test fun ignoresRegularExpensesTransfersAndEmptyReferences() {
        val regular = TransactionEntity(amount = 4.0, type = "EXPENSE", externalReference = "receipt-fee")
        val transfer = TransactionEntity(amount = 4.0, type = "TRANSFER", externalReference = "ABC123-fee")
        val emptyReference = TransactionEntity(amount = 4.0, type = "EXPENSE", externalReference = "-fee")

        assertEquals(emptyList<TransactionEntity>(), MpesaFeeMatcher.feeTransactions(listOf(regular, transfer, emptyReference)))
    }
}
