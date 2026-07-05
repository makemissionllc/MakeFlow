package com.makemission.makeflow.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.ui.theme.ExpressiveMotionSpec
import kotlinx.coroutines.delay

@Composable
fun StaggeredItem(
    index: Int,
    delayPerItem: Int = 50,
    offsetY: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            delay((index * delayPerItem).toLong())
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
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
    animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    ),
    formatter: (Double) -> String = { String.format("%,.2f", it) }
) {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = animationSpec,
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
    motionSpec: ExpressiveMotionSpec = ExpressiveMotionSpec.Default
): Modifier = composed {
    val source = remember { interactionSource ?: MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val targetScale by animateFloatAsState(
        targetValue = if (isPressed) motionSpec.pressScale else motionSpec.releaseScale,
        animationSpec = motionSpec.scaleAnimationSpec,
        label = "pressScale"
    )
    scale(scaleX = targetScale, scaleY = targetScale)
}

@Composable
fun AnimatedProgressBar(
    progress: Float,
    color: Color,
    trackColor: Color = Color(0xFF2C2C2C),
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
) {
    val reduced = LocalReducedMotion.current
    val spec = if (reduced) tween<Float>(150) else animationSpec
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spec,
        label = "progress"
    )
    LinearProgressIndicator(
        progress = { animatedProgress },
        color = color,
        trackColor = trackColor,
        modifier = modifier
    )
}
