package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity

@Composable
fun TransactionEntrySheetContent(
    dbAccounts: List<AccountEntity>,
    dbCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSaveTransaction: (TransactionType, AccountEntity, CategoryEntity, Double) -> Unit
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountInput by remember { mutableStateOf("0") }
    
    // Source Account selection
    var selectedSourceAccount by remember { mutableStateOf(dbAccounts.firstOrNull()) }
    
    // Destination Account (only for transfers)
    var selectedDestinationAccount by remember { mutableStateOf<AccountEntity?>(null) }
    
    // Filter down matching category elements to show parent items or direct assignments
    val contextCategories = dbCategories.filter { it.type == selectedType.name }
    var selectedCategory by remember { mutableStateOf(contextCategories.firstOrNull()) }

    // Synchronize source account selection upon database loaded flow
    LaunchedEffect(dbAccounts) {
        if (selectedSourceAccount == null && dbAccounts.isNotEmpty()) {
            selectedSourceAccount = dbAccounts.firstOrNull()
        }
    }

    // Synchronize destination account selection for transfers
    LaunchedEffect(selectedSourceAccount, dbAccounts) {
        val destList = dbAccounts.filter { it.id != selectedSourceAccount?.id }
        if (selectedDestinationAccount == null || !destList.contains(selectedDestinationAccount)) {
            selectedDestinationAccount = destList.firstOrNull()
        }
    }

    // Synchronize category selection on type / category list update
    LaunchedEffect(selectedType, dbCategories) {
        val filtered = dbCategories.filter { it.type == selectedType.name }
        selectedCategory = filtered.firstOrNull()
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
                text = "New Transaction",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(24.dp))
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

        // ── SOURCE ACCOUNT SELECTOR ──
        val labelFrom = if (selectedType == TransactionType.TRANSFER) "From Account:" else "Account:"
        Text(
            text = labelFrom,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            dbAccounts.forEach { account ->
                val isAccountSelected = selectedSourceAccount?.id == account.id
                Row(
                    modifier = Modifier
                        .background(
                            color = if (isAccountSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF1E2124),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isAccountSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(text = account.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = String.format("KES %,.0f", account.balance),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // ── DESTINATION ACCOUNT SELECTOR (ONLY FOR TRANSFERS) ──
        if (selectedType == TransactionType.TRANSFER) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "To Account:",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val destinationAccounts = dbAccounts.filter { it.id != selectedSourceAccount?.id }
                destinationAccounts.forEach { account ->
                    val isAccountSelected = selectedDestinationAccount?.id == account.id
                    val colorTarget = MaterialTheme.colorScheme.primary
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (isAccountSelected) colorTarget.copy(alpha = 0.2f) else Color(0xFF1E2124),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isAccountSelected) colorTarget else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedDestinationAccount = account }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(colorTarget.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = colorTarget,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(text = account.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = String.format("KES %,.0f", account.balance),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Transaction Entry Metric Line Display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "KES $amountInput",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap confirm check to save",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // ── CATEGORY SELECTOR STRIP (ONLY FOR INCOME / EXPENSE) ──
        if (selectedType != TransactionType.TRANSFER) {
            Text(
                text = "Category:",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                contextCategories.forEach { category ->
                    val isSub = category.parentCategoryId != null
                    val displayLabel = if (isSub) "↳ ${category.name}" else category.name
                    
                    CategoryChip(
                        label = displayLabel,
                        isSelected = selectedCategory?.id == category.id,
                        onClick = { selectedCategory = category }
                    )
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
                                    if (selectedType == TransactionType.TRANSFER) {
                                        if (selectedSourceAccount != null && selectedDestinationAccount != null && finalValue > 0.0) {
                                            val dummyTransferCategory = CategoryEntity(
                                                id = "transfer_${selectedDestinationAccount!!.id}",
                                                name = "To ${selectedDestinationAccount!!.name}",
                                                type = "TRANSFER"
                                            )
                                            onSaveTransaction(
                                                selectedType,
                                                selectedSourceAccount!!,
                                                dummyTransferCategory,
                                                finalValue
                                            )
                                        }
                                    } else {
                                        if (selectedSourceAccount != null && selectedCategory != null && finalValue > 0.0) {
                                            onSaveTransaction(
                                                selectedType,
                                                selectedSourceAccount!!,
                                                selectedCategory!!,
                                                finalValue
                                            )
                                        }
                                    }
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
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Confirm", tint = MaterialTheme.colorScheme.onPrimary)
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
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF1E2124),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
