package com.makemission.makeflow.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.makemission.makeflow.R
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.layout.AdaptiveAlertSheet

@Composable
internal fun UnsavedTransactionSheet(
    hapticManager: HapticManager,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit
) {
    AdaptiveAlertSheet(
        onDismissRequest = onKeepEditing,
        title = {
            Text(stringResource(R.string.transaction_discard_title), style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                stringResource(R.string.transaction_discard_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = {
                hapticManager.fire(HapticEvent.NAV_SELECT)
                onKeepEditing()
            }) {
                Text(stringResource(R.string.transaction_keep_editing))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                hapticManager.fire(HapticEvent.NAV_SELECT)
                onDiscard()
            }) {
                Text(stringResource(R.string.transaction_discard), color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
