package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TransactionEntrySheetContent(
    onDismiss: () -> Unit,
    onSaveTransaction: (TransactionType, FinancialAccount, String, Double) -> Unit
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountInput by remember { mutableStateOf("0") }
    
    // Core Seeded Accounts Configuration
    val accounts = remember {
        listOf(
            FinancialAccount("1", "Mpesa", 0.02, Color(0xFF00B074)),
            FinancialAccount("2", "Bank account", 17.40, Color(0xFF1E88E5)),
            FinancialAccount("3", "Investment", 11.07, Color(0xFF43A047))
        )
    }
    var selectedSourceAccount by remember { mutableStateOf(accounts[0]) }
    var selectedCategoryOrTarget by remember { mutableStateOf("") }

    // Dynamic Contextual Category Arrays
    val incomeCategories = listOf("Salary", "Allowance", "Gifts", "Freelance")
    val expenseCategories = listOf("Church", "Generosity", "Food & Drink", "Transport", "Rent")
    
    // Auto-update standard categories on type change
    LaunchedEffect(selectedType) {
        selectedCategoryOrTarget = when (selectedType) {
            TransactionType.INCOME -> incomeCategories.first()
            TransactionType.EXPENSE -> expenseCategories.first()
            TransactionType.TRANSFER -> accounts[1].name // Defaults to transferring to second account
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1011))
            .navigationBarsPadding()
    ) {
        // Header Controls Row Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Sheet",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onDismiss() }
            )
            Text(
                text = "New transaction",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(24.dp)) // Spacer baseline balance control
        }

        // Segmented Primary Switcher Module Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(Color(0xFF1E2124), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            TransactionType.values().forEach { type ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) Color(0xFF2C2F33) else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedType = type }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Multi-Account Selection Carousel Slider Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            accounts.forEach { account ->
                val isAccountSelected = selectedSourceAccount.id == account.id
                Row(
                    modifier = Modifier
                        .background(
                            color = if (isAccountSelected) account.iconColor.copy(alpha = 0.15f) else Color(0xFF1E2124),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedSourceAccount = account }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(account.iconColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(text = account.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = String.format("KES %,.2f", account.balance),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Transaction Entry Metric Line Display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "KES $amountInput",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add note",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // Sub-Category Selection Strip Wrapper
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (selectedType) {
                TransactionType.INCOME -> {
                    incomeCategories.forEach { category ->
                        CategoryChip(
                            label = category,
                            isSelected = selectedCategoryOrTarget == category,
                            onClick = { selectedCategoryOrTarget = category }
                        )
                    }
                }
                TransactionType.EXPENSE -> {
                    expenseCategories.forEach { category ->
                        CategoryChip(
                            label = category,
                            isSelected = selectedCategoryOrTarget == category,
                            onClick = { selectedCategoryOrTarget = category }
                        )
                    }
                }
                TransactionType.TRANSFER -> {
                    // Selection specifies destination targets mapping directly from remaining accounts
                    accounts.filter { it.id != selectedSourceAccount.id }.forEach { destAccount ->
                        CategoryChip(
                            label = "To ${destAccount.name}",
                            isSelected = selectedCategoryOrTarget == destAccount.name,
                            onClick = { selectedCategoryOrTarget = destAccount.name }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Numeric Entry Engine T9 Keypad
        val keys = listOf(
            "÷", "7", "8", "9", "⌫",
            "×", "4", "5", "6", "C",
            "-", "1", "2", "3", "✓",
            "+", "+/-", "0", ".", ""
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131416))
                .padding(6.dp),
            userScrollEnabled = false
        ) {
            items(keys) { key ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1.2f)
                        .padding(2.dp)
                        .clickable(enabled = key.isNotEmpty()) {
                            when (key) {
                                "⌫" -> {
                                    if (amountInput.length > 1) {
                                        amountInput = amountInput.dropLast(1)
                                    } else {
                                        amountInput = "0"
                                    }
                                }
                                "C" -> amountInput = "0"
                                "✓" -> {
                                    val finalValue = amountInput.toDoubleOrNull() ?: 0.0
                                    onSaveTransaction(
                                        selectedType,
                                        selectedSourceAccount,
                                        selectedCategoryOrTarget,
                                        finalValue
                                    )
                                }
                                "." -> {
                                    if (!amountInput.contains(".")) {
                                        amountInput += "."
                                    }
                                }
                                "÷", "×", "-", "+", "+/-" -> {
                                    // Math function hooks placeholder
                                }
                                else -> {
                                    if (amountInput == "0") {
                                        amountInput = key
                                    } else {
                                        amountInput += key
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (key) {
                        "⌫" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.8f))
                        "✓" -> Box(
                            modifier = Modifier
                                .fillMaxSize(0.8f)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Color.Black)
                        }
                        else -> Text(
                            text = key,
                            color = if (key == "÷" || key == "×" || key == "-" || key == "+") Color.White.copy(alpha = 0.4f) else Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color(0xFF1E2124),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
