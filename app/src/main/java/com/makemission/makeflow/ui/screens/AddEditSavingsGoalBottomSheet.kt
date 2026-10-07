package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.layout.AdaptiveDatePickerSheet
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.SavingsGoalEntity
import com.makemission.makeflow.ui.theme.LocalAppDimens
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSavingsGoalBottomSheet(
    existingGoal: SavingsGoalEntity?,
    accounts: List<AccountEntity>,
    currencyCode: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, targetAmount: Double, targetDate: Long?, linkedAccountId: String) -> Unit,
    onAddAccount: () -> Unit,
    focusedAccountId: String? = null
) {
    val dimens = LocalAppDimens.current
    val scope = rememberCoroutineScope()
    val isEdit = existingGoal != null

    var name by remember { mutableStateOf(existingGoal?.name ?: "") }
    var amountInput by remember {
        mutableStateOf(existingGoal?.targetAmount?.let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
        } ?: "")
    }
    var selectedAccountId by remember(existingGoal?.id, focusedAccountId) {
        val initialId = existingGoal?.linkedAccountId
            ?: focusedAccountId
            ?: accounts.firstOrNull()?.id
            ?: ""
        mutableStateOf(initialId)
    }
    var targetDate by remember { mutableStateOf(existingGoal?.targetDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val parsedAmount = amountInput.toDoubleOrNull()
    val isValid = name.isNotBlank() && parsedAmount != null && parsedAmount > 0 && selectedAccountId.isNotBlank()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetDate ?: (Calendar.getInstance().timeInMillis + 30L * 24 * 60 * 60 * 1000)
        )
        AdaptiveDatePickerSheet(
            title = "Choose a goal date",
            state = datePickerState,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                targetDate = datePickerState.selectedDateMillis
                showDatePicker = false
            }
        )
    }

    AdaptiveSheetDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (isEdit) "Edit Goal" else "New Goal",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = dimens.textXxl,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(dimens.spacingXXLarge))

            Text("Goal name", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = dimens.textSm)
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerSmall))
                    .padding(dimens.paddingMedium),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textL),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions.Default,
                decorationBox = { innerTextField ->
                    if (name.isEmpty()) {
                        Text("e.g. New Laptop", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontSize = dimens.textL)
                    }
                    innerTextField()
                },
                singleLine = true
            )

            Spacer(Modifier.height(dimens.spacingXLarge))

            Text("Target amount", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = dimens.textSm)
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = amountInput,
                onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' } },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerSmall))
                    .padding(dimens.paddingMedium),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textL),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                decorationBox = { innerTextField ->
                    if (amountInput.isEmpty()) {
                        Text("0.00", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontSize = dimens.textL)
                    }
                    innerTextField()
                },
                singleLine = true
            )

            Spacer(Modifier.height(dimens.spacingXLarge))

            Text("Linked account", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = dimens.textSm)
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
            ) {
                accounts.forEach { account ->
                    val selected = account.id == selectedAccountId
                    TextButton(
                        onClick = { selectedAccountId = account.id },
                        modifier = Modifier
                            .clip(RoundedCornerShape(dimens.shapeCornerXSmall))
                            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Text(
                            text = account.name,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = dimens.textSm,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(dimens.spacingMedium))

            TextButton(
                onClick = onAddAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    "Create new account",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = dimens.textSm
                )
            }

            Spacer(Modifier.height(dimens.spacingXLarge))

            Text("Target date (optional)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = dimens.textSm)
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerSmall))
            ) {
                val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                Text(
                    text = targetDate?.let { df.format(Date(it)) } ?: "No deadline",
                    color = if (targetDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = dimens.textM
                )
            }

            Spacer(Modifier.height(24.dp))

            TextButton(
                onClick = {
                    val amount = parsedAmount ?: return@TextButton
                    onSave(name, amount, targetDate, selectedAccountId)
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
                    .background(if (isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Text(
                    text = if (isEdit) "Save Changes" else "Create Goal",
                    color = if (isValid) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Bold,
                    fontSize = dimens.textL
                )
            }

            Spacer(Modifier.height(dimens.spacingMedium))
        }
    }
}
