package com.makemission.makeflow.core.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.SheetTopSlideShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheetDialog(
    onDismiss: () -> Unit,
    scrollable: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit
) {
    val dimens = LocalAppDimens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = SheetTopSlideShape
    ) {
        if (scrollable) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.paddingLarge)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = dimens.paddingExtra),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingXXLarge)
            ) {
                content()
            }
        }
        else content()
    }
}

@Composable
fun AdaptiveAlertSheet(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    val dimens = LocalAppDimens.current
    AdaptiveSheetDialog(
        onDismiss = onDismissRequest,
        containerColor = containerColor
    ) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            icon?.invoke()
            title?.invoke()
            text?.invoke()
            if (dismissButton != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton()
                    Spacer(Modifier.weight(1f))
                    confirmButton()
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    confirmButton()
                }
            }
        }
    }
}
