package com.makemission.makeflow.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.makemission.makeflow.ui.theme.ExpressiveMotion
import kotlin.math.roundToInt

@Composable
fun ExpressiveLineChart(
    dataPoints: List<Double>,
    modifier: Modifier = Modifier,
    onPointSelected: (index: Int, value: Double) -> Unit = { _, _ -> },
    onScrubCleared: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    var targetProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(dataPoints) {
        targetProgress = 0f
        targetProgress = 1f
    }

    val animationProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = ExpressiveMotion.StandardMorph
    )

    var activeScrubX by remember { mutableStateOf<Float?>(null) }
    var lastSelectedIndex by remember { mutableStateOf(-1) }

    val lineColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
    val guideLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val indicatorColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(surfaceColor, shape = MaterialTheme.shapes.large)
            .padding(vertical = 16.dp, horizontal = 8.dp)
            .pointerInput(dataPoints) {
                detectDragGestures(
                    onDragStart = { offset -> activeScrubX = offset.x },
                    onDragEnd = {
                        activeScrubX = null
                        lastSelectedIndex = -1
                        onScrubCleared()
                    },
                    onDragCancel = {
                        activeScrubX = null
                        lastSelectedIndex = -1
                        onScrubCleared()
                    },
                    onDrag = { change, _ ->
                        activeScrubX = change.position.x
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (dataPoints.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height
            val numPoints = dataPoints.size
            val stepX = width / (numPoints - 1).coerceAtLeast(1)

            val maxRaw = dataPoints.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
            val maxVal = maxRaw * 1.15

            val points = dataPoints.mapIndexed { index, amount ->
                val x = index * stepX
                val normalizedY = (amount / maxVal).toFloat()
                val y = height - (normalizedY * height * animationProgress)
                Offset(x, y)
            }

            if (points.isNotEmpty()) {
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val pPrev = points[i - 1]
                        val pCurr = points[i]
                        val cpX1 = pPrev.x + (pCurr.x - pPrev.x) / 2f
                        cubicTo(cpX1, pPrev.y, cpX1, pCurr.y, pCurr.x, pCurr.y)
                    }
                }

                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            activeScrubX?.let { scrubX ->
                val clampedX = scrubX.coerceIn(0f, width)
                val targetIndex = ((clampedX / width) * (numPoints - 1))
                    .roundToInt()
                    .coerceIn(0, numPoints - 1)

                val activePoint = points[targetIndex]

                if (targetIndex != lastSelectedIndex) {
                    lastSelectedIndex = targetIndex
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPointSelected(targetIndex, dataPoints[targetIndex])
                }

                drawLine(
                    color = guideLineColor,
                    start = Offset(activePoint.x, 0f),
                    end = Offset(activePoint.x, height),
                    strokeWidth = 1.5.dp.toPx()
                )

                drawCircle(
                    color = indicatorColor,
                    radius = 6.dp.toPx(),
                    center = activePoint
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 3.dp.toPx(),
                    center = activePoint
                )
            }
        }
    }
}
