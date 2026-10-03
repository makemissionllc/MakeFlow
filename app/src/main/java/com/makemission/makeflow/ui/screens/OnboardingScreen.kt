package com.makemission.makeflow.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.device.LocalReducedMotion
import kotlin.math.abs
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.core.format.CurrencyFormatter
import kotlinx.coroutines.launch
import java.util.UUID
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.components.AnimatedDoodle
import com.makemission.makeflow.ui.theme.LocalAppDimens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    selectedCurrency: String = "KES",
    onCurrencyChange: (String) -> Unit = {},
    onSaveAccounts: suspend (List<AccountEntity>) -> Unit = {},
    onSaveName: suspend (String) -> Unit = {},
    onOnboardingComplete: () -> Unit = {},
    hapticManager: HapticManager? = null
) {
    val dimens = LocalAppDimens.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var accountBalance by remember { mutableStateOf("") }
    var showNameError by remember { mutableStateOf(false) }

    val transientAccounts = remember {
        mutableStateListOf<AccountEntity>().apply {
            add(AccountEntity(UUID.randomUUID().toString(), "Cash", 0.0, 0.0, "#4CAF50"))
            add(AccountEntity(UUID.randomUUID().toString(), "Bank account", 0.0, 0.0, "#4CAF50"))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .systemBarsPadding()
        ) {
            val reducedMotion = LocalReducedMotion.current
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = pagerState.currentPage == 0 || username.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val pageOffset = (page - pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    .coerceIn(-1f, 1f)
                val pageAlpha: Float
                val pageScale: Float
                if (reducedMotion) {
                    pageAlpha = 1f
                    pageScale = 1f
                } else {
                    pageAlpha = (1f - abs(pageOffset) * 0.3f).coerceIn(0.7f, 1f)
                    pageScale = (1f - abs(pageOffset) * 0.05f).coerceIn(0.95f, 1f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = pageAlpha
                            scaleX = pageScale
                            scaleY = pageScale
                        }
                ) {
                    when (page) {
                        0 -> FeaturesStageView()
                        1 -> WelcomeStageView(
                        username = username,
                        onNameChange = {
                            username = it
                            if (it.isNotBlank()) showNameError = false
                        },
                        selectedCurrency = selectedCurrency,
                        onCurrencyChange = onCurrencyChange,
                        showNameError = showNameError
                    )
                    2 -> AccountSetupStageView(
                        selectedCurrency = selectedCurrency,
                        transientAccounts = transientAccounts,
                        accountName = accountName,
                        accountBalance = accountBalance,
                        onNameChange = { accountName = it },
                        onBalanceChange = { accountBalance = it },
                        onAddAccount = {
                            val parsed = accountBalance.toDoubleOrNull() ?: 0.0
                            if (accountName.isNotBlank()) {
                                transientAccounts.add(
                                    AccountEntity(
                                        id = UUID.randomUUID().toString(),
                                        name = accountName.trim(),
                                        balance = parsed,
                                        initialBalance = parsed,
                                        colorHex = "#6DD881"
                                    )
                                )
                                accountName = ""
                                accountBalance = ""
                            }
                        },
                        onRemoveAccount = { transientAccounts.remove(it) }
                    )
                }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val reducedMotion = LocalReducedMotion.current
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        val widthAnimSpec = if (reducedMotion) tween<Float>(100) else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        val widthScale by animateFloatAsState(
                            targetValue = if (isSelected) 24f else 8f,
                            animationSpec = widthAnimSpec,
                            label = "OnboardingIndicator"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(widthScale.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        )
                    }
                }

                Button(
                    onClick = {
                        when (pagerState.currentPage) {
                            0 -> {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                    hapticManager?.fire(HapticEvent.ONBOARDING_STEP_COMPLETE)
                                }
                            }
                            1 -> {
                                if (username.isNotBlank()) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(2)
                                        hapticManager?.fire(HapticEvent.ONBOARDING_STEP_COMPLETE)
                                    }
                                } else {
                                    showNameError = true
                                }
                            }
                            2 -> {
                                if (username.isNotBlank() && transientAccounts.isNotEmpty()) {
                                    coroutineScope.launch {
                                        onSaveName(username.trim())
                                        onSaveAccounts(transientAccounts.toList())
                                        hapticManager?.fire(HapticEvent.ONBOARDING_STEP_COMPLETE)
                                        onOnboardingComplete()
                                    }
                                }
                            }
                        }
                    },
                    enabled = when (pagerState.currentPage) {
                        0 -> true
                        1 -> true
                        2 -> transientAccounts.isNotEmpty()
                        else -> true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(dimens.shapeCornerMedium),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = when (pagerState.currentPage) {
                            0 -> "Get started"
                            1 -> "Continue"
                            2 -> "Done"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeStageView(
    username: String,
    onNameChange: (String) -> Unit,
    selectedCurrency: String = "KES",
    onCurrencyChange: (String) -> Unit = {},
    showNameError: Boolean = false
) {
    val dimens = LocalAppDimens.current
    val currencies = listOf("KES", "USD", "EUR", "GBP", "UGX", "TZS")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimens.paddingLarge),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy((-12).dp)
            ) {
                OnboardingVisualCard("Cash", CurrencyFormatter.format(3600.0, selectedCurrency), Icons.Rounded.Wallet, Modifier.align(Alignment.Start))
                OnboardingVisualCard("Savings", CurrencyFormatter.format(10000.0, selectedCurrency), Icons.Rounded.Savings, Modifier.align(Alignment.End))
                OnboardingVisualCard("Credit card", CurrencyFormatter.format(1450.0, selectedCurrency), Icons.Rounded.AccountBalanceWallet, Modifier.align(Alignment.Start))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mindful Wallet",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Manage all your finances with ease.",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            OutlinedTextField(
                value = username,
                onValueChange = onNameChange,
                label = { Text("Enter your name to unlock workspace") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(dimens.shapeCornerMedium),
                modifier = Modifier.fillMaxWidth()
            )

            if (showNameError) {
                Text(
                    text = "Please enter your name to continue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
            ) {
                currencies.forEach { code ->
                    val isSelected = code == selectedCurrency
                    Text(
                        text = code,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier
                            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .pressScale()
                            .clickable { onCurrencyChange(code) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Text(
                text = "All your financial data is kept securely on your local device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun AccountSetupStageView(
    selectedCurrency: String = "KES",
    transientAccounts: List<AccountEntity>,
    accountName: String,
    accountBalance: String,
    onNameChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (AccountEntity) -> Unit
) {
    val dimens = LocalAppDimens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
            Text(
                text = "Quick setup",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Seed initial trackers below. These form your account balance core.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = accountName,
                onValueChange = onNameChange,
                label = { Text("Account Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(dimens.cornerRadiusMedium),
                modifier = Modifier.weight(1.3f)
            )

            OutlinedTextField(
                value = accountBalance,
                onValueChange = onBalanceChange,
                label = { Text("Bal ($selectedCurrency)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(dimens.cornerRadiusMedium),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onAddAccount,
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(dimens.cornerRadiusMedium))
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }

        Text(
            text = "Accounts Ledger Topology",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.12f to Color.Black,
                            0.88f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(transientAccounts) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp))
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = CurrencyFormatter.format(item.balance, selectedCurrency),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { onRemoveAccount(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Discard",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
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
fun OnboardingVisualCard(name: String, balance: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = modifier
            .width(230.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(dimens.cornerRadiusLarge))
            .padding(dimens.paddingMedium),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = balance,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@Composable
fun FeaturesStageView(modifier: Modifier = Modifier) {
    val dimens = LocalAppDimens.current
    val features = remember {
        listOf(
            FeatureItem(Icons.Rounded.TrendingUp, "Spending Insights", "See where your money goes with smart spending breakdowns"),
            FeatureItem(Icons.Rounded.AccountBalance, "Budget Forecasting", "Predict future expenses and stay ahead of your budget"),
            FeatureItem(Icons.Rounded.AutoAwesome, "Smart Categories", "Learns from your corrections to sort transactions automatically"),
            FeatureItem(Icons.Rounded.Savings, "Savings Goals", "Set goals with personalized feasibility insights"),
            FeatureItem(Icons.Rounded.Sync, "M-PESA Import", "Auto-sync transactions from M-PESA messages"),
            FeatureItem(Icons.Rounded.Warning, "Anomaly Detection", "Get alerts for duplicate transactions and unusual activity"),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dimens.paddingLarge),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
            ) {
                Text(
                    text = "Mindful Wallet",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Your intelligent finance companion",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            AnimatedDoodle(
                icon = Icons.Rounded.Savings,
                doodleSize = 56.dp,
                tint = MaterialTheme.colorScheme.tertiary
            )
        }

        Text(
            text = "What it can do for you",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            features.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(dimens.shapeCornerMedium))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = feature.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = feature.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
