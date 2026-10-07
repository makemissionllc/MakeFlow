package com.makemission.makeflow.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

// ── Expressive Easing Engine ─────────────────────────────────────────
object ExpressiveMotion {
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val StandardDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val StandardAccelerate = CubicBezierEasing(0.2f, 0.0f, 1.0f, 1.0f)

    const val StaggerDelayMillis = 36
    const val MaxStaggerIndex = 8
    val StaggerOffset = 12.dp
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
    val colorAnimationSpec: SpringSpec<Color>,
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
            colorAnimationSpec = spring<Color>(
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

// ── Shared Overlay / Screen Transition Specs ──────────────────────────
object OverlayTransition {

    val navigationCrossfade = tween<Float>(
        durationMillis = 360,
        easing = ExpressiveMotion.EmphasizedEasing
    )
    val reducedNavigationCrossfade = tween<Float>(
        durationMillis = 150,
        easing = ExpressiveMotion.StandardDecelerate
    )
    val valueSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val progressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val reducedProgress = tween<Float>(
        durationMillis = 160,
        easing = ExpressiveMotion.StandardDecelerate
    )
    val contentSizeSpring = spring<IntSize>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val reducedContentSize = tween<IntSize>(
        durationMillis = 160,
        easing = ExpressiveMotion.StandardDecelerate
    )
    val reducedColor = tween<Color>(
        durationMillis = 120,
        easing = ExpressiveMotion.StandardDecelerate
    )
    const val TabSlideFraction = 1f / 6f
    const val ValueShiftFraction = 0.16f

    // ── Float specs (fade, scale, progress) ──
    val enterSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
    val exitSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
    val reducedEnter = tween<Float>(durationMillis = 200)
    val reducedExit = tween<Float>(durationMillis = 150)
    val bouncyEnter = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val bouncyExit = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // ── IntOffset specs (slide) ──
    val enterSlide = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
    val exitSlide = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
    val reducedEnterSlide = tween<IntOffset>(durationMillis = 200)
    val reducedExitSlide = tween<IntOffset>(durationMillis = 150)
    val bouncyEnterSlide = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val bouncyExitSlide = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
