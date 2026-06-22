package com.makemission.makeflow.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.verticalFadingEdge(
    topFadeHeight: Dp = 48.dp,
    bottomFadeHeight: Dp = 48.dp
): Modifier = this
    .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()

        val topPx = topFadeHeight.toPx().coerceAtMost(size.height / 2f)
        val bottomPx = bottomFadeHeight.toPx().coerceAtMost(size.height / 2f)

        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                topPx / size.height to Color.Black,
                1f - (bottomPx / size.height) to Color.Black,
                1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }
