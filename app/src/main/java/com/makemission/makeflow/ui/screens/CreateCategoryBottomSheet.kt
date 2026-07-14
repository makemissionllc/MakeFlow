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
                color = Color.White
            )
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                singleLine = true,
                cursorBrush = SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    Box {
                        if (name.isEmpty()) {
                            Text("Category name", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Type", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("EXPENSE", "INCOME").forEach { t ->
                    val isSelected = type == t
                    Text(
                        text = t.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) Color(0xFF121315) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .background(if (isSelected) Color.White else Color(0xFF1B1C1F), RoundedCornerShape(14.dp))
                            .clickable {
                                type = t
                                parentCategoryId = null
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Parent category (optional)", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val noneSelected = parentCategoryId == null
                Text(
                    text = "None (top-level)",
                    color = if (noneSelected) Color(0xFF121315) else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(if (noneSelected) Color.White else Color(0xFF1B1C1F), RoundedCornerShape(14.dp))
                        .clickable { parentCategoryId = null }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                typedParents.forEach { parent ->
                    val isSelected = parentCategoryId == parent.id
                    Text(
                        text = parent.name,
                        color = if (isSelected) Color(0xFF121315) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .background(if (isSelected) Color.White else Color(0xFF1B1C1F), RoundedCornerShape(14.dp))
                            .clickable { parentCategoryId = parent.id }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Color", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                categoryColors.forEach { colorHex ->
                    val isSelected = colorHex == selectedColorHex
                    val dotColor = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (_: Exception) {
                        Color(0xFF4CAF50)
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .then(
                                if (isSelected) Modifier.border(width = 2.dp, color = Color.White, shape = CircleShape)
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

        Spacer(modifier = Modifier.height(4.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Icon", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categoryIcons.forEach { icon ->
                    val isSelected = icon == selectedIcon
                    Text(
                        text = icon.removePrefix("Local").removePrefix("Directions").replace("([a-z])([A-Z])".toRegex(), "$1 $2"),
                        color = if (isSelected) Color(0xFF121315) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .background(if (isSelected) Color.White else Color(0xFF1B1C1F), RoundedCornerShape(14.dp))
                            .clickable { selectedIcon = icon }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSaveEnabled) Color.White else Color(0xFF1B1C1F),
                    RoundedCornerShape(16.dp)
                )
                .height(54.dp)
        ) {
            Text(
                text = "Create Category",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSaveEnabled) Color(0xFF121315) else Color.White.copy(alpha = 0.2f)
            )
        }
    }
}
