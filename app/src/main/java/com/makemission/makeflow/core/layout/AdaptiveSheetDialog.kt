package com.makemission.makeflow.core.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.makemission.makeflow.ui.theme.SheetTopSlideShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheetDialog(
    onDismiss: () -> Unit,
    scrollable: Boolean = true,
    content: @Composable () -> Unit
) {
    val layoutMode = LocalDeviceLayoutMode.current

    if (layoutMode == DeviceLayoutMode.COMPACT) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color(0xFF1B1C1F),
            shape = SheetTopSlideShape
        ) {
            if (scrollable) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    content()
                }
            } else {
                content()
            }
        }
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF1B1C1F),
                modifier = Modifier.widthIn(max = 480.dp)
            ) {
                if (scrollable) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        content()
                    }
                } else {
                    content()
                }
            }
        }
    }
}
