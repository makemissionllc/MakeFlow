package com.makemission.makeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CollapsingTopBar(
    title: String,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
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
            .background(Color(0xFF0A0A0A).copy(alpha = collapseFraction * 0.97f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(56.dp)
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
            color = Color.White.copy(alpha = collapseFraction),
            modifier = Modifier.align(Alignment.Center)
        )

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            content = actions
        )

        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .alpha(collapseFraction),
            thickness = 0.5.dp,
            color = Color(0xFF3A3A3A)
        )
    }
}

@Composable
fun LargeTitleItem(
    title: String,
    scrollState: LazyListState,
    modifier: Modifier = Modifier
) {
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
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 1f - collapseFraction),
        modifier = modifier
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
    )
}
