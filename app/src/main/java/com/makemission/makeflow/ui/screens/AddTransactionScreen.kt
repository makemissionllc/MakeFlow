package com.makemission.makeflow.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.ml.CategoryClassifier
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.local.entity.TransactionTemplateEntity
import com.makemission.makeflow.domain.AnomalyResult
import com.makemission.makeflow.domain.AnomalySeverity
import com.makemission.makeflow.domain.CalculatorEngine
import com.makemission.makeflow.domain.CategoryLabelResolver
import com.makemission.makeflow.domain.CategorySuggester
import com.makemission.makeflow.domain.CategorySuggestion
import com.makemission.makeflow.domain.DuplicateCheckResult
import com.makemission.makeflow.domain.PastTransactionForSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val ML_CONFIDENCE_THRESHOLD = 0.6f

@Composable
fun AddTransactionScreen(
    onDismiss: () -> Unit,
    onSave: suspend (amount: Double, type: String, categoryId: String?, accountId: String, note: String, timestamp: Long, fromAccountId: String?, toAccountId: String?) -> Unit,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    currencyCode: String,
    hapticManager: HapticManager,
    templates: List<TransactionTemplateEntity> = emptyList(),
    onSaveAsTemplate: ((name: String, amount: Double, type: String, categoryId: String?, accountId: String, note: String) -> Unit)? = null,
    onAnomalyCheck: (suspend (Double, String?) -> AnomalyResult?)? = null,
    onGetPastTransactions: (suspend () -> List<PastTransactionForSuggestion>)? = null,
    onCheckDuplicate: (suspend (TransactionEntity) -> DuplicateCheckResult?)? = null,
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
    val context = LocalContext.current
    val isEditMode = editTransaction != null

    // === Sub-screen visibility ===
    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }

    // === Core state ===
    var selectedType by remember { mutableStateOf(editTransaction?.type ?: initialType) }
    var confirmedAmount by remember { mutableStateOf<Double?>(null) }
    var selectedAccountId by remember { mutableStateOf(editTransaction?.accountId ?: initialAccountId ?: accounts.firstOrNull()?.id ?: "") }
    var selectedTransferFromId by remember { mutableStateOf(editTransaction?.fromAccountId ?: initialTransferFromId ?: accounts.firstOrNull()?.id ?: "") }
    var selectedTransferToId by remember { mutableStateOf(editTransaction?.toAccountId ?: initialTransferToId ?: accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: "") }
    var note by remember { mutableStateOf(editTransaction?.note ?: initialNote) }
    var selectedDate by remember { mutableStateOf(editTransaction?.timestamp ?: initialTimestamp ?: Calendar.getInstance().timeInMillis) }
    var noteExpanded by remember { mutableStateOf(isEditMode || initialNote.isNotEmpty()) }

    // === Feature state ===
    var anomalyResult by remember { mutableStateOf<AnomalyResult?>(null) }
    var categorySuggestion by remember { mutableStateOf<CategorySuggestion?>(null) }
    var duplicateWarning by remember { mutableStateOf<DuplicateCheckResult?>(null) }
    var pendingEntity by remember { mutableStateOf<TransactionEntity?>(null) }
    var templateNameInput by remember { mutableStateOf("") }
    val pastTransactions = remember { mutableStateOf<List<PastTransactionForSuggestion>>(emptyList()) }

    // === Category state ===
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

    val childCategories = remember(categories, selectedParentCategory) {
        if (selectedType == "TRANSFER") emptyList()
        else selectedParentCategory?.let { parent ->
            categories.filter { it.parentCategoryId == parent.id }
        } ?: emptyList()
    }

    val resolvedCategoryId = if (selectedType == "TRANSFER") null
        else selectedChildCategory?.id ?: selectedParentCategory?.id

    // === Calculator engine ===
    val engine = remember {
        val e = CalculatorEngine()
        val amt = editTransaction?.amount
        if (amt != null) {
            val initStr = if (amt == amt.toLong().toDouble()) amt.toLong().toString() else amt.toString()
            initStr.forEach { c ->
                if (c == '.') e.onDecimalPoint()
                else if (c.isDigit()) e.onDigit(c.digitToInt())
            }
        } else if (initialAmount.isNotEmpty()) {
            initialAmount.forEach { c ->
                if (c == '.') e.onDecimalPoint()
                else if (c.isDigit()) e.onDigit(c.digitToInt())
            }
        }
        e
    }
    var calculatorDisplay by remember { mutableStateOf(engine.displayValue) }

    val isAmountConfirmed = confirmedAmount != null
    val parsedAmount = confirmedAmount ?: engine.displayValue.toDoubleOrNull()
    val isSaveEnabled = (confirmedAmount != null || (engine.displayValue.toDoubleOrNull() != null && engine.displayValue != "0")) &&
        (selectedType != "TRANSFER" || selectedTransferFromId != selectedTransferToId)

    val amountForDisplay = if (isAmountConfirmed) {
        val a = confirmedAmount!!
        if (a == a.toLong().toDouble()) a.toLong().toString() else "%.2f".format(a)
    } else calculatorDisplay

    // === Date formatters ===
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()) }
    val compactDateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val todayCal = remember { Calendar.getInstance() }

    val displayDate = remember(selectedDate) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
        if (isToday) "Today, ${timeFormat.format(Date(selectedDate))}"
        else "${compactDateFormat.format(Date(selectedDate))}, ${timeFormat.format(Date(selectedDate))}"
    }

    // === Side effects ===
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

    LaunchedEffect(selectedType, categories) {
        if (categoryResetSkippedForEdit) {
            categoryResetSkippedForEdit = false
            return@LaunchedEffect
        }
        if (selectedType != "TRANSFER") {
            val parents = categories.filter { it.type == selectedType && it.parentCategoryId == null }
            selectedParentCategory = parents.firstOrNull()
            selectedChildCategory = null
            confirmedAmount = null
        } else {
            selectedParentCategory = null
            selectedChildCategory = null
        }
    }

    // === Save / Update handlers ===
    fun fireCorrection() {
        val suggestedId = categorySuggestion?.categoryId
        if (suggestedId != null && resolvedCategoryId != null && resolvedCategoryId != suggestedId) {
            onCategoryCorrected?.invoke(note, resolvedCategoryId, suggestedId)
        }
    }

    fun doSave(amount: Double) {
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
                onUpdate(updatedEntity)
                fireCorrection()
                hapticManager.fire(HapticEvent.TRANSACTION_SAVED)
                if (selectedType == "EXPENSE" && resolvedCategoryId != null && onAnomalyCheck != null
                    && amount != editTransaction.amount) {
                    val result = onAnomalyCheck(amount, resolvedCategoryId)
                    if (result != null && result.severity != AnomalySeverity.NONE) {
                        anomalyResult = result
                        hapticManager.fire(HapticEvent.ANOMALY_DETECTED)
                        delay(2500)
                    }
                }
                onDismiss()
                anomalyResult = null
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
                if (onCheckDuplicate != null) {
                    val result = onCheckDuplicate(entity)
                    if (result != null) {
                        duplicateWarning = result
                        pendingEntity = entity
                        return@launch
                    }
                }
                onSave(amount, selectedType, resolvedCategoryId, selectedAccountId, note, selectedDate, fromId, toId)
                fireCorrection()
                hapticManager.fire(HapticEvent.TRANSACTION_SAVED)
                if (selectedType == "EXPENSE" && resolvedCategoryId != null && onAnomalyCheck != null) {
                    val result = onAnomalyCheck(amount, resolvedCategoryId)
                    if (result != null && result.severity != AnomalySeverity.NONE) {
                        anomalyResult = result
                        hapticManager.fire(HapticEvent.ANOMALY_DETECTED)
                        delay(2500)
                    }
                }
                onDismiss()
                anomalyResult = null
            }
        }
    }

    // === Back handler ===
    BackHandler {
        when {
            showAccountPicker -> showAccountPicker = false
            showCategoryPicker -> showCategoryPicker = false
            showDateTimePicker -> showDateTimePicker = false
            else -> onDismiss()
        }
    }

    // === Sub-screens ===
    if (showAccountPicker) {
        AccountPickerScreen(
            accounts = accounts,
            selectedAccountId = if (selectedType == "TRANSFER") selectedTransferFromId else selectedAccountId,
            currencyCode = currencyCode,
            onAccountSelected = { id ->
                if (selectedType == "TRANSFER") selectedTransferFromId = id
                else selectedAccountId = id
                showAccountPicker = false
            },
            onBack = { showAccountPicker = false }
        )
        return
    }

    if (showCategoryPicker) {
        CategoryPickerScreen(
            selectedType = selectedType,
            categories = categories,
            selectedCategoryId = resolvedCategoryId,
            accounts = accounts,
            selectedAccountId = selectedTransferToId,
            currencyCode = currencyCode,
            onItemSelected = { id ->
                if (selectedType == "TRANSFER") {
                    selectedTransferToId = id
                } else {
                    val cat = categories.find { it.id == id }
                    if (cat != null) {
                        if (cat.parentCategoryId != null) {
                            selectedParentCategory = categories.find { it.id == cat.parentCategoryId }
                            selectedChildCategory = cat
                        } else {
                            selectedParentCategory = cat
                            selectedChildCategory = null
                        }
                    }
                }
                showCategoryPicker = false
            },
            onBack = { showCategoryPicker = false }
        )
        return
    }

    if (showDateTimePicker) {
        DateTimePickerScreen(
            initialTimestamp = selectedDate,
            onDateTimeSelected = { selectedDate = it; showDateTimePicker = false },
            onBack = { showDateTimePicker = false }
        )
        return
    }

    // === Anomaly banner ===
    AnimatedVisibility(
        visible = anomalyResult != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        anomalyResult?.let { result ->
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

    // === Main content ===
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
    ) {
        // Scrollable area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()))

            // === Title + Back ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = if (isEditMode) "Edit Transaction" else "New Transaction",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // === Templates (non-edit mode) ===
            if (!isEditMode && templates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                        engine.onClear()
                                        val amtStr = if (template.amount == template.amount.toLong().toDouble())
                                            template.amount.toLong().toString() else template.amount.toString()
                                        amtStr.forEach { c ->
                                            if (c == '.') engine.onDecimalPoint()
                                            else if (c.isDigit()) engine.onDigit(c.digitToInt())
                                        }
                                        calculatorDisplay = engine.displayValue
                                        confirmedAmount = null
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
                Spacer(modifier = Modifier.height(12.dp))
            }

            // === Type toggle ===
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
                            .clickable {
                                selectedType = value
                                confirmedAmount = null
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // === Amount display ===
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isAmountConfirmed) {
                        Text(
                            text = currencySymbol,
                            color = Color(0xFF4CAF50),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = amountForDisplay,
                            color = Color(0xFF4CAF50),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = currencySymbol,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = calculatorDisplay,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // === Calculator keypad ===
            CalculatorKeypad(
                engine = engine,
                displayValue = calculatorDisplay,
                onDisplayChanged = { calculatorDisplay = engine.displayValue },
                onConfirm = {
                    val amt = engine.onConfirm()
                    calculatorDisplay = engine.displayValue
                    if (amt != null) {
                        confirmedAmount = amt
                        hapticManager.fire(HapticEvent.NAV_SELECT)
                    }
                },
                context = context,
                hapticManager = hapticManager
            )

            Spacer(modifier = Modifier.height(16.dp))

            // === Note field (expandable) ===
            if (noteExpanded) {
                Column {
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
                                        text = "Record name (optional)",
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Collapse",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { noteExpanded = false }
                        .padding(vertical = 4.dp)
                )
            } else {
                Text(
                    text = "+ Record Name${if (note.isNotEmpty()) ": $note" else " (Optional)"}",
                    color = if (note.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { noteExpanded = true }
                        .padding(vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // === Three pills row ===
            if (selectedType == "TRANSFER") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PickerPill(
                        label = "From",
                        value = accounts.find { it.id == selectedTransferFromId }?.name ?: "Account",
                        modifier = Modifier.weight(1f),
                        onClick = { showAccountPicker = true }
                    )
                    PickerPill(
                        label = "To",
                        value = accounts.find { it.id == selectedTransferToId }?.name ?: "Account",
                        modifier = Modifier.weight(1f),
                        onClick = { showCategoryPicker = true }
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PickerPill(
                        label = "Account",
                        value = accounts.find { it.id == selectedAccountId }?.name ?: "Account",
                        modifier = Modifier.weight(1f),
                        onClick = { showAccountPicker = true }
                    )
                    PickerPill(
                        label = "Category",
                        value = selectedChildCategory?.name ?: selectedParentCategory?.name ?: "Category",
                        modifier = Modifier.weight(1f),
                        onClick = { showCategoryPicker = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            PickerPill(
                label = "Date & Time",
                value = displayDate,
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDateTimePicker = true }
            )

            // === Save as Template link ===
            if (!isEditMode && onSaveAsTemplate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Save as template",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { showSaveTemplateDialog = true }
                        .padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // === Save button (fixed at bottom) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            Button(
                onClick = {
                    val amount = parsedAmount ?: return@Button
                    doSave(amount)
                },
                enabled = isSaveEnabled,
                colors = ButtonDefaults.buttonColors(
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

    // === Dialogs ===

    if (showSaveTemplateDialog) {
        AlertDialog(
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
                TextButton(
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
                TextButton(onClick = {
                    showSaveTemplateDialog = false
                    templateNameInput = ""
                }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    duplicateWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = {
                duplicateWarning = null
                pendingEntity = null
            },
            containerColor = Color(0xFF1B1C1F),
            title = {
                Text("Possible duplicate", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This looks like a ${warning.reason} — save anyway?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val entity = pendingEntity ?: return@TextButton
                    scope.launch {
                        val fromId = if (entity.type == "TRANSFER") entity.fromAccountId else null
                        val toId = if (entity.type == "TRANSFER") entity.toAccountId else null
                        fireCorrection()
                        onSave(entity.amount, entity.type, entity.categoryId, entity.accountId, entity.note, entity.timestamp, fromId, toId)
                        hapticManager.fire(HapticEvent.TRANSACTION_SAVED)
                        if (entity.type == "EXPENSE" && entity.categoryId != null && onAnomalyCheck != null) {
                            val result = onAnomalyCheck(entity.amount, entity.categoryId)
                            if (result != null && result.severity != AnomalySeverity.NONE) {
                                anomalyResult = result
                                hapticManager.fire(HapticEvent.ANOMALY_DETECTED)
                                delay(2500)
                            }
                        }
                        duplicateWarning = null
                        pendingEntity = null
                        onDismiss()
                        anomalyResult = null
                    }
                }) {
                    Text("Save Anyway", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    duplicateWarning = null
                    pendingEntity = null
                }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@Composable
private fun CalculatorKeypad(
    engine: CalculatorEngine,
    displayValue: String,
    onDisplayChanged: () -> Unit,
    onConfirm: () -> Unit,
    context: Context,
    hapticManager: HapticManager
) {
    val buttonTextColor = Color.White
    val operatorColor = Color(0xFFFFA726)
    val functionColor = Color.White.copy(alpha = 0.6f)

    fun pressDigit(d: Int) {
        engine.onDigit(d)
        onDisplayChanged()
    }

    fun pressOperator(op: CalculatorEngine.Operator) {
        engine.onOperator(op)
        onDisplayChanged()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: Clear, Backspace, ±, ÷
        CalculatorRow {
            CalcButton("C", functionColor, onClick = {
                engine.onClear()
                onDisplayChanged()
            })
            CalcButton(
                label = "\u232B",
                color = functionColor,
                onClick = {
                    engine.onBackspace()
                    onDisplayChanged()
                },
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            engine.onClear()
                            onDisplayChanged()
                        },
                        onTap = {
                            engine.onBackspace()
                            onDisplayChanged()
                        }
                    )
                }
            )
            CalcButton("±", functionColor, onClick = {
                engine.toggleSign()
                onDisplayChanged()
            })
            CalcButton("÷", operatorColor, onClick = { pressOperator(CalculatorEngine.Operator.DIVIDE) })
        }

        // Row 2: 7, 8, 9, ×
        CalculatorRow {
            CalcButton("7") { pressDigit(7) }
            CalcButton("8") { pressDigit(8) }
            CalcButton("9") { pressDigit(9) }
            CalcButton("×", operatorColor) { pressOperator(CalculatorEngine.Operator.MULTIPLY) }
        }

        // Row 3: 4, 5, 6, −
        CalculatorRow {
            CalcButton("4") { pressDigit(4) }
            CalcButton("5") { pressDigit(5) }
            CalcButton("6") { pressDigit(6) }
            CalcButton("−", operatorColor) { pressOperator(CalculatorEngine.Operator.SUBTRACT) }
        }

        // Row 4: 1, 2, 3, +
        CalculatorRow {
            CalcButton("1") { pressDigit(1) }
            CalcButton("2") { pressDigit(2) }
            CalcButton("3") { pressDigit(3) }
            CalcButton("+", operatorColor) { pressOperator(CalculatorEngine.Operator.ADD) }
        }

        // Row 5: 0 (wide), ., copy, ✓
        CalculatorRow {
            Box(
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B1C1F))
                    .clickable { pressDigit(0) },
                contentAlignment = Alignment.Center
            ) {
                Text("0", color = buttonTextColor, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            CalcButton(".", onClick = { engine.onDecimalPoint(); onDisplayChanged() })
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B1C1F))
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("Amount", displayValue)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF4CAF50))
                    .clickable { onConfirm() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Confirm amount",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CalculatorRow(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        this.content()
    }
}

@Composable
private fun RowScope.CalcButton(
    label: String,
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .weight(1f)
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1B1C1F))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PickerPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1B1C1F))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Text(
            text = "\u203A",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 18.sp
        )
    }
}
