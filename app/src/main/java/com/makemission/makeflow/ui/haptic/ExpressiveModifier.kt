package com.makemission.makeflow.ui.haptic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.makemission.makeflow.ui.theme.LocalExpressiveMotion

fun Modifier.expressiveClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val motion = LocalExpressiveMotion.current

    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) motion.pressScale else motion.releaseScale,
        animationSpec = motion.scaleAnimationSpec,
        label = "expressiveScale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is PressInteraction.Release -> {
                    isPressed = false
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                is PressInteraction.Cancel -> {
                    isPressed = false
                }
            }
        }
    }

    this
        .scale(scale)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClickLabel = onClickLabel,
            role = role,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        )
}

@Composable
fun Modifier.expressiveBounceAndGlow(
    enabled: Boolean = true,
    glowColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val motion = LocalExpressiveMotion.current

    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) motion.longPressScale else motion.releaseScale,
        animationSpec = motion.scaleAnimationSpec,
        label = "bounceGlowScale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.18f else 0f,
        animationSpec = motion.colorAnimationSpec,
        label = "glowAlpha"
    )

    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is PressInteraction.Release -> {
                    isPressed = false
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                is PressInteraction.Cancel -> {
                    isPressed = false
                }
            }
        }
    }

    this
        .scale(scale)
        .drawBehind {
            if (glowAlpha > 0f) {
                drawRoundRect(
                    color = glowColor.copy(alpha = glowAlpha),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    size = size,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

@Composable
fun Modifier.expressiveLongPressTilt(
    onLongPressStart: (() -> Unit)? = null,
    onLongPressEnd: (() -> Unit)? = null
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val motion = LocalExpressiveMotion.current

    var isLongPressed by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val animRotX by animateFloatAsState(
        targetValue = if (isLongPressed) -dragOffsetY * 0.15f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tiltRotationX"
    )

    val animRotY by animateFloatAsState(
        targetValue = if (isLongPressed) dragOffsetX * 0.15f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tiltRotationY"
    )

    val animElevation by animateDpAsState(
        targetValue = if (isLongPressed) 12.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tiltElevation"
    )

    LaunchedEffect(isLongPressed) {
        if (!isLongPressed) {
            dragOffsetX = 0f
            dragOffsetY = 0f
        }
    }

    this
        .graphicsLayer {
            rotationX = animRotX
            rotationY = animRotY
            shadowElevation = animElevation.toPx()
            cameraDistance = 12f * density
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pressPosition = down.position

                    var isDragging = false
                    var pastLongPressThreshold = false

                    val longPressTimeout = viewConfiguration.longPressTimeoutMillis

                    val startTime = System.currentTimeMillis()

                    while (true) {
                        val event = awaitPointerEvent()
                        val drag = event.changes.firstOrNull()

                        if (drag == null || !drag.pressed) {
                            break
                        }

                        val elapsed = System.currentTimeMillis() - startTime
                        if (elapsed >= longPressTimeout && !pastLongPressThreshold) {
                            pastLongPressThreshold = true
                            isLongPressed = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPressStart?.invoke()
                        }

                        val currentDelta = drag.position - pressPosition
                        if (currentDelta.getDistance() > 10f) {
                            isDragging = true
                        }

                        if (isLongPressed) {
                            dragOffsetX += drag.position.x - pressPosition.x - dragOffsetX
                            dragOffsetY += drag.position.y - pressPosition.y - dragOffsetY
                        }
                    }

                    if (isLongPressed) {
                        isLongPressed = false
                        onLongPressEnd?.invoke()
                    }
                }
            }
        }
}
