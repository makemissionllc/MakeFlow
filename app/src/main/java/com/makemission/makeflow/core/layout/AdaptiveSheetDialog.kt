package com.makemission.makeflow.core.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.NavbarPillShape
import com.makemission.makeflow.ui.theme.SheetTopSlideShape
import com.makemission.makeflow.ui.components.animateAppContentSize
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheetDialog(
    onDismiss: () -> Unit,
    scrollable: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    confirmDismiss: () -> Boolean = { true },
    content: @Composable () -> Unit
) {
    val dimens = LocalAppDimens.current
    val currentConfirmDismiss by rememberUpdatedState(confirmDismiss)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || currentConfirmDismiss() }
    )
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
                    .padding(top = dimens.spacingLarge)
                    .animateAppContentSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = dimens.paddingExtra),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingXXLarge)
            ) {
                content()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.paddingLarge)
                    .padding(top = dimens.spacingLarge, bottom = dimens.paddingExtra)
                    .animateAppContentSize()
            ) {
                content()
            }
        }
    }
}

@Composable
fun AdaptiveSheetHeader(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    eyebrow: String? = null
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
        ) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }
    }
}

@Composable
fun AdaptiveSheetPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticManager: HapticManager? = null
) {
    val dimens = LocalAppDimens.current
    Button(
        onClick = {
            hapticManager?.fire(HapticEvent.NAV_SELECT)
            onClick()
        },
        modifier = modifier.fillMaxWidth().height(dimens.primaryButtonHeight),
        enabled = enabled,
        shape = NavbarPillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = dimens.paddingLarge)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
        )
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
