package com.makemission.makeflow.core.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TabletContentContainer(
    modifier: Modifier = Modifier,
    maxWidth: androidx.compose.ui.unit.Dp = 960.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .padding(horizontal = 24.dp)
        ) {
            content()
        }
    }
}
