package com.makemission.makeflow.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CorrectionRouterTest {

    private val labelSet = setOf("Fuel", "Groceries", "Restaurant", "Phone")

    @Test
    fun inVocabularyLabel_routesToPartB() {
        val result = CorrectionRouter.route("Fuel", labelSet)
        assertEquals(CorrectionRoute.PART_B_TFLITE, result)
    }

    @Test
    fun customCategoryLabel_routesToPartC() {
        val result = CorrectionRouter.route("Airtime", labelSet)
        assertEquals(CorrectionRoute.PART_C_CORRECTION_MEMORY, result)
    }

    @Test
    fun emptyLabelSet_alwaysReturnsPartC() {
        val result = CorrectionRouter.route("Fuel", emptySet())
        assertEquals(CorrectionRoute.PART_C_CORRECTION_MEMORY, result)
    }

    @Test
    fun caseSensitive_routing() {
        val result = CorrectionRouter.route("fuel", labelSet)
        assertEquals(CorrectionRoute.PART_C_CORRECTION_MEMORY, result)
    }

    @Test
    fun allLabelsInSet_routeToB() {
        labelSet.forEach { label ->
            assertEquals(
                "Expected $label to route to Part B",
                CorrectionRoute.PART_B_TFLITE,
                CorrectionRouter.route(label, labelSet)
            )
        }
    }
}
