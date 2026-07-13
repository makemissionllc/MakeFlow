package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.domain.DashboardCardType
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ReorderCardsSheet(
    currentOrder: List<DashboardCardType>,
    onSave: (List<DashboardCardType>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var items by remember { mutableStateOf(currentOrder) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    val itemHeight = 56.dp
    val spacing = 4.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }
    val totalItemStep = itemHeightPx + spacingPx

    AdaptiveSheetDialog(onDismiss = onDismiss, scrollable = false) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Edit Cards",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            val draggedIdx = draggedIndex
            val slotsCrossed = if (draggedIdx != null) {
                val threshold = itemHeightPx / 2f
                (dragOffset / threshold).roundToInt()
                    .coerceIn(-draggedIdx, items.size - 1 - draggedIdx)
            } else 0

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { startPos: androidx.compose.ui.geometry.Offset ->
                                val idx = (startPos.y / totalItemStep).toInt()
                                    .coerceIn(0, items.lastIndex)
                                draggedIndex = idx
                                dragOffset = 0f
                            },
                            onDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: androidx.compose.ui.geometry.Offset ->
                                change.consume()
                                if (draggedIndex == null) return@detectDragGesturesAfterLongPress
                                dragOffset += dragAmount.y
                            },
                            onDragEnd = {
                                val idx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                val threshold = itemHeightPx / 2f
                                val slots = (dragOffset / threshold).roundToInt()
                                    .coerceIn(-idx, items.size - 1 - idx)
                                if (slots != 0) {
                                    items = items.toMutableList().apply {
                                        val item = removeAt(idx)
                                        add(idx + slots, item)
                                    }
                                }
                                draggedIndex = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggedIndex = null
                                dragOffset = 0f
                            }
                        )
                    }
            ) {
                items.forEachIndexed { index, cardType ->
                    val isDragged = index == draggedIndex

                    val visualTranslation = when {
                        isDragged -> dragOffset
                        draggedIndex != null -> {
                            val di = draggedIndex!!
                            when {
                                index > di && index <= di + slotsCrossed -> totalItemStep
                                index < di && index >= di + slotsCrossed -> -totalItemStep
                                else -> 0f
                            }
                        }
                        else -> 0f
                    }

                    val elevation = if (isDragged) 12.dp else 0.dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .then(
                                if (elevation > 0.dp) {
                                    Modifier.shadow(elevation, RoundedCornerShape(12.dp))
                                } else Modifier
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF121315))
                            .graphicsLayer {
                                translationY = visualTranslation
                                if (isDragged) {
                                    scaleX = 1.03f
                                    scaleY = 1.03f
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Drag to reorder",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = cardType.displayName(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reset to automatic",
                    color = Color(0xFFEF5350),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable {
                            onReset()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp)
                )

                Text(
                    text = "Done",
                    color = Color(0xFF007AFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            onSave(items)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun DashboardCardType.displayName(): String = when (this) {
    DashboardCardType.INSIGHTS_SUMMARY -> "Insights Summary"
    DashboardCardType.ACCOUNTS -> "Accounts"
    DashboardCardType.BUDGET_PROGRESS -> "Budget Progress"
    DashboardCardType.RECURRING -> "Recurring"
}
