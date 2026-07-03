package com.makemission.makeflow.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AnomalyDetectorTest {

    private val stableHistory = listOf(100.0, 110.0, 90.0, 105.0, 95.0)

    @Test
    fun insufficientHistory_returnsNone() {
        val small = listOf(100.0, 110.0, 90.0, 95.0)
        val result = AnomalyDetector.analyze(500.0, small)
        assertEquals(AnomalySeverity.NONE, result.severity)
        assertEquals(false, result.isAnomalous)
    }

    @Test
    fun highSeverityOutlier_detectedCorrectly() {
        val result = AnomalyDetector.analyze(500.0, stableHistory)
        assertEquals(AnomalySeverity.HIGH, result.severity)
        assertEquals(true, result.isAnomalous)
        assertEquals(100.0, result.categoryAverage, 0.001)
    }

    @Test
    fun moderateSeverity_boundary() {
        val boundaryHistory = listOf(100.0, 100.0, 100.0, 100.0, 100.0)
        val result = AnomalyDetector.analyze(200.0, boundaryHistory)
        assertEquals(AnomalySeverity.MODERATE, result.severity)
        assertEquals(true, result.isAnomalous)
    }

    @Test
    fun lowAmount_notFlagged() {
        val result = AnomalyDetector.analyze(1.0, stableHistory)
        assertEquals(AnomalySeverity.NONE, result.severity)
        assertEquals(false, result.isAnomalous)
    }

    @Test
    fun zeroVarianceHistory_doesNotCrash() {
        val flat = listOf(100.0, 100.0, 100.0, 100.0, 100.0)
        val result = AnomalyDetector.analyze(200.0, flat)
        assertEquals(true, result.isAnomalous)
        assertEquals(AnomalySeverity.MODERATE, result.severity)
    }

    @Test
    fun amountAtMean_returnsNone() {
        val result = AnomalyDetector.analyze(100.0, stableHistory)
        assertEquals(AnomalySeverity.NONE, result.severity)
        assertEquals(false, result.isAnomalous)
    }

    @Test
    fun emptyHistory_returnsNone() {
        val result = AnomalyDetector.analyze(500.0, emptyList())
        assertEquals(AnomalySeverity.NONE, result.severity)
        assertEquals(false, result.isAnomalous)
    }
}
