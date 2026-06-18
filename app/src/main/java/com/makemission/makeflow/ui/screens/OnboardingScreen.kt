package com.makemission.makeflow.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.ui.state.CurrencyInfo
import com.makemission.makeflow.ui.state.OnboardingUiState
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import com.makemission.makeflow.ui.viewmodel.OnboardingViewModel
import com.makemission.makeflow.ui.haptic.lightTick
import com.makemission.makeflow.ui.haptic.mediumConfirm
import com.makemission.makeflow.ui.haptic.strongPress
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = viewModel(),
    onCompleted: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { uiState.totalPages })
    val dimens = LocalAppDimens.current

    LaunchedEffect(uiState.currentPage) {
        pagerState.animateScrollToPage(uiState.currentPage)
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            viewModel.onPageChange(pagerState.currentPage)
        }
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onCompleted()
    }

    MakeFlowTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D0D1A),
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    key = { it }
                ) { page ->
                    PageContent(
                        page = page,
                        state = uiState,
                        onUsernameChange = viewModel::onUsernameChange,
                        onCurrencyChange = viewModel::onCurrencyChange,
                        onBalanceChange = viewModel::onBalanceChange,
                        onToggleCategory = viewModel::onToggleCategory,
                        pagerState = pagerState
                    )
                }

                BottomBar(
                    currentPage = uiState.currentPage,
                    totalPages = uiState.totalPages,
                    canContinue = uiState.canContinue,
                    isCompleting = uiState.isCompleting,
                    onContinue = viewModel::onContinue,
                    onSkip = viewModel::onSkip
                )
            }
        }
    }
}

@Composable
private fun PageContent(
    page: Int,
    state: OnboardingUiState,
    onUsernameChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onToggleCategory: (String) -> Unit,
    pagerState: PagerState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (page) {
            0 -> WelcomePage(name = state.username, onNameChange = onUsernameChange)
            1 -> CurrencyPage(
                selected = state.baseCurrency,
                onSelect = onCurrencyChange,
                isActive = state.currentPage == 1
            )
            2 -> BalancePage(
                balance = state.startingBalance,
                currency = state.baseCurrency,
                onBalanceChange = onBalanceChange,
                isActive = state.currentPage == 2
            )
            3 -> CategoriesPage(
                active = state.activeCategories,
                onToggle = onToggleCategory,
                isActive = state.currentPage == 3
            )
            4 -> CompletionPage(
                username = state.username,
                currency = state.baseCurrency,
                balance = state.startingBalance,
                categories = state.activeCategories,
                isCompleting = state.isCompleting
            )
        }
    }
}

@Composable
private fun WelcomePage(
    name: String,
    onNameChange: (String) -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (name.isNotBlank()) 0.85f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "logoScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Wallet,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "MakeFlow",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your personal finance\nand task companion",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "What should we call you?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        }
    }
}

