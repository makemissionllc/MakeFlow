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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.ui.theme.SheetTopSlideShape
import kotlinx.coroutines.launch

private val accountColors = listOf(
    "#4CAF50", "#2196F3", "#FF9800", "#E91E63",
    "#9C27B0", "#00BCD4", "#FF5722", "#607D8B"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountBottomSheet(
    onDismiss: () -> Unit,
    onSave: suspend (name: String, balance: Double, colorHex: String) -> Unit,
    account: AccountEntity? = null,
    currencySymbol: String
) {
    val isEdit = account != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(account?.name ?: "") }
    var balanceInput by remember { mutableStateOf(account?.balance?.let { String.format("%.2f", it) } ?: "") }
    var selectedColorHex by remember { mutableStateOf(account?.colorHex ?: accountColors.first()) }

    val parsedBalance = balanceInput.toDoubleOrNull()
    val isSaveEnabled = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1B1C1F),
        shape = SheetTopSlideShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEdit) "Edit Account" else "New Account",
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
                                Text(
                                    text = "Account name",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Current balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )

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
                        value = balanceInput,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                balanceInput = newValue
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
                                if (balanceInput.isEmpty()) {
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
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(accountColors) { colorHex ->
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
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                    } else Modifier
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

            androidx.compose.material3.Button(
                onClick = {
                    val balance = parsedBalance ?: 0.0
                    scope.launch {
                        onSave(name.trim(), balance, selectedColorHex)
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
                    text = if (isEdit) "Update Account" else "Save Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}