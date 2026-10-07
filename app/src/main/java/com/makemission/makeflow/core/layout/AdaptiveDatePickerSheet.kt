package com.makemission.makeflow.core.layout

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.makemission.makeflow.ui.theme.LocalAppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveDatePickerSheet(
    title: String,
    state: DatePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = "Choose date"
) {
    val dimens = LocalAppDimens.current
    AdaptiveSheetDialog(onDismiss = onDismiss) {
        AdaptiveSheetHeader(title = title, onDismiss = onDismiss)
        DatePicker(state = state)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            AdaptiveSheetPrimaryButton(
                label = confirmLabel,
                onClick = onConfirm,
                modifier = Modifier.weight(1.4f)
            )
        }
    }
}
