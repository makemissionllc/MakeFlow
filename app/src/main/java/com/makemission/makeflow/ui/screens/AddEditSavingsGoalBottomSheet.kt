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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.SavingsGoalEntity
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
    onAddAccount: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEdit = existingGoal != null
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf(existingGoal?.name ?: "") }
    var amountInput by remember {
        mutableStateOf(existingGoal?.targetAmount?.let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
        } ?: "")
    }
    var selectedAccountId by remember {
        mutableStateOf(existingGoal?.linkedAccountId ?: accounts.firstOrNull()?.id ?: "")
    }
    var targetDate by remember { mutableStateOf(existingGoal?.targetDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val parsedAmount = amountInput.toDoubleOrNull()
    val isValid = name.isNotBlank() && parsedAmount != null && parsedAmount > 0 && selectedAccountId.isNotBlank()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetDate ?: (Calendar.getInstance().timeInMillis + 30L * 24 * 60 * 60 * 1000)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    targetDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = Color.White.copy(alpha = 0.6f)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AdaptiveSheetDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (isEdit) "Edit Goal" else "New Goal",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))

            Text("Goal name", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2B2E), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions.Default,
                decorationBox = { innerTextField ->
                    if (name.isEmpty()) {
                        Text("e.g. New Laptop", color = Color.White.copy(alpha = 0.3f), fontSize = 16.sp)
                    }
                    innerTextField()
                },
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Text("Target amount", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = amountInput,
                onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' } },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2B2E), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                decorationBox = { innerTextField ->
                    if (amountInput.isEmpty()) {
                        Text("0.00", color = Color.White.copy(alpha = 0.3f), fontSize = 16.sp)
                    }
                    innerTextField()
                },
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Text("Linked account", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { account ->
                    val selected = account.id == selectedAccountId
                    TextButton(
                        onClick = { selectedAccountId = account.id },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Color(0xFF2A2B2E) else Color(0xFF1B1C1F))
                    ) {
                        Text(
                            text = account.name,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onAddAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    "Create new account",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            Text("Target date (optional)", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2B2E), RoundedCornerShape(12.dp))
            ) {
                val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                Text(
                    text = targetDate?.let { df.format(Date(it)) } ?: "No deadline",
                    color = if (targetDate != null) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
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
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isValid) Color.White else Color(0xFF2A2B2E)),
            ) {
                Text(
                    text = if (isEdit) "Save Changes" else "Create Goal",
                    color = if (isValid) Color(0xFF121315) else Color.White.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
