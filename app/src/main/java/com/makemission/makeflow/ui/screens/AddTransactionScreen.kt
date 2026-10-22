package com.makemission.makeflow.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import com.makemission.makeflow.core.device.LocalReducedMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.rounded.Receipt
import com.makemission.makeflow.core.layout.AdaptiveAlertSheet
import com.makemission.makeflow.core.layout.AdaptiveSheetPrimaryButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.ml.CategoryClassifier
import com.makemission.makeflow.core.ml.NanoReceiptScanner
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.data.local.entity.TransactionTemplateEntity
import com.makemission.makeflow.domain.AnomalyResult
import com.makemission.makeflow.domain.AnomalySeverity
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import com.makemission.makeflow.ui.theme.LocalExpressiveMotion
import com.makemission.makeflow.ui.theme.OverlayTransition
import com.makemission.makeflow.ui.components.FittedAmountText
import com.makemission.makeflow.domain.CalculatorEngine
import com.makemission.makeflow.domain.CategoryLabelResolver
import com.makemission.makeflow.domain.CategorySuggester
import com.makemission.makeflow.domain.CategorySuggestion
import com.makemission.makeflow.domain.AccountSuggester
import com.makemission.makeflow.domain.DuplicateCheckResult
import com.makemission.makeflow.domain.TransactionCommitHandler
import com.makemission.makeflow.domain.TransactionExitGuard
import com.makemission.makeflow.domain.CurrencyConverter
import com.makemission.makeflow.domain.CurrencyRate
import com.makemission.makeflow.domain.CategoryUsage
import com.makemission.makeflow.domain.CategoryUsageRanker
import com.makemission.makeflow.domain.PastTransactionForSuggestion
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.math.BigDecimal
import com.makemission.makeflow.R

private const val ML_CONFIDENCE_THRESHOLD = 0.6f

