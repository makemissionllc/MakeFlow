package com.makemission.makeflow.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AddTransactionSheet(navController: NavController) {
    Log.d("MakeFlowDebug", "AddTransactionSheet composing")
    val context = LocalContext.current
    val db = remember {
        try {
            AppDatabase.getDatabase(context)
        } catch (e: Exception) {
            Log.e("MakeFlowDebug", "AddTransactionSheet getDatabase failed", e)
            return@remember null
        }
    } ?: return

    val categories by db.categoryDao().getAllCategories().collectAsState(initial = emptyList())

    var rawInput by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("EXPENSE") }
    var note by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val amount = parseAmount(rawInput)
    val isFormValid = amount > 0.0

    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCategoryId == null) {
            val firstCategory = categories.firstOrNull { it.name != "Income" }
            selectedCategoryId = firstCategory?.id ?: categories.first().id
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AddTopBar(onClose = { navController.popBackStack() })
        AmountDisplay(rawInput = rawInput)
        TransactionTypeSelector(
            selected = transactionType,
            onSelect = { transactionType = it }
        )
        NoteField(note = note, onNoteChange = { note = it })
        CategoryRow(
            categories = categories,
            selectedId = selectedCategoryId,
            onSelect = { selectedCategoryId = it }
        )
        NumpadGrid(
            onDigit = { d ->
                if (rawInput.contains(".") && rawInput.substringAfter(".").length >= 2) return@NumpadGrid
                rawInput += d
            },
            onDecimal = {
                if (!rawInput.contains(".")) rawInput += "."
            },
            onBackspace = {
                if (rawInput.isNotEmpty()) rawInput = rawInput.dropLast(1)
            },
            modifier = Modifier.weight(1f)
        )
        SubmitButton(
            enabled = isFormValid,
            onClick = {
                scope.launch {
                    val tx = TransactionEntity(
                        amount = amount,
                        type = transactionType,
                        categoryId = selectedCategoryId,
                        note = note.trim(),
                        timestamp = System.currentTimeMillis()
                    )
                    db.transactionDao().insertTransaction(tx)
                    navController.popBackStack()
                }
            }
        )
    }
}

@Composable
private fun AddTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = "Add",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun AmountDisplay(rawInput: String) {
    val display = formatNumpadDisplay(rawInput)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = display,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TransactionTypeSelector(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        listOf("Expense", "Income").forEach { label ->
            val isSelected = selected == label.uppercase()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onSelect(label.uppercase()) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun NoteField(note: String, onNoteChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(10.dp))
        val text = if (note.isEmpty()) "What was this for?" else note
        val color = if (note.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onBackground
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier
                .weight(1f)
                .clickable { /* focus field */ }
                .padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun CategoryRow(
    categories: List<CategoryEntity>,
    selectedId: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.id == selectedId
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSelect(category.id) }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForCategoryName(category.iconName, category.name),
                        contentDescription = category.name,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun NumpadGrid(
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                when (key) {
                                    "⌫" -> onBackspace()
                                    "." -> onDecimal()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "⌫") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmitButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = "Add Transaction",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun iconForCategoryName(iconName: String, categoryName: String): ImageVector {
    if (iconName.isNotBlank()) {
        return when (iconName) {
            "Home" -> Icons.Filled.Home
            "ShoppingCart" -> Icons.Filled.ShoppingCart
            "LocationOn" -> Icons.Filled.LocationOn
            "Star" -> Icons.Filled.Star
            "Check" -> Icons.Filled.Check
            else -> Icons.Filled.Check
        }
    }
    return when (categoryName) {
        "Electronics" -> Icons.Filled.ShoppingCart
        "Food & Dining" -> Icons.Filled.ShoppingCart
        "Transportation" -> Icons.Filled.LocationOn
        "Entertainment" -> Icons.Filled.Star
        "Housing" -> Icons.Filled.Home
        "Income" -> Icons.Filled.Check
        else -> Icons.Filled.Check
    }
}

private fun parseAmount(raw: String): Double {
    if (raw.isEmpty()) return 0.0
    return if (raw.contains(".")) {
        val whole = raw.substringBefore(".").ifEmpty { "0" }
        val frac = raw.substringAfter(".").padEnd(2, '0').take(2)
        "$whole.$frac".toDoubleOrNull() ?: 0.0
    } else {
        raw.toDoubleOrNull() ?: 0.0
    }
}

private fun formatNumpadDisplay(raw: String): String {
    if (raw.isEmpty()) return "$ 0.00"
    val commaFmt = NumberFormat.getNumberInstance(Locale.US)
    return if (raw.contains(".")) {
        val whole = raw.substringBefore(".").ifEmpty { "0" }
        val frac = raw.substringAfter(".").padEnd(2, '0').take(2)
        val wholeFormatted = commaFmt.format(whole.toLongOrNull() ?: 0L)
        "$ $wholeFormatted.$frac"
    } else {
        val formatted = commaFmt.format(raw.toLongOrNull() ?: 0L)
        "$ $formatted.00"
    }
}
