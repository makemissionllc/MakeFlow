package com.makemission.makeflow.data

import com.makemission.makeflow.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringDetectorTest {

    private val dayMs = 24L * 60 * 60 * 1000

    @Test
    fun `detect monthly recurring subscription`() {
        val now = System.currentTimeMillis()
        val txns = listOf(
            TransactionEntity(amount = 14.99, type = "EXPENSE", note = "Netflix", timestamp = now - 90 * dayMs, categoryId = "bills"),
            TransactionEntity(amount = 14.99, type = "EXPENSE", note = "Netflix", timestamp = now - 60 * dayMs, categoryId = "bills"),
            TransactionEntity(amount = 14.99, type = "EXPENSE", note = "Netflix", timestamp = now - 30 * dayMs, categoryId = "bills"),
            TransactionEntity(amount = 14.99, type = "EXPENSE", note = "Netflix", timestamp = now, categoryId = "bills")
        )
        val results = RecurringDetector.detect(txns)
        assertEquals(1, results.size)
        val pattern = results.first()
        assertEquals("netflix", pattern.normalizedDescription)
        assertEquals(14.99, pattern.avgAmount, 0.001)
        assertEquals(30, pattern.intervalDays)
        assertTrue(pattern.confidence > 0.9f)
    }

    @Test
    fun `irregular amounts should not be detected as recurring`() {
        val now = System.currentTimeMillis()
        val txns = listOf(
            TransactionEntity(amount = 12.50, type = "EXPENSE", note = "Amazon", timestamp = now - 90 * dayMs),
            TransactionEntity(amount = 67.30, type = "EXPENSE", note = "Amazon", timestamp = now - 60 * dayMs),
            TransactionEntity(amount = 8.99, type = "EXPENSE", note = "Amazon", timestamp = now - 30 * dayMs),
            TransactionEntity(amount = 129.99, type = "EXPENSE", note = "Amazon", timestamp = now)
        )
        val results = RecurringDetector.detect(txns)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `fewer than 3 occurrences should be excluded`() {
        val txns = listOf(
            TransactionEntity(amount = 9.99, type = "EXPENSE", note = "Spotify", timestamp = 0),
            TransactionEntity(amount = 9.99, type = "EXPENSE", note = "Spotify", timestamp = 30 * dayMs)
        )
        val results = RecurringDetector.detect(txns)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `same-day duplicates should not produce false recurring pattern`() {
        val now = System.currentTimeMillis()
        val txns = listOf(
            TransactionEntity(amount = 5.0, type = "EXPENSE", note = "Coffee", timestamp = now),
            TransactionEntity(amount = 5.0, type = "EXPENSE", note = "Coffee", timestamp = now + 1000),
            TransactionEntity(amount = 5.0, type = "EXPENSE", note = "Coffee", timestamp = now + 2000)
        )
        val results = RecurringDetector.detect(txns)
        assertTrue(results.isEmpty())
    }
}