@Composable
private fun CurrencyPage(
    selected: String,
    onSelect: (String) -> Unit,
    isActive: Boolean
) {
    val primaryCurrencies = remember {
        OnboardingUiState.availableCurrencies.filter { it.code in setOf("USD", "KES", "UGX", "EUR", "GBP") }
    }
    val selectedInfo = remember(selected) {
        OnboardingUiState.availableCurrencies.find { it.code == selected }
    }
    val animatedSymbol = selectedInfo?.symbol ?: "$"
    var hasAppeared by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (isActive && !hasAppeared) hasAppeared = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Currency",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose your primary currency to\npersonalize your balance display",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = hasAppeared,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 3 }
        ) {
            CurrencySelector(
                animatedSymbol = animatedSymbol,
                selectedInfo = selectedInfo,
                primaryCurrencies = primaryCurrencies,
                onSelect = onSelect,
                selected = selected
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CurrencySelector(
    animatedSymbol: String,
    selectedInfo: CurrencyInfo?,
    primaryCurrencies: List<CurrencyInfo>,
    onSelect: (String) -> Unit,
    selected: String
) {
    val dimens = LocalAppDimens.current

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "TRANSACTION PREVIEW",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedContent(
                targetState = animatedSymbol,
                transitionSpec = {
                    val direction = if (targetState.length > initialState.length) -1 else 1
                    (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } +
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                            (slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } +
                                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                },
                label = "currencySymbol"
            ) { currentSymbol ->
                Text(
                    text = currentSymbol,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "4,250.00",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "+",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Salary Deposit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Select Currency",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        val haptic = LocalHapticFeedback.current

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall + 4.dp)
        ) {
            items(primaryCurrencies) { currency ->
                val isSelected = currency.code == selected

                val chipScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "chipScale"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer(scaleX = chipScale, scaleY = chipScale)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            haptic.lightTick()
                            onSelect(currency.code)
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currency.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = currency.code,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (selectedInfo != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                    .padding(12.dp)
            ) {
                Text(
                    text = selectedInfo.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BalancePage(
    balance: String,
    currency: String,
    onBalanceChange: (String) -> Unit,
    isActive: Boolean
) {
    var hasAppeared by remember { mutableStateOf(false) }

    val currentSymbol = remember(currency) {
        OnboardingUiState.availableCurrencies.find { it.code == currency }?.symbol ?: "$"
    }
    val parsedBalance = balance.toDoubleOrNull() ?: 0.0
    val fmt = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    val animatedBalance by animateFloatAsState(
        targetValue = parsedBalance.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "balanceInterpolation"
    )

    val displayText = remember(animatedBalance) {
        "$currentSymbol ${fmt.format(animatedBalance.toDouble())}"
    }

    LaunchedEffect(isActive) {
        if (isActive && !hasAppeared) hasAppeared = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "First Deposit",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Seed your account with an\ninitial balance to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = hasAppeared,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 4 }
        ) {
            VirtualCardInitialDepositBuilder(
                displayText = displayText,
                currency = currency,
                balance = balance,
                onBalanceChange = onBalanceChange,
                currentSymbol = currentSymbol
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun VirtualCardInitialDepositBuilder(
    displayText: String,
    currency: String,
    balance: String,
    onBalanceChange: (String) -> Unit,
    currentSymbol: String
) {
    val dimens = LocalAppDimens.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((dimens.cardMinHeight * 2.5f).coerceAtLeast(180.dp))
                .clip(RoundedCornerShape(dimens.cornerRadiusMedium + 2.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(dimens.cornerRadiusMedium + 2.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimens.paddingLarge),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VIRTUAL",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Text(
                    text = "\u2022\u2022\u2022\u2022   4209",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 4.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "CARD HOLDER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "You",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedContent(
                            targetState = displayText,
                            transitionSpec = {
                                (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } +
                                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                                        (slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } +
                                                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                            },
                            label = "balanceFigure"
                        ) { animatedDisplay ->
                            Text(
                                text = animatedDisplay,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "DEPOSIT AMOUNT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = balance,
                onValueChange = { input ->
                    val filtered = input.filter { c -> c.isDigit() || c == '.' }
                    val parts = filtered.split(".")
                    val cleaned = if (parts.size > 2) "${parts[0]}.${parts[1].take(2)}"
                    else if (parts.size == 2) "${parts[0]}.${parts[1].take(2)}"
                    else filtered
                    if (cleaned.length <= 12) onBalanceChange(cleaned)
                },
                placeholder = { Text("0.00") },
                prefix = {
                    Text(
                        text = currentSymbol,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            if (balance.isNotBlank() && (balance.toDoubleOrNull() ?: 0.0) > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This will be your starting balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun CategoriesPage(
    active: Set<String>,
    onToggle: (String) -> Unit,
    isActive: Boolean
) {
    val haptic = LocalHapticFeedback.current
    var hasAppeared by remember { mutableStateOf(false) }

    val categories = OnboardingUiState.categoryTemplates
    val dimens = LocalAppDimens.current

    LaunchedEffect(isActive) {
        if (isActive && !hasAppeared) hasAppeared = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Budget Categories",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select the categories you want\nto track in your budget",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = hasAppeared,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 4 }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category.name in active
                    val glowColor = try {
                        Color(android.graphics.Color.parseColor(category.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    val cardScale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.92f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "cardScale"
                    )

                    val cardAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.5f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "cardAlpha"
                    )

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "iconScale"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(
                                scaleX = cardScale,
                                scaleY = cardScale,
                                alpha = cardAlpha
                            )
                            .clickable {
                                if (category.name in active) haptic.lightTick()
                                else haptic.mediumConfirm()
                                onToggle(category.name)
                            },
                        shape = RoundedCornerShape(dimens.cornerRadiusMedium),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        ),
                        border = if (isSelected)
                            BorderStroke(1.5.dp, glowColor.copy(alpha = 0.6f))
                        else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = iconForCategory(category.name),
                                contentDescription = category.name,
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer(
                                        scaleX = iconScale,
                                        scaleY = iconScale
                                    ),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))

                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Active",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionPage(
    username: String,
    currency: String,
    balance: String,
    categories: Set<String>,
    isCompleting: Boolean
) {
    val fmt = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val balanceValue = balance.toDoubleOrNull() ?: 0.0
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        if (!isCompleting) {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = spring()) +
                        slideInVertically(animationSpec = spring()) { it / 3 }
            ) {
                Text(
                    text = "All Set, $username!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 4 }
            ) {
                Text(
                    text = "Here's your financial snapshot",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 5 }
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SummaryRow("Name", username)
                    Divider()
                    SummaryRow("Currency", currency)
                    Divider()
                    SummaryRow("Balance", "$currency ${fmt.format(balanceValue)}")
                    Divider()
                    SummaryRow("Categories", "${categories.size} active")
                }
            }
        } else {
            LoadingAnimation()
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    )
}

@Composable
private fun LoadingAnimation() {
    val dotCount = 3
    val delays = listOf(0, 200, 400)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "Setting up your account...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            delays.forEach { delayMs ->
                var visible by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(delayMs.toLong())
                    while (true) {
                        visible = true
                        delay(600)
                        visible = false
                        delay(600)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (visible) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    currentPage: Int,
    totalPages: Int,
    canContinue: Boolean,
    isCompleting: Boolean,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF0D0D1A).copy(alpha = 0.95f)
                    ),
                    startY = 0f,
                    endY = 200f
                )
            )
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            repeat(totalPages) { i ->
                val isActive = i == currentPage

                val animatedWidth by animateDpAsState(
                    targetValue = if (isActive) 24.dp else 8.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "dotWidth"
                )

                Box(
                    modifier = Modifier
                        .width(animatedWidth)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }

        val haptic = LocalHapticFeedback.current

        Button(
            onClick = {
                haptic.strongPress()
                onContinue()
            },
            enabled = canContinue && !isCompleting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Text(
                text = if (currentPage == totalPages - 1) "Get Started" else "Continue",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }

        if (currentPage < totalPages - 1) {
            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = {
                    haptic.mediumConfirm()
                    onSkip()
                }
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

private fun iconForCategory(name: String): ImageVector {
    return when (name) {
        "Housing" -> Icons.Filled.Home
        "Food & Dining", "Electronics" -> Icons.Filled.ShoppingCart
        "Transportation" -> Icons.Filled.LocationOn
        "Entertainment" -> Icons.Filled.Star
        "Income" -> Icons.Filled.Check
        else -> Icons.Filled.Home
    }
}
