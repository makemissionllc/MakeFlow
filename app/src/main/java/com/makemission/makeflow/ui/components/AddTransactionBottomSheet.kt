@file:OptIn(ExperimentalMaterial3Api::class)
package com.makemission.makeflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.viewmodel.CategoriesViewModel
import com.makemission.makeflow.ui.haptic.lightTick
import com.makemission.makeflow.ui.haptic.mediumConfirm
import com.makemission.makeflow.ui.haptic.strongPress
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val INCOME_GLOW = Color(0xFF1B5E20)
private val EXPENSE_GLOW = Color(0xFF4A1A1A)

@Composable
fun AddTransactionBottomSheet(
    viewModel: CategoriesViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categories = viewModel.uiState.value.categories
    val dimens = LocalAppDimens.current
    var rawInput by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("EXPENSE") }
    var note by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    val amount = parseAmount(rawInput)
    val isFormValid = amount > 0.0 && selectedCategoryId != null

    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCategoryId == null) {
            val first = categories.firstOrNull { it.name != "Income" }
            selectedCategoryId = first?.id ?: categories.first().id
        }
    }

    val isIncome = transactionType == "INCOME"
    val glowColor by animateColorAsState(
        targetValue = if (isIncome) INCOME_GLOW else EXPENSE_GLOW,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sheetGlow"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = dimens.cornerRadiusXL, topEnd = dimens.cornerRadiusXL),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(400)) +
                    slideInVertically(
                        animationSpec = tween(400),
                        initialOffsetY = { it / 4 }
                    )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .graphicsLayer {
                        shadowElevation = 8f
                        this.clip = true
                        shape = RoundedCornerShape(topStart = 40f, topEnd = 40f)
                    }
                    .background(glowColor.copy(alpha = 0.06f))
                    .padding(bottom = dimens.paddingExtra)
            ) {
                AmountDisplay(rawInput = rawInput)
                Spacer(modifier = Modifier.height(dimens.paddingMedium))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.paddingLarge),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExpressiveChoiceChip(
                        label = "Expense",
                        icon = Icons.Filled.ArrowDownward,
                        isSelected = transactionType == "EXPENSE",
                        onClick = { transactionType = "EXPENSE" },
                        activeColor = MaterialTheme.colorScheme.errorContainer,
                        onActiveColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    ExpressiveChoiceChip(
                        label = "Income",
                        icon = Icons.Filled.ArrowUpward,
                        isSelected = transactionType == "INCOME",
                        onClick = { transactionType = "INCOME" },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(dimens.paddingSmall))
                ExpressiveInputField(
                    value = note,
                    onValueChange = { note = it },
                    hint = "Add a note...",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.padding(horizontal = dimens.paddingLarge)
                )
                Spacer(modifier = Modifier.height(dimens.paddingSmall))
                CategoryGrid(
                    categories = categories,
                    selectedId = selectedCategoryId,
                    onSelect = { selectedCategoryId = it }
                )
                Spacer(modifier = Modifier.height(dimens.paddingSmall))
                NumpadRow(
                    onDigit = { d ->
                        if (rawInput.contains(".") && rawInput.substringAfter(".").length >= 2) return@NumpadRow
                        rawInput += d
                    },
                    onDecimal = {
                        if (!rawInput.contains(".")) rawInput += "."
                    },
                    onBackspace = {
                        if (rawInput.isNotEmpty()) rawInput = rawInput.dropLast(1)
                    }
                )
                Spacer(modifier = Modifier.height(dimens.paddingSmall))
                SaveButton(
                    enabled = isFormValid,
                    onClick = {
                        if (isFormValid) {
                            viewModel.onAddTransaction(
                                categoryId = selectedCategoryId!!,
                                amount = amount,
                                type = transactionType,
                                note = note
                            )
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AmountDisplay(rawInput: String) {
    val dimens = LocalAppDimens.current
    val display = formatNumpadDisplay(rawInput)

    val scope = rememberCoroutineScope()
    val scaleAnimatable = remember { Animatable(1f) }

    LaunchedEffect(rawInput.length) {
        if (rawInput.isNotEmpty()) {
            scope.launch {
                scaleAnimatable.animateTo(
                    targetValue = 1.08f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                )
                scaleAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingLarge, vertical = dimens.paddingSmall + 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = display,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scaleAnimatable.value
                scaleY = scaleAnimatable.value
            }
        )
    }
}



@Composable
private fun CategoryGrid(
    categories: List<CategoryEntity>,
    selectedId: String?,
    onSelect: (String?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dimens = LocalAppDimens.current

    Column(modifier = Modifier.padding(horizontal = dimens.paddingLarge)) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall + 6.dp)
        ) {
            items(categories) { category ->
                val isSelected = category.id == selectedId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { haptic.mediumConfirm(); onSelect(category.id) }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(dimens.iconSizeLarge + 12.dp)
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
                            modifier = Modifier.size(dimens.iconSizeMedium),
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
}

@Composable
private fun NumpadKey(
    key: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "numpadKeyScale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "numpadKeyElevation"
    )

    val rippleColor = MaterialTheme.colorScheme.primary
    val rippleAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.12f else 0f,
        animationSpec = tween(
            durationMillis = if (isPressed) 80 else 250,
            easing = LinearEasing
        ),
        label = "rippleAlpha"
    )

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is PressInteraction.Release -> {
                    isPressed = false
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                is PressInteraction.Cancel -> {
                    isPressed = false
                }
            }
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                shadowElevation = elevation.toPx()
                shape = RoundedCornerShape(16.dp)
                clip = true
            }
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .drawBehind {
                if (rippleAlpha > 0f) {
                    drawCircle(
                        color = rippleColor.copy(alpha = rippleAlpha),
                        radius = size.minDimension / 2f
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (key == "\u232B") {
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

@Composable
private fun NumpadRow(
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit
) {
    val dimens = LocalAppDimens.current
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "\u232B")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall)
            ) {
                row.forEach { key ->
                    NumpadKey(
                        key = key,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(dimens.numpadKeyAspectRatio),
                        onClick = {
                            when (key) {
                                "\u232B" -> onBackspace()
                                "." -> onDecimal()
                                else -> onDigit(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val dimens = LocalAppDimens.current

    Button(
        onClick = { haptic.strongPress(); onClick() },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingLarge)
            .height(52.dp),
        shape = RoundedCornerShape(dimens.cornerRadiusMedium),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = "Save Transaction",
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
        "Electronics", "Food & Dining" -> Icons.Filled.ShoppingCart
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
    if (raw.isEmpty()) return "$0.00"
    val commaFmt = NumberFormat.getNumberInstance(Locale.US)
    return if (raw.contains(".")) {
        val whole = raw.substringBefore(".").ifEmpty { "0" }
        val frac = raw.substringAfter(".").padEnd(2, '0').take(2)
        "$ ${commaFmt.format(whole.toLongOrNull() ?: 0L)}.$frac"
    } else {
        "$ ${commaFmt.format(raw.toLongOrNull() ?: 0L)}.00"
    }
}
