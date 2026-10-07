package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.core.layout.AdaptiveSheetHeader
import com.makemission.makeflow.core.layout.AdaptiveSheetPrimaryButton
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.data.local.entity.CategoryEntity
import kotlinx.coroutines.launch

@Composable
fun AddBudgetBottomSheet(
    onDismiss: () -> Unit,
    onSave: suspend (budgetId: String?, categoryId: String, categoryName: String, amount: Double, period: String, name: String?) -> Unit,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    editingBudget: BudgetEntity? = null
) {
    val dimens = LocalAppDimens.current
    val scope = rememberCoroutineScope()
    val isEditing = editingBudget != null

    val expenseParents = remember(categories) {
        categories.filter { it.type == "EXPENSE" && it.parentCategoryId == null }
    }

    val initialParent = remember(editingBudget, expenseParents) {
        if (editingBudget != null) {
            val cat = categories.find { it.id == editingBudget.categoryId }
            cat?.let { c ->
                if (c.parentCategoryId != null) {
                    expenseParents.find { it.id == c.parentCategoryId }
                } else {
                    expenseParents.find { it.id == c.id }
                }
            }
        } else null
    }
    val initialChild = remember(editingBudget, categories) {
        if (editingBudget != null) {
            val cat = categories.find { it.id == editingBudget.categoryId }
            cat?.takeIf { it.parentCategoryId != null }
        } else null
    }

    var selectedParentCategory by remember { mutableStateOf(initialParent ?: expenseParents.firstOrNull()) }
    var selectedChildCategory by remember { mutableStateOf(initialChild) }

    val childCategories = remember(categories, selectedParentCategory) {
        selectedParentCategory?.let { parent ->
            categories.filter { it.parentCategoryId == parent.id }
        } ?: emptyList()
    }

    val selectedCategoryId = selectedChildCategory?.id ?: selectedParentCategory?.id ?: ""
    val selectedCategoryName = selectedChildCategory?.name ?: selectedParentCategory?.name ?: ""

    var amountInput by remember {
        mutableStateOf(editingBudget?.amount?.let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
        } ?: "")
    }
    var nameInput by remember { mutableStateOf(editingBudget?.name ?: "") }
    var selectedPeriod by remember { mutableStateOf(editingBudget?.period ?: "Monthly") }

    val parsedAmount = amountInput.toDoubleOrNull()
    val isSaveEnabled = parsedAmount != null && parsedAmount != 0.0 && selectedCategoryId.isNotEmpty()

    AdaptiveSheetDialog(
        onDismiss = onDismiss
    ) {
        AdaptiveSheetHeader(
            title = if (isEditing) "Edit Budget" else "New Budget",
            onDismiss = onDismiss
        )

        if (isEditing) {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = selectedCategoryName,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = dimens.textMl,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerSmall))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                    expenseParents.forEach { parent ->
                        val isSelected = parent.id == selectedParentCategory?.id
                        Text(
                            text = parent.name,
                            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = dimens.textSm,
                            modifier = Modifier
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(dimens.cornerRadiusMedium)
                                )
                                .clickable {
                                    selectedParentCategory = parent
                                    selectedChildCategory = null
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
                if (childCategories.isNotEmpty()) {
                    Text(
                        text = "Subcategory",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                        childCategories.forEach { child ->
                            val isSelected = child.id == selectedChildCategory?.id
                            Text(
                                text = child.name,
                                color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = dimens.textSm,
                                modifier = Modifier
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        RoundedCornerShape(dimens.cornerRadiusMedium)
                                    )
                                    .clickable { selectedChildCategory = child }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(dimens.spacingMedium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currencySymbol,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(dimens.spacingMedium))
            BasicTextField(
                value = amountInput,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountInput = newValue
                    }
                },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Box {
                        if (amountInput.isEmpty()) {
                            Text(
                                text = "0.00",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.width(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(dimens.spacingMedium))

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
            Text(
                text = "Name (optional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            BasicTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = dimens.textMl,
                    fontWeight = FontWeight.Medium
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerSmall))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (nameInput.isEmpty()) {
                            Text(
                                text = selectedCategoryName,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                fontSize = dimens.textMl
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(dimens.spacingMedium))

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
            Text(
                text = "Resets every",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                listOf("Weekly", "Monthly", "Annual").forEach { period ->
                    val isSelected = selectedPeriod == period
                    Text(
                        text = period,
                        color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = dimens.textSm,
                        modifier = Modifier
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh,
                                RoundedCornerShape(dimens.cornerRadiusMedium)
                            )
                            .clickable { selectedPeriod = period }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(dimens.spacingLarge))

        AdaptiveSheetPrimaryButton(
            label = if (isEditing) "Save Changes" else "Save Budget",
            onClick = {
                parsedAmount?.let { amount ->
                    scope.launch {
                    val budgetName = nameInput.ifBlank { null }
                    onSave(editingBudget?.id, selectedCategoryId, selectedCategoryName, amount, selectedPeriod, budgetName)
                    onDismiss()
                    }
                }
            },
            enabled = isSaveEnabled
        )
    }
}
