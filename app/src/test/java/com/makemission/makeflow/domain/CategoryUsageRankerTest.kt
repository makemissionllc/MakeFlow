package com.makemission.makeflow.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryUsageRankerTest {

    private val dayMillis = 24 * 60 * 60 * 1000L
    private val now = 1700000000000L

    private fun usage(categoryId: String, type: String, daysAgo: Int): CategoryUsage {
        return CategoryUsage(categoryId, type, now - (daysAgo.toLong() * dayMillis))
    }

    @Test
    fun `empty input returns empty map`() {
        val result = CategoryUsageRanker.rank(emptyList(), nowMillis = now)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `categories below threshold are excluded`() {
        val usages = listOf(
            usage("cat1", "EXPENSE", 5),
            usage("cat1", "EXPENSE", 10),
            usage("cat2", "EXPENSE", 3)
        )
        val result = CategoryUsageRanker.rank(usages, nowMillis = now, minUsageThreshold = 3)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `categories at threshold are included`() {
        val usages = listOf(
            usage("cat1", "EXPENSE", 1),
            usage("cat1", "EXPENSE", 5),
            usage("cat1", "EXPENSE", 10)
        )
        val result = CategoryUsageRanker.rank(usages, nowMillis = now, minUsageThreshold = 3)
        assertEquals(1, result["EXPENSE"]!!.size)
        assertEquals("cat1", result["EXPENSE"]!![0].categoryId)
        assertEquals(3, result["EXPENSE"]!![0].count)
    }

    @Test
    fun `ranking sorts by frequency descending`() {
        val usages = listOf(
            usage("cat1", "EXPENSE", 1),
            usage("cat1", "EXPENSE", 2),
            usage("cat1", "EXPENSE", 3),
            usage("cat2", "EXPENSE", 4),
            usage("cat2", "EXPENSE", 5),
            usage("cat2", "EXPENSE", 6),
            usage("cat2", "EXPENSE", 7),
            usage("cat2", "EXPENSE", 8)
        )
        val result = CategoryUsageRanker.rank(usages, nowMillis = now, minUsageThreshold = 3)
        assertEquals(2, result["EXPENSE"]!!.size)
        assertEquals("cat2", result["EXPENSE"]!![0].categoryId)
        assertEquals(5, result["EXPENSE"]!![0].count)
        assertEquals("cat1", result["EXPENSE"]!![1].categoryId)
        assertEquals(3, result["EXPENSE"]!![1].count)
    }

    @Test
    fun `income and expense ranked independently`() {
        val usages = listOf(
            usage("inc1", "INCOME", 1),
            usage("inc1", "INCOME", 2),
            usage("inc1", "INCOME", 3),
            usage("exp1", "EXPENSE", 1),
            usage("exp1", "EXPENSE", 2),
            usage("exp1", "EXPENSE", 3),
            usage("exp1", "EXPENSE", 4),
            usage("exp2", "EXPENSE", 5),
            usage("exp2", "EXPENSE", 6),
            usage("exp2", "EXPENSE", 7)
        )
        val result = CategoryUsageRanker.rank(usages, nowMillis = now, minUsageThreshold = 3)
        assertEquals(1, result["INCOME"]!!.size)
        assertEquals("inc1", result["INCOME"]!![0].categoryId)
        assertEquals(2, result["EXPENSE"]!!.size)
        assertEquals("exp1", result["EXPENSE"]!![0].categoryId)
        assertEquals("exp2", result["EXPENSE"]!![1].categoryId)
    }

    @Test
    fun `usage outside time window is ignored`() {
        val usages = listOf(
            usage("cat1", "EXPENSE", 85),
            usage("cat1", "EXPENSE", 88),
            usage("cat1", "EXPENSE", 91),
            usage("cat1", "EXPENSE", 95)
        )
        val result = CategoryUsageRanker.rank(usages, nowMillis = now, timeWindowDays = 90, minUsageThreshold = 3)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `maxResults limits output`() {
        val usages = listOf(
            usage("cat1", "EXPENSE", 1),
            usage("cat1", "EXPENSE", 2),
            usage("cat1", "EXPENSE", 3),
            usage("cat2", "EXPENSE", 4),
            usage("cat2", "EXPENSE", 5),
            usage("cat2", "EXPENSE", 6),
            usage("cat3", "EXPENSE", 7),
            usage("cat3", "EXPENSE", 8),
            usage("cat3", "EXPENSE", 9),
            usage("cat4", "EXPENSE", 10),
            usage("cat4", "EXPENSE", 11),
            usage("cat4", "EXPENSE", 12),
            usage("cat5", "EXPENSE", 13),
            usage("cat5", "EXPENSE", 14),
            usage("cat5", "EXPENSE", 15),
            usage("cat6", "EXPENSE", 16),
            usage("cat6", "EXPENSE", 17),
            usage("cat6", "EXPENSE", 18)
        )
        val result = CategoryUsageRanker.rank(usages, nowMillis = now, maxResults = 5, minUsageThreshold = 3)
        assertEquals(5, result["EXPENSE"]!!.size)
    }

    @Test
    fun `tied scores preserve arbitrary but stable order`() {
        val usages = listOf(
            usage("cat1", "EXPENSE", 1),
            usage("cat1", "EXPENSE", 2),
            usage("cat1", "EXPENSE", 3),
            usage("cat2", "EXPENSE", 4),
            usage("cat2", "EXPENSE", 5),
            usage("cat2", "EXPENSE", 6)
        )
        val result = CategoryUsageRanker.rank(usages, nowMillis = now, minUsageThreshold = 3)
        assertEquals(2, result["EXPENSE"]!!.size)
        assertEquals(3, result["EXPENSE"]!![0].count)
        assertEquals(3, result["EXPENSE"]!![1].count)
    }

    @Test
    fun `category used only once is not surfaced`() {
        val usages = listOf(
            usage("cat1", "EXPENSE", 1),
            usage("cat2", "EXPENSE", 2)
        )
        val result = CategoryUsageRanker.rank(usages, nowMillis = now, minUsageThreshold = 3)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `transfer type categories are ranked like any other`() {
        val usages = listOf(
            usage("transferCat", "TRANSFER", 1),
            usage("transferCat", "TRANSFER", 2),
            usage("transferCat", "TRANSFER", 3),
            usage("transferCat", "TRANSFER", 4)
        )
        val result = CategoryUsageRanker.rank(usages, nowMillis = now, minUsageThreshold = 3)
        assertEquals(1, result["TRANSFER"]!!.size)
        assertEquals("transferCat", result["TRANSFER"]!![0].categoryId)
        assertEquals(4, result["TRANSFER"]!![0].count)
    }

    @Test
    fun `all types outside window returns empty`() {
        val usages = listOf(
            usage("cat1", "EXPENSE", 100),
            usage("cat1", "EXPENSE", 101),
            usage("cat1", "EXPENSE", 102)
        )
        val result = CategoryUsageRanker.rank(usages, nowMillis = now, timeWindowDays = 90, minUsageThreshold = 3)
        assertTrue(result.isEmpty())
    }
}
