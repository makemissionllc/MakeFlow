package com.makemission.makeflow.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Expressive Easing Engine ─────────────────────────────────────────
object ExpressiveMotion {
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val StandardDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val StandardAccelerate = CubicBezierEasing(0.2f, 0.0f, 1.0f, 1.0f)

    val StandardMorph = tween<Float>(durationMillis = 500, easing = EmphasizedEasing)

    fun <T> bouncySpring(
        dampingRatio: Float = Spring.DampingRatioMediumBouncy,
        stiffness: Float = Spring.StiffnessMedium
    ): SpringSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness)
}

// ── Motion Spec for Interactive Components ───────────────────────────
@Immutable
data class ExpressiveMotionSpec(
    val pressScale: Float,
    val releaseScale: Float,
    val longPressScale: Float,
    val scaleAnimationSpec: SpringSpec<Float>,
    val colorAnimationSpec: SpringSpec<Float>,
    val scalePressDown: Float,
    val elevationPressIncrease: Dp,
    val elevationDefault: Dp,
) {
    companion object {
        val Default = ExpressiveMotionSpec(
            pressScale = 0.96f,
            releaseScale = 1f,
            longPressScale = 0.94f,
            scaleAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            colorAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            scalePressDown = 0.96f,
            elevationPressIncrease = 4.dp,
            elevationDefault = 0.dp,
        )
    }
}

val LocalExpressiveMotion = staticCompositionLocalOf { ExpressiveMotionSpec.Default }