@Composable
fun AddTransactionScreen(
    onDismiss: () -> Unit,
    onSave: suspend (amount: Double, type: String, categoryId: String?, accountId: String, note: String, timestamp: Long, fromAccountId: String?, toAccountId: String?, destinationAmount: Double?) -> Unit,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencyCode: String,
    exchangeRates: List<ExchangeRateEntity> = emptyList(),
    hapticManager: HapticManager,
    templates: List<TransactionTemplateEntity> = emptyList(),
    onSaveAsTemplate: ((name: String, amount: Double, type: String, categoryId: String?, accountId: String, note: String) -> Unit)? = null,
    onAnomalyCheck: (suspend (Double, String?) -> AnomalyResult?)? = null,
    onGetPastTransactions: (suspend () -> List<PastTransactionForSuggestion>)? = null,
    onGetCategoryUsages: (suspend () -> List<CategoryUsage>)? = null,
    onGetUsualAmounts: (suspend (accountId: String, type: String, categoryId: String?) -> List<Double>)? = null,
    onCheckDuplicate: (suspend (TransactionEntity) -> DuplicateCheckResult?)? = null,
    categoryClassifier: CategoryClassifier? = null,
    smartCategorizationEnabled: Boolean = true,
    showSaveAndAddAnother: Boolean = true,
    correctionMemory: Map<String, String> = emptyMap(),
    onCategoryCorrected: ((description: String, correctedCategoryId: String, originalSuggestedCategoryId: String?) -> Unit)? = null,
    initialType: String = "EXPENSE",
    initialAmount: String = "",
    initialNote: String = "",
    initialTimestamp: Long? = null,
    initialAccountId: String? = null,
    initialTransferFromId: String? = null,
    initialTransferToId: String? = null,
    editTransaction: TransactionEntity? = null,
    onUpdate: (suspend (TransactionEntity) -> Unit)? = null,
    mpesaTransactionCost: Double? = null,
    mpesaAiAssisted: Boolean = false,
    logMpesaCosts: Boolean = true
) {
    val dimens = LocalAppDimens.current
    val layoutMode = LocalDeviceLayoutMode.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isRepeatMode by rememberSaveable(editTransaction?.id) { mutableStateOf(false) }
    val isEditMode = editTransaction != null && !isRepeatMode

    // === Sub-screen visibility ===
    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    // === Core state ===
    var selectedType by remember { mutableStateOf(editTransaction?.type ?: initialType) }
    var confirmedAmount by remember { mutableStateOf<Double?>(null) }
    var selectedAccountId by remember { mutableStateOf(editTransaction?.accountId ?: initialAccountId ?: accounts.firstOrNull()?.id ?: "") }
    var selectedTransferFromId by remember { mutableStateOf(editTransaction?.fromAccountId ?: initialTransferFromId ?: accounts.firstOrNull()?.id ?: "") }
    var selectedTransferToId by remember { mutableStateOf(editTransaction?.toAccountId ?: initialTransferToId ?: accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: "") }
    var note by remember { mutableStateOf(editTransaction?.note ?: initialNote) }
    var selectedDate by rememberSaveable(editTransaction?.id) {
        mutableStateOf(editTransaction?.timestamp ?: initialTimestamp ?: Calendar.getInstance().timeInMillis)
    }
    var noteExpanded by remember { mutableStateOf(isEditMode || initialNote.isNotEmpty()) }

    // === Feature state ===
    var categorySuggestion by remember { mutableStateOf<CategorySuggestion?>(null) }
    var categorySuggestionSkippedForTemplate by remember { mutableStateOf<Pair<String, String>?>(null) }
    val commitHandler = remember(onCheckDuplicate, onAnomalyCheck, hapticManager) {
        TransactionCommitHandler(onCheckDuplicate, onAnomalyCheck, hapticManager)
    }
    var templateNameInput by remember { mutableStateOf("") }
    var isScanningReceipt by remember { mutableStateOf(false) }
    var receiptScanMessage by remember { mutableStateOf<String?>(null) }
    var receiptSuggestion by remember { mutableStateOf<NanoReceiptScanner.ReceiptSuggestion?>(null) }
    val pastTransactions = remember { mutableStateOf<List<PastTransactionForSuggestion>>(emptyList()) }
    val categoryUsages = remember { mutableStateOf<List<CategoryUsage>>(emptyList()) }
    var addAnotherAfterSave by remember { mutableStateOf(false) }
    var usualAmounts by remember { mutableStateOf<List<Double>>(emptyList()) }
    val usualAccount = remember(note, selectedType, selectedAccountId, isEditMode, accounts, pastTransactions.value) {
        if (isEditMode || selectedType == "TRANSFER") null
        else AccountSuggester.suggest(
            inputDescription = note,
            pastTransactions = pastTransactions.value,
            availableAccountIds = accounts.mapTo(mutableSetOf()) { it.id }
        )?.takeUnless { it.accountId == selectedAccountId }
            ?.let { suggestion -> accounts.firstOrNull { it.id == suggestion.accountId } }
    }

    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isScanningReceipt = true
                receiptScanMessage = null
                receiptSuggestion = null
                val bitmap = withContext(Dispatchers.IO) { decodeReceiptBitmap(context, uri) }
                if (bitmap == null) {
                    receiptScanMessage = "Couldn't open that photo. You can still enter the transaction manually."
                } else {
                    val result = try {
                        NanoReceiptScanner.suggest(bitmap)
                    } finally {
                        bitmap.recycle()
                    }
                    when (result) {
                        is NanoReceiptScanner.Result.Suggested -> receiptSuggestion =
                            NanoReceiptScanner.ReceiptSuggestion(result.amount, result.merchant)
                        NanoReceiptScanner.Result.Unavailable -> receiptScanMessage =
                            "Gemini Nano isn't available on this device right now. You can still enter the transaction manually."
                        NanoReceiptScanner.Result.Unreadable -> receiptScanMessage =
                            "Couldn't read a clear total from that receipt. You can still enter it manually."
                    }
                }
                isScanningReceipt = false
            }
        }
    }

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
    val frequentCategories = remember(categoryUsages.value, categories, selectedType) {
        CategoryUsageRanker.rank(categoryUsages.value)[selectedType].orEmpty()
            .take(4)
            .mapNotNull { ranked -> categories.find { it.id == ranked.categoryId } }
    }
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
    var calculatorRevision by remember { mutableIntStateOf(0) }
    val calculatorExpression = remember(calculatorRevision) { engine.expression }

    fun resetForNextTransaction() {
        engine.onClear()
        calculatorDisplay = engine.displayValue
        calculatorRevision++
        confirmedAmount = null
        note = ""
        noteExpanded = false
        selectedDate = Calendar.getInstance().timeInMillis
        categorySuggestion = null
        commitHandler.clearAlerts()
    }

    val isAmountConfirmed = confirmedAmount != null
    val parsedAmount = confirmedAmount ?: engine.displayValue.toDoubleOrNull()
    val fromAccountForAmount = accounts.firstOrNull {
        it.id == if (selectedType == "TRANSFER") selectedTransferFromId else selectedAccountId
    }
    val toAccountForAmount = accounts.firstOrNull { it.id == selectedTransferToId }
    val amountCurrencyCode = fromAccountForAmount?.currencyCode ?: currencyCode
    val destinationCurrencyCode = toAccountForAmount?.currencyCode ?: currencyCode
    val conversionRates = exchangeRates.mapNotNull { rate ->
        rate.baseCurrencyCode?.let { base ->
            CurrencyRate(rate.currencyCode, base, rate.rateToDisplayCurrency, rate.rateDate)
        }
    }
    val transferDestinationAmount = if (selectedType == "TRANSFER" && parsedAmount != null) {
        CurrencyConverter.convert(parsedAmount, amountCurrencyCode, destinationCurrencyCode, conversionRates)
    } else null
    val isSaveEnabled = (confirmedAmount != null || (!engine.hasUnconfirmedOperation && engine.displayValue.toDoubleOrNull()?.let { it > 0 } == true)) &&
        (parsedAmount ?: 0.0) > 0 && fromAccountForAmount != null &&
        (selectedType != "TRANSFER" || (toAccountForAmount != null && selectedTransferFromId != selectedTransferToId && transferDestinationAmount != null))

    val hasUnsavedChanges = TransactionExitGuard.hasUnsavedChanges(
        current = TransactionExitGuard.FormState(
            amountInput = confirmedAmount?.toString() ?: calculatorDisplay,
            type = selectedType,
            categoryId = resolvedCategoryId,
            accountId = selectedAccountId,
            note = note,
            timestamp = selectedDate,
            fromAccountId = selectedTransferFromId,
            toAccountId = selectedTransferToId,
            hasPendingCalculation = engine.hasUnconfirmedOperation
        ),
        original = if (isEditMode) editTransaction else null
    )
    fun dismissForm() {
        if (isExiting) return
        isExiting = true
        onDismiss()
    }
    fun requestDismiss() {
        if (isExiting) return
        if (hasUnsavedChanges) {
            hapticManager.fire(HapticEvent.NAV_SELECT)
            showDiscardConfirmation = true
        } else {
            dismissForm()
        }
    }

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
        if (onGetCategoryUsages != null) {
            categoryUsages.value = onGetCategoryUsages()
        }
    }

    LaunchedEffect(selectedType, selectedAccountId, resolvedCategoryId, onGetUsualAmounts) {
        usualAmounts = onGetUsualAmounts?.invoke(selectedAccountId, selectedType, resolvedCategoryId).orEmpty()
    }

    LaunchedEffect(note, selectedType, smartCategorizationEnabled, correctionMemory, categories, pastTransactions.value) {
        if (categorySuggestionSkippedForTemplate == (note to selectedType)) {
            categorySuggestionSkippedForTemplate = null
            categorySuggestion = null
            return@LaunchedEffect
        }
        categorySuggestionSkippedForTemplate = null
        if (editTransaction != null && note == editTransaction.note && selectedType == editTransaction.type) {
            categorySuggestion = null
            return@LaunchedEffect
        }
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

    fun completeSave() {
        if (addAnotherAfterSave) {
            addAnotherAfterSave = false
            resetForNextTransaction()
        } else {
            dismissForm()
        }
    }

    fun applyTemplate(template: TransactionTemplateEntity) {
        engine.onClear()
        val amount = if (template.amount == template.amount.toLong().toDouble()) {
            template.amount.toLong().toString()
        } else {
            template.amount.toString()
        }
        amount.forEach { digit ->
            if (digit == '.') engine.onDecimalPoint() else if (digit.isDigit()) engine.onDigit(digit.digitToInt())
        }
        calculatorDisplay = engine.displayValue
        calculatorRevision++
        confirmedAmount = null

        val changingType = selectedType != template.type
        if (changingType) categoryResetSkippedForEdit = true
        selectedType = template.type
        selectedAccountId = if (accounts.any { it.id == template.accountId }) template.accountId else selectedAccountId
        val templateCategory = categories.find { it.id == template.categoryId && it.type == template.type }
            ?: categories.firstOrNull { it.type == template.type && it.parentCategoryId == null }
        selectedParentCategory = when {
            templateCategory == null -> null
            templateCategory.parentCategoryId == null -> templateCategory
            else -> categories.find { it.id == templateCategory.parentCategoryId }
        }
        selectedChildCategory = templateCategory?.takeIf { it.parentCategoryId != null }
        categorySuggestion = null
        categorySuggestionSkippedForTemplate = template.note to template.type
        note = template.note
        noteExpanded = template.note.isNotEmpty()
        hapticManager.fire(HapticEvent.NAV_SELECT)
    }

    fun doSave(amount: Double, addAnother: Boolean = false) {
        addAnotherAfterSave = addAnother
        scope.launch {
            val fromId = if (selectedType == "TRANSFER") selectedTransferFromId else null
            val toId = if (selectedType == "TRANSFER") selectedTransferToId else null
            val accountId = fromId ?: selectedAccountId
            if (isEditMode && onUpdate != null) {
                val updatedEntity = editTransaction.copy(
                    amount = amount,
                    type = selectedType,
                    categoryId = resolvedCategoryId,
                    accountId = accountId,
                    fromAccountId = fromId,
                    toAccountId = toId,
                    note = note,
                    timestamp = selectedDate,
                    destinationAmount = transferDestinationAmount
                )
                commitHandler.update(updatedEntity, editTransaction, onUpdate)
                fireCorrection()
                dismissForm()
            } else {
                val entity = TransactionEntity(
                    amount = amount,
                    type = selectedType,
                    categoryId = resolvedCategoryId,
                    accountId = accountId,
                    fromAccountId = fromId,
                    toAccountId = toId,
                    note = note,
                    timestamp = selectedDate,
                    destinationAmount = transferDestinationAmount
                )
                commitHandler.prepareSave(
                    entity = entity,
                    onSave = { saved ->
                        onSave(saved.amount, saved.type, saved.categoryId, saved.accountId, saved.note, saved.timestamp, saved.fromAccountId, saved.toAccountId, saved.destinationAmount)
                        fireCorrection()
                    },
                    onComplete = { completeSave() }
                )
            }
        }
    }

    // === Back handler ===
    BackHandler {
        when {
            showAccountPicker -> showAccountPicker = false
            showCategoryPicker -> showCategoryPicker = false
            showDateTimePicker -> showDateTimePicker = false
            else -> requestDismiss()
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
            categoryUsages = categoryUsages.value,
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
    val reducedMotion = LocalReducedMotion.current
    AnimatedVisibility(
        visible = commitHandler.anomalyResult != null,
        enter = if (reducedMotion) {
            fadeIn(OverlayTransition.reducedEnter)
        } else {
            slideInVertically(
                animationSpec = OverlayTransition.enterSlide,
                initialOffsetY = { -(it * OverlayTransition.ValueShiftFraction).toInt() }
            ) + fadeIn(OverlayTransition.enterFade)
        },
        exit = if (reducedMotion) {
            fadeOut(OverlayTransition.reducedExit)
        } else {
            slideOutVertically(
                animationSpec = OverlayTransition.exitSlide,
                targetOffsetY = { -(it * OverlayTransition.ValueShiftFraction).toInt() }
            ) + fadeOut(OverlayTransition.exitFade)
        }
    ) {
        commitHandler.anomalyResult?.let { result ->
            val categoryName = selectedChildCategory?.name ?: selectedParentCategory?.name ?: "this category"
            val message = if (result.severity == AnomalySeverity.HIGH) {
                "This is significantly higher than your usual $categoryName spend (avg ${CurrencyFormatter.format(result.categoryAverage, currencyCode)})"
            } else {
                "This is higher than your usual $categoryName spend"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerSmall))
                    .padding(dimens.spacingLarge)
            ) {
                Text(
                    text = message,
                    color = LocalMakeFlowColors.current.colorWarning,
                    fontSize = dimens.textSm,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // === Main content ===
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Scrollable area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .widthIn(max = dimens.navbarMaxWidth)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = dimens.paddingMedium)
        ) {
            Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()))

            // === Title + Back ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { requestDismiss() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = when {
                            isRepeatMode -> stringResource(R.string.add_log_again)
                            isEditMode -> stringResource(R.string.add_edit_title)
                            else -> stringResource(R.string.add_new_title)
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingMedium))

            if (isEditMode) {
                OutlinedButton(
                    onClick = {
                        hapticManager.fire(HapticEvent.NAV_SELECT)
                        selectedDate = Calendar.getInstance().timeInMillis
                        commitHandler.clearAlerts()
                        isRepeatMode = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(dimens.iconSizeSmall)
                    )
                    Spacer(Modifier.width(dimens.spacingMedium))
                    Text(
                        text = stringResource(R.string.add_log_again),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.height(dimens.spacingMedium))
            }

            if (mpesaAiAssisted) {
                Text(
                    text = "On-device AI suggestion. Check the amount, date, and account before saving.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(dimens.shapeCornerSmall)
                        )
                        .padding(dimens.spacingMedium)
                )
                Spacer(modifier = Modifier.height(dimens.spacingMedium))
            }

            if (!isEditMode && !isRepeatMode) {
                OutlinedButton(
                    onClick = {
                        hapticManager.fire(HapticEvent.NAV_SELECT)
                        receiptPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !isScanningReceipt,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isScanningReceipt) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(dimens.iconSizeSmall),
                            strokeWidth = dimens.progressTrackHeight
                        )
                    } else {
                        Icon(Icons.Rounded.Receipt, contentDescription = null)
                    }
                    Spacer(Modifier.width(dimens.spacingMedium))
                    Text(if (isScanningReceipt) "Reading receipt" else "Scan receipt")
                }
                Text(
                    text = "Choose a photo. It stays on this device and is not saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                receiptScanMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(dimens.spacingMedium))
            }

            // === Templates (non-edit mode) ===
            if (!isEditMode && templates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
                    if (layoutMode == DeviceLayoutMode.EXPANDED) {
                        templates.chunked(2).forEach { templateRow ->
                            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                                templateRow.forEach { template ->
                                    val templateCurrency = accounts.firstOrNull { it.id == template.accountId }
                                        ?.currencyCode ?: currencyCode
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable { applyTemplate(template) }
                                            .padding(dimens.spacingLarge),
                                        verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
                                    ) {
                                        Text(
                                            text = template.name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(template.amount, templateCurrency),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                if (templateRow.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    } else {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                            templates.forEach { template ->
                                Text(
                                    text = template.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = dimens.textSm,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.cornerRadiusMedium))
                                        .clickable { applyTemplate(template) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(dimens.spacingLarge))
            }

            // === Type toggle ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.segmentedControlHeight + dimens.segmentedControlInset + dimens.segmentedControlInset)
                    .clip(RoundedCornerShape(dimens.shapeCornerPill))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(dimens.segmentedControlInset),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
            ) {
                listOf(
                    stringResource(R.string.add_expense) to "EXPENSE",
                    stringResource(R.string.add_income) to "INCOME",
                    stringResource(R.string.add_transfer) to "TRANSFER"
                ).forEach { (label, value) ->
                    val isSelected = selectedType == value
                    val segmentBackground by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        animationSpec = if (reducedMotion) OverlayTransition.reducedColor
                            else LocalExpressiveMotion.current.colorAnimationSpec,
                        label = "transactionTypeBackground"
                    )
                    val segmentContent by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = if (reducedMotion) OverlayTransition.reducedColor
                            else LocalExpressiveMotion.current.colorAnimationSpec,
                        label = "transactionTypeContent"
                    )
                    Text(
                        text = label,
                        color = segmentContent,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(dimens.shapeCornerPill))
                            .background(
                                segmentBackground,
                                RoundedCornerShape(dimens.shapeCornerPill)
                            )
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = {
                                    selectedType = value
                                    confirmedAmount = null
                                    hapticManager.fire(HapticEvent.TOGGLE)
                                }
                            )
                            .padding(horizontal = dimens.spacingSmall)
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingXXLarge))

            // === Amount display ===
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.cornerRadiusLarge))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = dimens.paddingLarge, vertical = dimens.paddingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
                ) {
                    calculatorExpression?.let { expression ->
                        Text(
                            text = expression,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    FittedAmountText(
                        text = CurrencyFormatter.format(amountForDisplay.toDoubleOrNull() ?: 0.0, amountCurrencyCode),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingLarge))

            if (selectedType == "TRANSFER") {
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium), modifier = Modifier.fillMaxWidth()) {
                    PickerChip("${stringResource(R.string.add_from)} · ${fromAccountForAmount?.name ?: stringResource(R.string.add_account)} · $amountCurrencyCode", Modifier.weight(1f)) { showAccountPicker = true }
                    PickerChip("${stringResource(R.string.add_to)} · ${toAccountForAmount?.name ?: stringResource(R.string.add_account)} · $destinationCurrencyCode", Modifier.weight(1f)) { showCategoryPicker = true }
                }
                val receivedAmount = transferDestinationAmount
                val transferMessage = when {
                    receivedAmount == null -> "A rate for $amountCurrencyCode to $destinationCurrencyCode is unavailable. Refresh exchange rates to continue."
                    amountCurrencyCode == destinationCurrencyCode -> "The destination account receives the same amount."
                    else -> "Estimated to receive ${CurrencyFormatter.format(receivedAmount, destinationCurrencyCode)} using the latest saved rate."
                }
                Text(
                    text = transferMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (receivedAmount == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (layoutMode == DeviceLayoutMode.EXPANDED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.add_transfer_balance,
                                stringResource(R.string.add_from),
                                CurrencyFormatter.format(fromAccountForAmount?.balance ?: 0.0, amountCurrencyCode)
                            ),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.add_transfer_balance,
                                stringResource(R.string.add_to),
                                CurrencyFormatter.format(toAccountForAmount?.balance ?: 0.0, destinationCurrencyCode)
                            ),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val categoryName = selectedChildCategory?.name ?: selectedParentCategory?.name ?: stringResource(R.string.add_category)
                val isSmartPick = resolvedCategoryId != null && categorySuggestion?.categoryId == resolvedCategoryId
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium), modifier = Modifier.fillMaxWidth()) {
                    PickerChip(
                        text = if (isSmartPick) "$categoryName · ${stringResource(R.string.add_smart_pick)}" else categoryName,
                        modifier = Modifier.weight(1f),
                        emphasized = true
                    ) {
                        showCategoryPicker = true
                    }
                    PickerChip("${accounts.find { it.id == selectedAccountId }?.name ?: stringResource(R.string.add_account)} · $amountCurrencyCode", Modifier.weight(1f)) { showAccountPicker = true }
                }
                if (layoutMode == DeviceLayoutMode.EXPANDED) {
                    Text(
                        text = stringResource(
                            R.string.add_available_balance,
                            CurrencyFormatter.format(fromAccountForAmount?.balance ?: 0.0, amountCurrencyCode)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (frequentCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(dimens.spacingMedium))
                    Text(
                        text = stringResource(R.string.add_frequent_categories),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
                    ) {
                        frequentCategories.forEach { category ->
                            PickerChip(
                                text = category.name,
                                modifier = Modifier.testTag("frequent_category_${category.id}"),
                                emphasized = category.id == resolvedCategoryId,
                                onClick = {
                                    selectedParentCategory = if (category.parentCategoryId == null) category
                                    else categories.find { it.id == category.parentCategoryId }
                                    selectedChildCategory = category.takeIf { it.parentCategoryId != null }
                                    categorySuggestion = null
                                    hapticManager.fire(HapticEvent.NAV_SELECT)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingLarge))
            if (selectedType != "TRANSFER") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                    Text(
                        text = stringResource(R.string.add_usual),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
                        (usualAmounts.distinct().take(4).ifEmpty { listOf(100.0, 200.0, 500.0, 1000.0) }).forEach { value ->
                            Text(
                                text = CurrencyFormatter.formatNumber(value, 0),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(dimens.shapeCornerPill))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable {
                                        engine.onClear()
                                        val amount = value.toLong().toString()
                                        amount.forEach { digit -> engine.onDigit(digit.digitToInt()) }
                                        calculatorDisplay = engine.displayValue
                                        confirmedAmount = null
                                        calculatorRevision++
                                        hapticManager.fire(HapticEvent.NAV_SELECT)
                                    }
                                    .padding(horizontal = dimens.paddingMedium, vertical = dimens.spacingMedium)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(dimens.spacingSmall))
            }

            Spacer(modifier = Modifier.height(dimens.spacingSmall))

            // === Calculator keypad ===
            CalculatorKeypad(
                engine = engine,
                onDisplayChanged = {
                    calculatorDisplay = engine.displayValue
                    confirmedAmount = null
                    calculatorRevision++
                },
                onConfirm = {
                    val amt = engine.onConfirm()
                    calculatorDisplay = engine.displayValue
                    if (amt != null) {
                        confirmedAmount = amt
                        hapticManager.fire(HapticEvent.NAV_SELECT)
                    }
                },
                hapticManager = hapticManager
            )

            Spacer(modifier = Modifier.height(dimens.spacingXLarge))

            // === Note field (expandable) ===
            if (noteExpanded) {
                Column {
                    BasicTextField(
                        value = note,
                        onValueChange = { note = it },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textM),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            Box {
                                if (note.isEmpty()) {
                                    Text(
                                        text = "Record name (optional)",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        fontSize = dimens.textM
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        thickness = 0.5.dp
                    )
                }
                Spacer(modifier = Modifier.height(dimens.spacingXSmall))
                Text(
                    text = "Collapse",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = dimens.textS,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { noteExpanded = false }
                        .padding(vertical = 4.dp)
                )
            } else {
                Text(
                    text = "+ Record Name${if (note.isNotEmpty()) ": $note" else " (Optional)"}",
                    color = if (note.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = dimens.textM,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { noteExpanded = true }
                        .padding(vertical = 12.dp)
                )
            }

            AnimatedVisibility(
                visible = usualAccount != null,
                enter = if (reducedMotion) {
                    fadeIn(OverlayTransition.reducedEnter)
                } else {
                    expandVertically(OverlayTransition.contentSizeChange) + fadeIn(OverlayTransition.fastEnter)
                },
                exit = if (reducedMotion) {
                    fadeOut(OverlayTransition.reducedExit)
                } else {
                    shrinkVertically(OverlayTransition.contentSizeChange) + fadeOut(OverlayTransition.fastExit)
                }
            ) {
                usualAccount?.let { account ->
                    AssistChip(
                        onClick = {
                            selectedAccountId = account.id
                            hapticManager.fire(HapticEvent.TOGGLE)
                        },
                        label = {
                            Text(stringResource(R.string.add_use_usual_account, account.name))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("usual_account_suggestion")
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingLarge))

            PickerPill(
                label = "Date & Time",
                value = displayDate,
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDateTimePicker = true }
            )

            // === M-PESA fee note ===
            val showFeeNote = mpesaTransactionCost != null && mpesaTransactionCost > 0 && logMpesaCosts
            if (showFeeNote) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerSmall))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "+ ${CurrencyFormatter.format(mpesaTransactionCost!!, currencyCode)} fee will also be logged separately",
                        color = LocalMakeFlowColors.current.colorSuccess,
                        fontSize = dimens.textSm,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // === Save as Template link ===
            if (!isEditMode && onSaveAsTemplate != null) {
                Spacer(modifier = Modifier.height(dimens.spacingMedium))
                Text(
                    text = "Save as template",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = dimens.textSm,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { showSaveTemplateDialog = true }
                        .padding(vertical = dimens.spacingMedium)
                )
            }

            Spacer(modifier = Modifier.height(dimens.spacingXLarge))
        }

        // === Save button (fixed at bottom) ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = dimens.navbarMaxWidth)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = dimens.paddingMedium)
                .padding(bottom = dimens.paddingMedium + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
            ) {
                Button(
                    onClick = {
                        val amount = parsedAmount ?: return@Button
                        doSave(amount)
                    },
                    enabled = isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(dimens.shapeCornerPill),
                    modifier = Modifier
                        .weight(if (!isEditMode && !isRepeatMode && showSaveAndAddAnother) 1.15f else 1f)
                        .height(dimens.primaryButtonHeight)
                ) {
                    Text(
                        text = if (isEditMode) stringResource(R.string.add_update) else stringResource(R.string.add_save),
                        fontSize = dimens.textL,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!isEditMode && !isRepeatMode && showSaveAndAddAnother) {
                    OutlinedButton(
                        onClick = {
                            val amount = parsedAmount ?: return@OutlinedButton
                            hapticManager.fire(HapticEvent.NAV_SELECT)
                            doSave(amount, addAnother = true)
                        },
                        enabled = isSaveEnabled,
                        shape = RoundedCornerShape(dimens.shapeCornerPill),
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.primaryButtonHeight)
                    ) {
                        Text(
                            text = stringResource(R.string.add_save_another),
                            fontSize = dimens.textSm,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // === Dialogs ===

    if (showDiscardConfirmation) {
        UnsavedTransactionSheet(
            hapticManager = hapticManager,
            onKeepEditing = { showDiscardConfirmation = false },
            onDiscard = {
                showDiscardConfirmation = false
                dismissForm()
            }
        )
    }

    if (showSaveTemplateDialog) {
        AdaptiveAlertSheet(
            onDismissRequest = {
                showSaveTemplateDialog = false
                templateNameInput = ""
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("Save as template", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Name",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    BasicTextField(
                        value = templateNameInput,
                        onValueChange = { templateNameInput = it },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textL),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            Box {
                                if (templateNameInput.isEmpty()) {
                                    Text(
                                        text = "e.g. Monthly Rent",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        fontSize = dimens.textL
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(dimens.shapeCornerSmall))
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
                    Text("Save", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveTemplateDialog = false
                    templateNameInput = ""
                }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    commitHandler.duplicateWarning?.let { warning ->
        AdaptiveAlertSheet(
            onDismissRequest = {
                commitHandler.clearAlerts()
                addAnotherAfterSave = false
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("Possible duplicate", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This looks like a ${warning.reason} — save anyway?",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = dimens.textM
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val entity = commitHandler.pendingEntity ?: return@TextButton
                    scope.launch {
                        commitHandler.saveIgnoringDuplicate(
                            entity = entity,
                            onSave = { saved ->
                                onSave(saved.amount, saved.type, saved.categoryId, saved.accountId, saved.note, saved.timestamp, saved.fromAccountId, saved.toAccountId, saved.destinationAmount)
                                fireCorrection()
                            },
                            onComplete = { completeSave() }
                        )
                    }
                }) {
                    Text("Save Anyway", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    commitHandler.clearAlerts()
                    addAnotherAfterSave = false
                }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    receiptSuggestion?.let { suggestion ->
        AdaptiveAlertSheet(
            onDismissRequest = { receiptSuggestion = null },
            confirmButton = {
                AdaptiveSheetPrimaryButton(
                    label = "Use these details",
                    onClick = {
                        selectedType = "EXPENSE"
                        engine.onClear()
                        BigDecimal.valueOf(suggestion.amount)
                            .stripTrailingZeros()
                            .toPlainString()
                            .forEach { char ->
                                if (char == '.') engine.onDecimalPoint()
                                else if (char.isDigit()) engine.onDigit(char.digitToInt())
                            }
                        calculatorDisplay = engine.displayValue
                        confirmedAmount = null
                        calculatorRevision++
                        suggestion.merchant?.let { merchant ->
                            note = merchant
                            noteExpanded = true
                        }
                        receiptSuggestion = null
                    },
                    hapticManager = hapticManager
                )
            },
            dismissButton = {
                TextButton(onClick = { receiptSuggestion = null }) {
                    Text("Not now")
                }
            },
            icon = {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Check the receipt details", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
                    Text(
                        CurrencyFormatter.format(suggestion.amount, amountCurrencyCode),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        suggestion.merchant ?: "Merchant not recognized",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Verify the total and account currency before saving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

private fun decodeReceiptBitmap(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null
    } catch (_: Exception) {
        return null
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
    var sampleSize = 1
    while (longestEdge / sampleSize > 1600) sampleSize *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun CalculatorKeypad(
    engine: CalculatorEngine,
    onDisplayChanged: () -> Unit,
    onConfirm: () -> Unit,
    hapticManager: HapticManager
) {
    val dimens = LocalAppDimens.current
    val operatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val functionColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    fun pressDigit(d: Int) {
        engine.onDigit(d)
        onDisplayChanged()
        hapticManager.fire(HapticEvent.NAV_SELECT)
    }

    fun pressOperator(op: CalculatorEngine.Operator) {
        engine.onOperator(op)
        onDisplayChanged()
        hapticManager.fire(HapticEvent.NAV_SELECT)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(CalculatorEngine.Operator.ADD, CalculatorEngine.Operator.SUBTRACT,
                CalculatorEngine.Operator.MULTIPLY, CalculatorEngine.Operator.DIVIDE).forEach { op ->
                Text(op.symbol.toString(), color = operatorColor, fontSize = dimens.textXl,
                    modifier = Modifier.clickable { pressOperator(op) }.padding(horizontal = dimens.spacingMedium, vertical = dimens.spacingSmall))
            }
            Text("=", color = MaterialTheme.colorScheme.primary, fontSize = dimens.textXl, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onConfirm).padding(horizontal = dimens.spacingMedium, vertical = dimens.spacingSmall))
        }
        // Digits follow the familiar keypad order.
        CalculatorRow {
            CalcButton("1") { pressDigit(1) }
            CalcButton("2") { pressDigit(2) }
            CalcButton("3") { pressDigit(3) }
        }

        CalculatorRow {
            CalcButton("4") { pressDigit(4) }
            CalcButton("5") { pressDigit(5) }
            CalcButton("6") { pressDigit(6) }
        }

        CalculatorRow {
            CalcButton("7") { pressDigit(7) }
            CalcButton("8") { pressDigit(8) }
            CalcButton("9") { pressDigit(9) }
        }

        // Row 5: decimal, zero, backspace
        CalculatorRow {
            CalcButton(".", onClick = {
                engine.onDecimalPoint()
                onDisplayChanged()
                hapticManager.fire(HapticEvent.NAV_SELECT)
            })
            CalcButton("0") { pressDigit(0) }
            CalcButton("⌫", functionColor, onLongClick = { engine.onClear(); onDisplayChanged() }) {
                engine.onBackspace()
                onDisplayChanged()
                hapticManager.fire(HapticEvent.NAV_SELECT)
            }
        }
    }
}

@Composable
private fun CalculatorRow(
    content: @Composable RowScope.() -> Unit
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
    ) {
        this.content()
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RowScope.CalcButton(
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val dimens = LocalAppDimens.current
    Box(
        modifier = Modifier
            .weight(1f)
            .height(dimens.primaryButtonHeight)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = dimens.textDisplay,
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
    val dimens = LocalAppDimens.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = dimens.textM,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Text(
            text = "\u203A",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            fontSize = 18.sp
        )
    }
}

@Composable
private fun PickerChip(
    text: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    onClick: () -> Unit
) {
    val dimens = LocalAppDimens.current
    Text(
        text = text,
        color = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        fontSize = dimens.textSm,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(dimens.shapeCornerPill))
            .background(if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.paddingMedium, vertical = dimens.spacingLarge)
    )
}
