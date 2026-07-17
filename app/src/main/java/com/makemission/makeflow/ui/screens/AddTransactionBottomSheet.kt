package com.makemission.makeflow.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.ml.CategoryClassifier
import com.makemission.makeflow.core.ml.CategoryPrediction
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionTemplateEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.domain.AnomalySeverity
import com.makemission.makeflow.domain.CategoryLabelResolver
import com.makemission.makeflow.domain.CategorySuggester
import com.makemission.makeflow.domain.CategorySuggestion
import com.makemission.makeflow.domain.PastTransactionForSuggestion
import com.makemission.makeflow.domain.TransactionCommitHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val ML_CONFIDENCE_THRESHOLD = 0.6f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    onDismiss: () -> Unit,
    onSave: suspend (amount: Double, type: String, categoryId: String?, accountId: String, note: String, timestamp: Long, fromAccountId: String?, toAccountId: String?) -> Unit,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    currencyCode: String,
    hapticManager: HapticManager,
    templates: List<TransactionTemplateEntity> = emptyList(),
    onSaveAsTemplate: ((name: String, amount: Double, type: String, categoryId: String?, accountId: String, note: String) -> Unit)? = null,
    commitHandler: TransactionCommitHandler? = null,
    onGetPastTransactions: (suspend () -> List<PastTransactionForSuggestion>)? = null,
    categoryClassifier: CategoryClassifier? = null,
    smartCategorizationEnabled: Boolean = true,
    correctionMemory: Map<String, String> = emptyMap(),
    onCategoryCorrected: ((description: String, correctedCategoryId: String, originalSuggestedCategoryId: String?) -> Unit)? = null,
    initialType: String = "INCOME",
    initialAmount: String = "",
    initialNote: String = "",
    initialTimestamp: Long? = null,
    initialAccountId: String? = null,
    initialTransferFromId: String? = null,
    initialTransferToId: String? = null,
    editTransaction: TransactionEntity? = null,
    onUpdate: (suspend (TransactionEntity) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val isEditMode = editTransaction != null

    var selectedType by remember { mutableStateOf(editTransaction?.type ?: initialType) }
    var amountInput by remember {
        val amt = editTransaction?.amount
        mutableStateOf(if (amt != null) {
            if (amt == amt.toLong().toDouble()) amt.toLong().toString() else amt.toString()
        } else initialAmount)
    }
    var selectedAccountId by remember { mutableStateOf(editTransaction?.accountId ?: initialAccountId ?: accounts.firstOrNull()?.id ?: "") }
    var selectedTransferFromId by remember { mutableStateOf(editTransaction?.fromAccountId ?: initialTransferFromId ?: accounts.firstOrNull()?.id ?: "") }
    var selectedTransferToId by remember { mutableStateOf(editTransaction?.toAccountId ?: initialTransferToId ?: accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: "") }
    var note by remember { mutableStateOf(editTransaction?.note ?: initialNote) }
    var selectedDate by remember { mutableStateOf(editTransaction?.timestamp ?: initialTimestamp ?: Calendar.getInstance().timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var categorySuggestion by remember { mutableStateOf<CategorySuggestion?>(null) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var templateNameInput by remember { mutableStateOf("") }
    val pastTransactions = remember { mutableStateOf<List<PastTransactionForSuggestion>>(emptyList()) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val parentCategories = remember(categories, selectedType) {
        if (selectedType == "TRANSFER") emptyList()
        else categories.filter { it.type == selectedType && it.parentCategoryId == null }
    }

    val initialParentCat = remember(editTransaction, categories) {
        if (editTransaction != null && editTransaction.type != "TRANSFER" && editTransaction.categoryId != null) {
            val cat = categories.find { it.id == editTransaction.categoryId }
            if (cat?.parentCategoryId != null) categories.find { it.id == cat.parentCategoryId } else cat
        } else null
    }
    val initialChildCat = remember(editTransaction, categories) {
        if (editTransaction != null && editTransaction.type != "TRANSFER" && editTransaction.categoryId != null) {
            val cat = categories.find { it.id == editTransaction.categoryId }
            if (cat?.parentCategoryId != null) cat else null
        } else null
    }

    var selectedParentCategory by remember { mutableStateOf(initialParentCat) }
    var selectedChildCategory by remember { mutableStateOf(initialChildCat) }
    var categoryResetSkippedForEdit by remember { mutableStateOf(isEditMode) }

    LaunchedEffect(Unit) {
        if (onGetPastTransactions != null) {
            pastTransactions.value = onGetPastTransactions()
        }
    }

    LaunchedEffect(note, smartCategorizationEnabled, correctionMemory) {
        if (selectedType == "TRANSFER" || note.length < 2) {
            categorySuggestion = null
            return@LaunchedEffect
        }
        delay(300)

        val suggesterResult = CategorySuggester.suggest(note, pastTransactions.value)

        if (suggesterResult != null && suggesterResult.confidence >= 1.0f) {
            categorySuggestion = suggesterResult
            val cat = parentCategories.find { it.id == suggesterResult.categoryId }
            if (cat != null) {
                selectedParentCategory = cat
                selectedChildCategory = null
            } else {
                val childCat = categories.find { it.id == suggesterResult.categoryId }
                if (childCat != null) {
                    val parent = categories.find { it.id == childCat.parentCategoryId }
                    if (parent != null) {
                        selectedParentCategory = parent
                        selectedChildCategory = childCat
                    }
                }
            }
            return@LaunchedEffect
        }

        val normalized = CategorySuggester.normalize(note)
        correctionMemory[normalized]?.let { memoryCategoryId ->
            val memCat = categories.find { it.id == memoryCategoryId }
            if (memCat != null) {
                categorySuggestion = CategorySuggestion(
                    categoryId = memoryCategoryId,
                    confidence = 0.95f,
                    matchedOnExactPastDescription = false
                )
                if (memCat.parentCategoryId != null) {
                    val parent = parentCategories.find { it.id == memCat.parentCategoryId }
                    selectedParentCategory = parent
                    selectedChildCategory = memCat
                } else {
                    selectedParentCategory = memCat
                    selectedChildCategory = null
                }
                return@LaunchedEffect
            }
        }

        if (smartCategorizationEnabled && categoryClassifier != null) {
            val prediction = withContext(Dispatchers.IO) {
                categoryClassifier.classify(note)
            }
            if (prediction != null && prediction.confidence >= ML_CONFIDENCE_THRESHOLD) {
                val mlCat = CategoryLabelResolver.resolve(prediction.categoryName, categories)
                if (mlCat != null) {
                    categorySuggestion = CategorySuggestion(
                        categoryId = mlCat.id,
                        confidence = prediction.confidence,
                        matchedOnExactPastDescription = false
                    )
                    if (mlCat.parentCategoryId != null) {
                        val parent = parentCategories.find { it.id == mlCat.parentCategoryId }
                        selectedParentCategory = parent
                        selectedChildCategory = mlCat
                    } else {
                        selectedParentCategory = mlCat
                        selectedChildCategory = null
                    }
                    return@LaunchedEffect
                }
            }
        }

        if (suggesterResult != null && suggesterResult.confidence >= 0.5f) {
            categorySuggestion = suggesterResult
        } else {
            categorySuggestion = null
        }
    }

    val childCategories = remember(categories, selectedParentCategory) {
        if (selectedType == "TRANSFER") emptyList()
        else selectedParentCategory?.let { parent ->
            categories.filter { it.parentCategoryId == parent.id }
        } ?: emptyList()
    }

    val resolvedCategoryId = if (selectedType == "TRANSFER") null
        else selectedChildCategory?.id ?: selectedParentCategory?.id

    LaunchedEffect(selectedType, categories) {
        if (categoryResetSkippedForEdit) {
            categoryResetSkippedForEdit = false
            return@LaunchedEffect
        }
        if (selectedType != "TRANSFER") {
            val parents = categories.filter { it.type == selectedType && it.parentCategoryId == null }
            selectedParentCategory = parents.firstOrNull()
            selectedChildCategory = null
        } else {
            selectedParentCategory = null
            selectedChildCategory = null
        }
    }

    val parsedAmount = amountInput.toDoubleOrNull()
    val isSaveEnabled = parsedAmount != null && parsedAmount != 0.0 &&
        (selectedType != "TRANSFER" || selectedTransferFromId != selectedTransferToId)

    fun fireCorrection() {
        val suggestedId = categorySuggestion?.categoryId
        if (suggestedId != null && resolvedCategoryId != null && resolvedCategoryId != suggestedId) {
            onCategoryCorrected?.invoke(note, resolvedCategoryId, suggestedId)
        }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) {
                    Text("OK", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AdaptiveSheetDialog(
        onDismiss = onDismiss
    ) {
        val reducedMotion = LocalReducedMotion.current
        AnimatedVisibility(
                visible = commitHandler?.anomalyResult != null,
                enter = if (reducedMotion) fadeIn() else slideInVertically { -it } + fadeIn(),
                exit = if (reducedMotion) fadeOut() else slideOutVertically { -it } + fadeOut()
            ) {
                commitHandler?.anomalyResult?.let { result ->
                    val categoryName = selectedChildCategory?.name ?: selectedParentCategory?.name ?: "this category"
                    val message = if (result.severity == AnomalySeverity.HIGH) {
                        "This is significantly higher than your usual $categoryName spend (avg ${CurrencyFormatter.format(result.categoryAverage, currencyCode)})"
                    } else {
                        "This is higher than your usual $categoryName spend"
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2C2C1E), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message,
                            color = Color(0xFFFFA726),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "Edit Transaction" else "Add Transaction",
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

            if (!isEditMode && templates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Templates",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        templates.forEach { template ->
                            Text(
                                text = template.name,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .background(Color(0xFF2C2C1E), RoundedCornerShape(14.dp))
                                    .clickable {
                                        amountInput = if (template.amount == template.amount.toLong().toDouble())
                                            template.amount.toLong().toString() else template.amount.toString()
                                        selectedType = template.type
                                        selectedAccountId = if (accounts.any { it.id == template.accountId })
                                            template.accountId else selectedAccountId
                                        note = template.note
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Income" to "INCOME", "Expense" to "EXPENSE", "Transfer" to "TRANSFER").forEach { (label, value) ->
                    val isSelected = selectedType == value
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF121315) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) Color.White else Color(0xFF1B1C1F),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedType = value }
                            .padding(vertical = 12.dp)
                    )
                }
            }

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
                        } else {
                            hapticManager.fire(HapticEvent.VALIDATION_ERROR)
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

            if (selectedType == "TRANSFER") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "From account",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { account ->
                            val isSelected = account.id == selectedTransferFromId
                            Text(
                                text = account.name,
                                color = if (isSelected) Color(0xFF121315) else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .background(
                                        if (isSelected) Color.White else Color(0xFF1B1C1F),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedTransferFromId = account.id }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "To account",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { account ->
                            val isSelected = account.id == selectedTransferToId
                            Text(
                                text = account.name,
                                color = if (isSelected) Color(0xFF121315) else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .background(
                                        if (isSelected) Color.White else Color(0xFF1B1C1F),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedTransferToId = account.id }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { account ->
                            val isSelected = account.id == selectedAccountId
                            Text(
                                text = account.name,
                                color = if (isSelected) Color(0xFF121315) else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .background(
                                        if (isSelected) Color.White else Color(0xFF1B1C1F),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedAccountId = account.id }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (selectedType != "TRANSFER") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    parentCategories.forEach { parent ->
                        val isSelected = parent.id == selectedParentCategory?.id
                        val isSuggested = !isSelected && categorySuggestion?.categoryId == parent.id
                        val bgColor = when {
                            isSelected -> Color.White
                            isSuggested -> Color(0xFF2C2C1E)
                            else -> Color(0xFF1B1C1F)
                        }
                        val textColor = if (isSelected) Color(0xFF121315) else Color.White
                        val borderMod = if (isSuggested) Modifier.background(bgColor, RoundedCornerShape(14.dp))
                            else Modifier.background(bgColor, RoundedCornerShape(14.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = parent.name,
                                color = textColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .then(borderMod)
                                    .clickable {
                                        selectedParentCategory = parent
                                        selectedChildCategory = null
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                            if (isSuggested) {
                                Text(
                                    text = "suggested",
                                    color = Color(0xFFFFA726),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                if (childCategories.isNotEmpty()) {
                    Text(
                        text = "Subcategory",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.35f)
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        childCategories.forEach { child ->
                            val isSelected = child.id == selectedChildCategory?.id
                            val isSuggested = !isSelected && categorySuggestion?.categoryId == child.id
                            val bgColor = when {
                                isSelected -> Color.White
                                isSuggested -> Color(0xFF2C2C1E)
                                else -> Color(0xFF1B1C1F)
                            }
                            val textColor = if (isSelected) Color(0xFF121315) else Color.White
                            val borderMod = if (isSuggested) Modifier.background(bgColor, RoundedCornerShape(14.dp))
                                else Modifier.background(bgColor, RoundedCornerShape(14.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = child.name,
                                    color = textColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .then(borderMod)
                                        .clickable { selectedChildCategory = child }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                                if (isSuggested) {
                                    Text(
                                        text = "suggested",
                                        color = Color(0xFFFFA726),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White),
                    decorationBox = { innerTextField ->
                        Box {
                            if (note.isEmpty()) {
                                Text(
                                    text = "Add a note (optional)",
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.DateRange,
                    contentDescription = "Select date",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = dateFormat.format(Date(selectedDate)),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isEditMode && onSaveAsTemplate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSaveTemplateDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Save as template",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (showSaveTemplateDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = {
                        showSaveTemplateDialog = false
                        templateNameInput = ""
                    },
                    containerColor = Color(0xFF1B1C1F),
                    title = {
                        Text("Save as template", color = Color.White, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            Text(
                                text = "Name",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            BasicTextField(
                                value = templateNameInput,
                                onValueChange = { templateNameInput = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                singleLine = true,
                                cursorBrush = SolidColor(Color.White),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (templateNameInput.isEmpty()) {
                                            Text(
                                                text = "e.g. Monthly Rent",
                                                color = Color.White.copy(alpha = 0.3f),
                                                fontSize = 16.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                if (templateNameInput.isNotBlank()) {
                                    onSaveAsTemplate?.invoke(templateNameInput.trim(), parsedAmount ?: 0.0, selectedType, resolvedCategoryId, selectedAccountId, note)
                                    showSaveTemplateDialog = false
                                    templateNameInput = ""
                                }
                            },
                            enabled = templateNameInput.isNotBlank()
                        ) {
                            Text("Save", color = Color.White)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showSaveTemplateDialog = false
                                templateNameInput = ""
                            }
                        ) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                )
            }

            commitHandler?.duplicateWarning?.let { warning ->
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = {
                        commitHandler?.clearAlerts()
                    },
                    containerColor = Color(0xFF1B1C1F),
                    title = {
                        Text("Possible duplicate", color = Color.White, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(
                            "This looks like a ${warning.reason} ÔÇö save anyway?",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            val entity = commitHandler?.pendingEntity ?: return@TextButton
                            scope.launch {
                                commitHandler?.saveIgnoringDuplicate(
                                    entity = entity,
                                    onSave = { tx ->
                                        fireCorrection()
                                        onSave(tx.amount, tx.type, tx.categoryId, tx.accountId, tx.note, tx.timestamp,
                                            if (tx.type == "TRANSFER") tx.fromAccountId else null,
                                            if (tx.type == "TRANSFER") tx.toAccountId else null)
                                    },
                                    onComplete = { onDismiss() }
                                )
                            }
                        }) {
                            Text("Save Anyway", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            commitHandler?.clearAlerts()
                        }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                )
            }

            androidx.compose.material3.Button(
                onClick = {
                    val amount = parsedAmount ?: return@Button
                    scope.launch {
                        val fromId = if (selectedType == "TRANSFER") selectedTransferFromId else null
                        val toId = if (selectedType == "TRANSFER") selectedTransferToId else null
                        if (isEditMode && onUpdate != null && editTransaction != null) {
                            val updatedEntity = editTransaction.copy(
                                amount = amount,
                                type = selectedType,
                                categoryId = resolvedCategoryId,
                                accountId = selectedAccountId,
                                fromAccountId = fromId,
                                toAccountId = toId,
                                note = note,
                                timestamp = selectedDate
                            )
                            if (commitHandler != null && editTransaction != null) {
                                commitHandler.update(entity = updatedEntity, originalTransaction = editTransaction) {
                                    onUpdate(it)
                                }
                                fireCorrection()
                                onDismiss()
                            } else {
                                onUpdate(updatedEntity)
                                fireCorrection()
                                onDismiss()
                            }
                        } else {
                            val entity = TransactionEntity(
                                amount = amount,
                                type = selectedType,
                                categoryId = resolvedCategoryId,
                                accountId = selectedAccountId,
                                fromAccountId = fromId,
                                toAccountId = toId,
                                note = note,
                                timestamp = selectedDate
                            )
                            if (commitHandler != null) {
                                commitHandler.prepareSave(
                                    entity = entity,
                                    onSave = { tx ->
                                        fireCorrection()
                                        onSave(tx.amount, tx.type, tx.categoryId, tx.accountId, tx.note, tx.timestamp,
                                            if (tx.type == "TRANSFER") tx.fromAccountId else null,
                                            if (tx.type == "TRANSFER") tx.toAccountId else null)
                                    },
                                    onComplete = { onDismiss() }
                                )
                            } else {
                                onSave(amount, selectedType, resolvedCategoryId, selectedAccountId, note, selectedDate, fromId, toId)
                                fireCorrection()
                                onDismiss()
                            }
                        }
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
                    text = if (isEditMode) "Update Transaction" else "Save Transaction",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
    }
}
