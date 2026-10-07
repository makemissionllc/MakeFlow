package com.makemission.makeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makemission.makeflow.ui.theme.LocalAppDimens

@Composable
fun CollapsingTopBar(
    title: String,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val dimens = LocalAppDimens.current
    val config = LocalAdaptiveConfig.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val collapseFraction by remember {
        derivedStateOf {
            when {
                scrollState.firstVisibleItemIndex > 0 -> 1f
                else -> (scrollState.firstVisibleItemScrollOffset / 100f)
                    .coerceIn(0f, 1f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(config.barHeight + statusBarHeight)
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to MaterialTheme.colorScheme.surfaceDim.copy(alpha = collapseFraction * 0.70f),
                            (statusBarHeight / (config.barHeight + statusBarHeight)) to MaterialTheme.colorScheme.surfaceDim.copy(alpha = collapseFraction * 0.70f),
                            0.90f to MaterialTheme.colorScheme.surfaceDim.copy(alpha = collapseFraction * 0.30f),
                            1f to Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarHeight)
        ) {
            if (navigationIcon != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                ) { navigationIcon() }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = collapseFraction),
                modifier = Modifier.align(Alignment.Center)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                content = actions
            )
        }
    }
}

@Composable
fun LargeTitleItem(
    title: String,
    scrollState: LazyListState,
    modifier: Modifier = Modifier
) {
    val dimens = LocalAppDimens.current
    val config = LocalAdaptiveConfig.current
    val collapseFraction by remember {
        derivedStateOf {
            when {
                scrollState.firstVisibleItemIndex > 0 -> 1f
                else -> (scrollState.firstVisibleItemScrollOffset / 100f)
                    .coerceIn(0f, 1f)
            }
        }
    }

    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 1f - collapseFraction),
        fontSize = config.titleFontSize,
        modifier = modifier
            .padding(start = config.horizontalPadding, end = config.horizontalPadding, top = 8.dp, bottom = config.titlePaddingBottom)
    )
}
