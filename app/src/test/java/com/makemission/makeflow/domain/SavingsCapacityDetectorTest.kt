package com.makemission.makeflow.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsCapacityDetectorTest {

    @Test
    fun `less than 3 months returns null`() {
        val result = SavingsCapacityDetector.compute(
            listOf(MonthlySurplus(1000.0, 500.0))
        )
        assertNull(result)
    }

    @Test
    fun `empty list returns null`() {
        val result = SavingsCapacityDetector.compute(emptyList())
        assertNull(result)
    }

    @Test
    fun `negative surplus returns null`() {
        val result = SavingsCapacityDetector.compute(
            listOf(
                MonthlySurplus(1000.0, 500.0),
                MonthlySurplus(1000.0, 400.0),
                MonthlySurplus(500.0, 600.0)
            )
        )
        assertNull(result)
    }

    @Test
    fun `zero surplus returns null`() {
        val result = SavingsCapacityDetector.compute(
            listOf(
                MonthlySurplus(1000.0, 1000.0),
                MonthlySurplus(1000.0, 500.0),
                MonthlySurplus(1000.0, 600.0)
            )
        )
        assertNull(result)
    }

    @Test
    fun `consistent surplus returns suggestion`() {
        val result = SavingsCapacityDetector.compute(
            listOf(
                MonthlySurplus(2000.0, 1000.0),
                MonthlySurplus(2000.0, 1000.0),
                MonthlySurplus(2000.0, 1000.0)
            )
        )
        assertNotNull(result)
        assertEquals(800.0, result!!.suggestedAmount, 0.001)
        assertEquals(1000.0, result.safeFloor, 0.001)
    }

    @Test
    fun `six months with consistent surplus gives max confidence`() {
        val result = SavingsCapacityDetector.compute(
            (1..6).map {
                MonthlySurplus(2000.0, 1000.0)
            }
        )
        assertNotNull(result)
        assertTrue(result!!.confidence > 0.9f)
    }

    @Test
    fun `suggested amount is 80 percent of safe floor`() {
        val result = SavingsCapacityDetector.compute(
            listOf(
                MonthlySurplus(5000.0, 2500.0),
                MonthlySurplus(5000.0, 2500.0),
                MonthlySurplus(5000.0, 2500.0)
            )
        )
        assertNotNull(result)
        assertEquals(2500.0 * 0.8, result!!.suggestedAmount, 0.001)
    }

    @Test
    fun `safe floor is minimum surplus across months`() {
        val result = SavingsCapacityDetector.compute(
            listOf(
                MonthlySurplus(3000.0, 1500.0),
                MonthlySurplus(3000.0, 1000.0),
                MonthlySurplus(3000.0, 1200.0)
            )
        )
        assertNotNull(result)
        assertEquals(1500.0, result!!.safeFloor, 0.001)
    }

    @Test
    fun `high variance reduces confidence`() {
        val stable = SavingsCapacityDetector.compute(
            listOf(
                MonthlySurplus(2000.0, 1000.0),
                MonthlySurplus(2000.0, 1000.0),
                MonthlySurplus(2000.0, 1000.0)
            )
        )
        val volatile = SavingsCapacityDetector.compute(
            listOf(
                MonthlySurplus(10000.0, 2000.0),
                MonthlySurplus(2000.0, 1000.0),
                MonthlySurplus(1500.0, 1400.0)
            )
        )
        assertNotNull(stable)
        assertNotNull(volatile)
        assertTrue(stable!!.confidence > volatile!!.confidence)
    }

    @Test
    fun `monthCount reflects input size`() {
        val data = listOf(
            MonthlySurplus(1000.0, 500.0),
            MonthlySurplus(1000.0, 400.0),
            MonthlySurplus(1000.0, 450.0),
            MonthlySurplus(1000.0, 500.0)
        )
        val result = SavingsCapacityDetector.compute(data)
        assertNotNull(result)
        assertEquals(4, result!!.monthCount)
    }

    @Test
    fun `exactly 3 months returns suggestion`() {
        val result = SavingsCapacityDetector.compute(
            listOf(
                MonthlySurplus(1000.0, 400.0),
                MonthlySurplus(1000.0, 400.0),
                MonthlySurplus(1000.0, 400.0)
            )
        )
        assertNotNull(result)
        assertEquals(3, result!!.monthCount)
    }

    @Test
    fun `max 6 months is accepted`() {
        val result = SavingsCapacityDetector.compute(
            (1..6).map { MonthlySurplus(2000.0, 1000.0) }
        )
        assertNotNull(result)
        assertEquals(6, result!!.monthCount)
    }
}
