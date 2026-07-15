package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.SavingsGoalEntity

data class SavingsGoalWithProgress(
    val goal: SavingsGoalEntity,
    val progress: Double,
    val percentage: Float
)
