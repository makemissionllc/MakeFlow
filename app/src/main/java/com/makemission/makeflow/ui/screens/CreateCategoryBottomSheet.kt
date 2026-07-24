package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import kotlinx.coroutines.launch

private val categoryColors = listOf(
    "#4CAF50", "#2196F3", "#FF9800", "#E91E63",
    "#9C27B0", "#00BCD4", "#FF5722", "#607D8B",
    "#8BC34A", "#FFC107", "#3F51B5", "#009688",
    "#795548", "#F44336", "#673AB7", "#CDDC39"
)

private val categoryIcons = listOf(
    "Check", "Shopping", "Restaurant", "Flight",
    "Home", "Work", "School", "Fitness",
    "Pets", "CardGiftcard", "Build", "Security",
    "Movie", "Palette", "Sports", "BeachAccess",
    "DirectionsCar", "DirectionsBus", "Phone", "Wifi",
    "LocalHospital", "AccountBalance", "TrendingUp", "MoreHoriz"
)

@Composable
fun CreateCategoryBottomSheet(
    onDismiss: () -> Unit,
    onCreate: suspend (CategoryEntity) -> String?,
    parentCategories: List<CategoryEntity>,
    existingType: String = "EXPENSE"
) {
    val dimens = LocalAppDimens.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(existingType) }
    var parentCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedColorHex by remember { mutableStateOf(categoryColors.first()) }
    var selectedIcon by remember { mutableStateOf(categoryIcons.first()) }

    val typedParents = parentCategories.filter { it.type == type && it.parentCategoryId == null }
    val isSaveEnabled = name.isNotBlank()

    AdaptiveSheetDialog(onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "New Category",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textM),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Box {
                        if (name.isEmpty()) {
                            Text("Category name", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = dimens.textM)
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(dimens.spacingXSmall))

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
            Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                listOf("EXPENSE", "INCOME").forEach { t ->
                    val isSelected = type == t
                    Text(
                        text = t.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = dimens.textSm,
                        modifier = Modifier
                            .background(if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusMedium))
                            .clickable {
                                type = t
                                parentCategoryId = null
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(dimens.spacingXSmall))

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
            Text("Parent category (optional)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                val noneSelected = parentCategoryId == null
                Text(
                    text = "None (top-level)",
                    color = if (noneSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = dimens.textSm,
                    modifier = Modifier
                        .background(if (noneSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusMedium))
                        .clickable { parentCategoryId = null }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                typedParents.forEach { parent ->
                    val isSelected = parentCategoryId == parent.id
                    Text(
                        text = parent.name,
                        color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = dimens.textSm,
                        modifier = Modifier
                            .background(if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusMedium))
                            .clickable { parentCategoryId = parent.id }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(dimens.spacingXSmall))

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
            Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)) {
                categoryColors.forEach { colorHex ->
                    val isSelected = colorHex == selectedColorHex
                    val dotColor = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (_: Exception) {
                        LocalMakeFlowColors.current.colorSuccess
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .then(
                                if (isSelected) Modifier.border(width = 2.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                                else Modifier
                            )
                            .padding(if (isSelected) 3.dp else 0.dp)
                            .clip(CircleShape)
                            .size(28.dp)
                            .background(dotColor, CircleShape)
                            .clickable { selectedColorHex = colorHex }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(dimens.spacingXSmall))

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
            Text("Icon", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                categoryIcons.forEach { icon ->
                    val isSelected = icon == selectedIcon
                    Text(
                        text = icon.removePrefix("Local").removePrefix("Directions").replace("([a-z])([A-Z])".toRegex(), "$1 $2"),
                        color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = dimens.textS,
                        maxLines = 1,
                        modifier = Modifier
                            .background(if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusMedium))
                            .clickable { selectedIcon = icon }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(dimens.spacingLarge))

        TextButton(
            onClick = {
                val trimmedName = name.trim()
                if (trimmedName.isBlank()) return@TextButton
                scope.launch {
                    val entity = CategoryEntity(
                        name = trimmedName,
                        type = type,
                        parentCategoryId = parentCategoryId,
                        iconName = selectedIcon,
                        colorHex = selectedColorHex
                    )
                    onCreate(entity)
                    onDismiss()
                }
            },
            enabled = isSaveEnabled,
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSaveEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(dimens.shapeCornerMedium)
                )
                .height(54.dp)
        ) {
            Text(
                text = "Create Category",
                fontSize = dimens.textL,
                fontWeight = FontWeight.Bold,
                color = if (isSaveEnabled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        }
    }
}
