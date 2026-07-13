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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.RecurringTransactionEntity
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AddEditRecurringTransactionBottomSheet(
    onDismiss: () -> Unit,
    onSave: suspend (RecurringTransactionEntity) -> Unit,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    editEntity: RecurringTransactionEntity? = null,
    prefillAmount: Double? = null,
    prefillDescription: String? = null
) {
    val scope = rememberCoroutineScope()

    val isEditing = editEntity != null

    var description by remember {
        mutableStateOf(editEntity?.description ?: prefillDescription ?: "")
    }
    var amountInput by remember {
        mutableStateOf(
            if (editEntity != null) editEntity.amount.toString()
            else if (prefillAmount != null) prefillAmount.toString()
            else ""
        )
    }
    var selectedType by remember { mutableStateOf(editEntity?.type ?: "EXPENSE") }
    var selectedCategoryId by remember { mutableStateOf(editEntity?.categoryId ?: "") }
    var selectedAccountId by remember { mutableStateOf(editEntity?.accountId ?: "") }
    var selectedFrequency by remember { mutableStateOf(editEntity?.frequency ?: "MONTHLY") }
    var customDaysInput by remember { mutableStateOf(if (editEntity?.frequency == "CUSTOM_DAYS" && editEntity.intervalDays > 0) editEntity.intervalDays.toString() else "") }
    var autoConfirm by remember { mutableStateOf(editEntity?.autoConfirm ?: false) }
    var enableEndDate by remember { mutableStateOf(editEntity?.endDate != null) }

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    val expenseCategories = remember(categories) {
        categories.filter { it.type == "EXPENSE" }
    }

    val parsedAmount = amountInput.toDoubleOrNull()
    val intervalDays = when (selectedFrequency) {
        "WEEKLY" -> 7
        "MONTHLY" -> 30
        "ANNUAL" -> 365
        "CUSTOM_DAYS" -> customDaysInput.toIntOrNull() ?: 0
        else -> 30
    }
    val isSaveEnabled = description.isNotBlank()
            && parsedAmount != null && parsedAmount != 0.0
            && selectedAccountId.isNotEmpty()
            && intervalDays > 0
            && (selectedType != "EXPENSE" || selectedCategoryId.isNotEmpty())

    AdaptiveSheetDialog(
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEditing) "Edit Recurring" else "New Recurring",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
            BasicTextField(
                value = description,
                onValueChange = { description = it },
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                singleLine = true,
                cursorBrush = SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    Box {
                        if (description.isEmpty()) {
                            Text(
                                text = "e.g. Netflix Subscription",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1C1F), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currencySymbol,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = amountInput,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountInput = newValue
                    }
                },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                cursorBrush = SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    Box {
                        if (amountInput.isEmpty()) {
                            Text(
                                text = "0.00",
                                color = Color.White.copy(alpha = 0.3f),
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

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Type",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("INCOME", "EXPENSE").forEach { type ->
                    val isSelected = selectedType == type
                    Text(
                        text = type,
                        color = if (isSelected) Color(0xFF121315) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .background(
                                if (isSelected) Color.White else Color(0xFF1B1C1F),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                selectedType = type
                                selectedCategoryId = ""
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        if (selectedType == "EXPENSE") {
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
                if (filteredCategories.isEmpty()) {
                    Text(
                        text = "No categories available",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 13.sp
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredCategories) { cat ->
                            val isSelected = cat.id == selectedCategoryId
                            Text(
                                text = cat.name,
                                color = if (isSelected) Color(0xFF121315) else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .background(
                                        if (isSelected) Color.White else Color(0xFF1B1C1F),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedCategoryId = cat.id }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
            if (accounts.isEmpty()) {
                Text(
                    text = "No accounts — create one first",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 13.sp
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts) { acct ->
                        val isSelected = acct.id == selectedAccountId
                        Text(
                            text = acct.name,
                            color = if (isSelected) Color(0xFF121315) else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color.White else Color(0xFF1B1C1F),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedAccountId = acct.id }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Frequency",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("WEEKLY", "MONTHLY", "ANNUAL", "CUSTOM_DAYS").forEach { freq ->
                    val isSelected = selectedFrequency == freq
                    Text(
                        text = freq.replace("_", " "),
                        color = if (isSelected) Color(0xFF121315) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .background(
                                if (isSelected) Color.White else Color(0xFF1B1C1F),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedFrequency = freq }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            if (selectedFrequency == "CUSTOM_DAYS") {
                BasicTextField(
                    value = customDaysInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) customDaysInput = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White),
                    decorationBox = { innerTextField ->
                        Box {
                            if (customDaysInput.isEmpty()) {
                                Text(
                                    text = "Days",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .width(120.dp)
                        .background(Color(0xFF1B1C1F), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Auto-confirm",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (autoConfirm) "Creates transaction automatically" else "Asks before creating",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = autoConfirm,
                onCheckedChange = { autoConfirm = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                    uncheckedTrackColor = Color(0xFF2C2C2C)
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "End date",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (enableEndDate) "Stops after end date" else "Never ends",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = enableEndDate,
                onCheckedChange = { enableEndDate = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                    uncheckedTrackColor = Color(0xFF2C2C2C)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        androidx.compose.material3.Button(
            onClick = {
                val amount = parsedAmount ?: return@Button
                val now = System.currentTimeMillis()
                val entity = RecurringTransactionEntity(
                    id = editEntity?.id ?: UUID.randomUUID().toString(),
                    description = description.trim(),
                    amount = amount,
                    type = selectedType,
                    categoryId = if (selectedType == "EXPENSE") selectedCategoryId.ifEmpty { null } else null,
                    accountId = selectedAccountId,
                    frequency = selectedFrequency,
                    intervalDays = intervalDays,
                    startDate = editEntity?.startDate ?: now,
                    endDate = if (enableEndDate) (now + 365L * 24 * 60 * 60 * 1000) else null,
                    nextDueDate = editEntity?.nextDueDate ?: now,
                    autoConfirm = autoConfirm,
                    isActive = editEntity?.isActive ?: true
                )
                scope.launch {
                    onSave(entity)
                    onDismiss()
                }
            },
            enabled = isSaveEnabled,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF121315),
                disabledContainerColor = Color(0xFF1B1C1F),
                disabledContentColor = Color.White.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = if (isEditing) "Update" else "Create",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
