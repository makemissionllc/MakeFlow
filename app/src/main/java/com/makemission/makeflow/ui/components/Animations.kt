package com.makemission.makeflow.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.ui.theme.ExpressiveMotion
import com.makemission.makeflow.ui.theme.ExpressiveMotionSpec
import com.makemission.makeflow.ui.theme.LocalExpressiveMotion
import com.makemission.makeflow.ui.theme.OverlayTransition
import kotlinx.coroutines.delay

@Composable
fun StaggeredItem(
    index: Int,
    delayPerItem: Int = ExpressiveMotion.StaggerDelayMillis,
    offsetY: Dp = ExpressiveMotion.StaggerOffset,
    content: @Composable () -> Unit
) {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced, index) {
        if (reduced) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            delay((index.coerceIn(0, ExpressiveMotion.MaxStaggerIndex) * delayPerItem).toLong())
            progress.animateTo(
                targetValue = 1f,
                animationSpec = OverlayTransition.enterSpring
            )
        }
    }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress.value
            translationY = offsetY.toPx() * (1f - progress.value.coerceIn(0f, 1f))
        }
    ) {
        content()
    }
}

@Composable
fun AnimatedCounter(
    targetValue: Double,
    prefix: String = "",
    suffix: String = "",
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    animationSpec: AnimationSpec<Float> = OverlayTransition.valueSpring,
    formatter: (Double) -> String = { String.format("%,.2f", it) }
) {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = if (LocalReducedMotion.current) snap() else animationSpec,
        label = "counter"
    )
    Text(
        text = "$prefix${formatter(animatedValue.toDouble())}$suffix",
        modifier = modifier,
        color = color,
        style = style
    )
}

fun Modifier.pressScale(
    interactionSource: MutableInteractionSource? = null,
    motionSpec: ExpressiveMotionSpec? = null
): Modifier = composed {
    val source = remember { interactionSource ?: MutableInteractionSource() }
    val resolvedMotion = motionSpec ?: LocalExpressiveMotion.current
    val isPressed by source.collectIsPressedAsState()
    val reduced = LocalReducedMotion.current
    val targetScale by animateFloatAsState(
        targetValue = if (!reduced && isPressed) resolvedMotion.pressScale else resolvedMotion.releaseScale,
        animationSpec = if (reduced) snap() else resolvedMotion.scaleAnimationSpec,
        label = "pressScale"
    )
    scale(scaleX = targetScale, scaleY = targetScale)
}

@Composable
fun AnimatedDoodle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    doodleSize: Dp = 76.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                animationSpec = OverlayTransition.bouncyEnter
            )
        }
    }
    Box(modifier = modifier.size(doodleSize), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(Modifier.size(doodleSize)) {
            val p = progress.value.coerceIn(0f, 1f)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val unit = size.minDimension / 76f
            val stroke = 2.dp.toPx()
            drawArc(
                color = tint.copy(alpha = 0.26f * p),
                startAngle = 206f,
                sweepAngle = 292f * p,
                useCenter = false,
                topLeft = Offset(cx - 27f * unit, cy - 27f * unit),
                size = Size(54f * unit, 54f * unit),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            val rays = listOf(
                Offset(cx - 30f * unit, cy - 24f * unit) to Offset(cx - 36f * unit, cy - 30f * unit),
                Offset(cx + 28f * unit, cy - 27f * unit) to Offset(cx + 35f * unit, cy - 34f * unit),
                Offset(cx + 31f * unit, cy + 23f * unit) to Offset(cx + 37f * unit, cy + 28f * unit),
                Offset(cx - 29f * unit, cy + 24f * unit) to Offset(cx - 35f * unit, cy + 31f * unit)
            )
            rays.forEach { (from, to) ->
                val delta = to - from
                drawLine(
                    color = tint.copy(alpha = 0.68f * p),
                    start = from,
                    end = from + delta * p,
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
        val iconSize = doodleSize * (38f / 76f)
        val scale = if (reduced) 1f else 0.92f + 0.08f * progress.value
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = 0.78f),
            modifier = Modifier.size(iconSize).scale(scale)
        )
    }
}

@Composable
fun AnimatedProgressBar(
    progress: Float,
    color: Color,
    trackColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = OverlayTransition.progressSpring
) {
    val reduced = LocalReducedMotion.current
    val spec = if (reduced) OverlayTransition.reducedProgress else animationSpec
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spec,
        label = "progress"
    )
    val resolvedTrackColor = if (trackColor != Color.Unspecified) trackColor else MaterialTheme.colorScheme.surfaceContainerHigh
    LinearProgressIndicator(
        progress = { animatedProgress },
        color = color,
        trackColor = resolvedTrackColor,
        modifier = modifier
    )
}

@Composable
fun Modifier.animateAppContentSize(): Modifier {
    val reduced = LocalReducedMotion.current
    return animateContentSize(
        animationSpec = if (reduced) {
            OverlayTransition.reducedContentSize
        } else {
            OverlayTransition.contentSizeSpring
        }
    )
}
