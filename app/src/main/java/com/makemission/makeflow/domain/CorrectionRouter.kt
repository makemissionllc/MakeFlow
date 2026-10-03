package com.makemission.makeflow.domain

enum class CorrectionRoute {
    PART_B_TFLITE,
    PART_C_CORRECTION_MEMORY
}

object CorrectionRouter {
    fun route(correctedCategoryName: String, labelSet: Collection<String>): CorrectionRoute {
        return if (correctedCategoryName in labelSet)
            CorrectionRoute.PART_B_TFLITE
        else
            CorrectionRoute.PART_C_CORRECTION_MEMORY
    }
}
