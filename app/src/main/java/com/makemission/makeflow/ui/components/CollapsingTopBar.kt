package com.makemission.makeflow.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.unit.sp

@Composable
fun CollapsingTopBar(
    title: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {}
) {
    val collapseFraction by remember {
        derivedStateOf {
            val scrollOffset = listState.firstVisibleItemScrollOffset
            (scrollOffset / 80f).coerceIn(0f, 1f)
        }
    }
    val animatedFraction by animateFloatAsState(
        targetValue = collapseFraction,
        animationSpec = tween(200),
        label = "collapseFraction"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A).copy(alpha = animatedFraction * 0.97f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .alpha(1f - animatedFraction),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading()
            Text(
                text = title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .alpha(animatedFraction),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading()
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }
    }
}
