package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.core.logging.AppLogger
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.ui.components.AnimatedDoodle
import com.makemission.makeflow.ui.components.AnimatedProgressBar
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.MindfulAccentHex
import com.makemission.makeflow.ui.theme.OverlayTransition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

private val onboardingAccountColor = MindfulAccentHex
private val supportedCurrencies = listOf("KES", "USD", "EUR", "GBP", "UGX", "TZS")

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    val reducedMotion = LocalReducedMotion.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val accounts = remember {
        mutableStateListOf(
            starterAccount("Cash"),
            starterAccount("Bank account")
        )
    }

    var username by remember { mutableStateOf("") }
    var showNameError by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }

    fun moveToPage(page: Int) {
        coroutineScope.launch {
            if (reducedMotion) {
                pagerState.scrollToPage(page)
            } else {
                pagerState.animateScrollToPage(
                    page = page,
                    animationSpec = OverlayTransition.navigationCrossfade
                )
            }
            hapticManager?.fire(HapticEvent.ONBOARDING_STEP_COMPLETE)
        }
    }

    fun finishOnboarding() {
        if (isSaving || accounts.isEmpty()) return
        coroutineScope.launch {
            isSaving = true
            saveError = false
            try {
                onSaveName(username.trim())
                onSaveAccounts(accounts.toList())
                hapticManager?.fire(HapticEvent.ONBOARDING_STEP_COMPLETE)
                onOnboardingComplete()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                AppLogger.error("Onboarding", "Could not finish account setup", error)
                saveError = true
            } finally {
                isSaving = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = dimens.paddingLarge, vertical = dimens.spacingLarge)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = dimens.onboardingContentMaxWidth)
                .fillMaxWidth()
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OnboardingHeader(
                page = pagerState.currentPage,
                onBack = { moveToPage(pagerState.currentPage - 1) }
            )

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> WelcomeStageView(selectedCurrency)
                    1 -> ProfileStageView(
                        username = username,
                        onNameChange = {
                            username = it
                            if (it.isNotBlank()) showNameError = false
                        },
                        selectedCurrency = selectedCurrency,
                        showNameError = showNameError,
                        onChooseCurrency = { showCurrencySheet = true }
                    )
                    else -> AccountSetupStageView(
                        accounts = accounts,
                        selectedCurrency = selectedCurrency,
                        onAddAccount = { showAccountSheet = true },
                        onRemoveAccount = { account -> accounts.remove(account) }
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
            ) {
                if (saveError) {
                    Text(
                        text = "Setup could not be saved. Please try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(
                    onClick = {
                        when (pagerState.currentPage) {
                            0 -> moveToPage(1)
                            1 -> if (username.isNotBlank()) moveToPage(2) else {
                                showNameError = true
                                hapticManager?.fire(HapticEvent.VALIDATION_ERROR)
                            }
                            else -> finishOnboarding()
                        }
                    },
                    enabled = !isSaving && (pagerState.currentPage != 2 || accounts.isNotEmpty()),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.primaryButtonHeight)
                        .pressScale()
                ) {
                    Text(
                        text = when {
                            isSaving -> "Setting things up..."
                            pagerState.currentPage == 0 -> "Get started"
                            pagerState.currentPage == 1 -> "Continue"
                            else -> "Start using Mindful"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    if (showCurrencySheet) {
        CurrencyPickerSheet(
            selectedCurrency = selectedCurrency,
            onDismiss = { showCurrencySheet = false },
            onSelect = { currency ->
                onCurrencyChange(currency)
                showCurrencySheet = false
                hapticManager?.fire(HapticEvent.NAV_SELECT)
            }
        )
    }

    if (showAccountSheet) {
        AddOnboardingAccountSheet(
            currency = selectedCurrency,
            onDismiss = { showAccountSheet = false },
            onAdd = { accounts.add(it) }
        )
    }
}

@Composable
private fun OnboardingHeader(page: Int, onBack: () -> Unit) {
    val dimens = LocalAppDimens.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (page > 0) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Spacer(Modifier.size(dimens.iconSizeLarge))
            }
            Text(
                text = "STEP ${page + 1} OF 3",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedProgressBar(
            progress = (page + 1) / 3f,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.progressTrackHeight)
                .clip(CircleShape)
        )
    }
}

@Composable
fun WelcomeStageView(selectedCurrency: String = "KES") {
    val dimens = LocalAppDimens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingXXLarge)
    ) {
        WelcomePreview(selectedCurrency)
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
            Text(
                text = "A calmer view of your money.",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Bring spending, plans, and savings into one clear place, then make progress at your own pace.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(dimens.paddingMedium),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Private by design. Your finance data stays on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun WelcomePreview(currency: String) {
    val dimens = LocalAppDimens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(dimens.paddingLarge),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)) {
                    Text(
                        text = "SAMPLE OVERVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Money in view",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                AnimatedDoodle(
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    doodleSize = dimens.iconSizeLarge,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = CurrencyFormatter.format(128450.0, currency),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingXLarge)
            ) {
                PreviewMetric(
                    label = "INCOME",
                    amount = CurrencyFormatter.format(42300.0, currency),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                PreviewMetric(
                    label = "SPENDING",
                    amount = CurrencyFormatter.format(18650.0, currency),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PreviewMetric(
    label: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.spacingXSmall)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

@Composable
private fun ProfileStageView(
    username: String,
    onNameChange: (String) -> Unit,
    selectedCurrency: String,
    showNameError: Boolean,
    onChooseCurrency: () -> Unit
) {
    val dimens = LocalAppDimens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingXLarge)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
            Text(
                text = "Make it yours",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "A few details help Mindful feel like your space.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = username,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your name") },
            supportingText = {
                Text(if (showNameError) "Enter your name to continue" else "You can change this later")
            },
            isError = showNameError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            shape = MaterialTheme.shapes.large
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
            Text(
                text = "Home currency",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale()
                    .clickable(onClick = onChooseCurrency),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingMedium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)) {
                        Text(
                            text = selectedCurrency,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Amounts will be shown in $selectedCurrency",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AccountSetupStageView(
    accounts: List<AccountEntity>,
    selectedCurrency: String,
    onAddAccount: () -> Unit,
    onRemoveAccount: (AccountEntity) -> Unit
) {
    val dimens = LocalAppDimens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
            Text(
                text = "Where do you keep money?",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Start with a couple of places, or add the accounts you use most.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (accounts.isEmpty()) {
            Text(
                text = "Add at least one account to start tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        accounts.forEachIndexed { index, account ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimens.spacingSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
            ) {
                Box(
                    modifier = Modifier
                        .size(dimens.iconSizeLarge)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
                ) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = CurrencyFormatter.format(account.balance, selectedCurrency),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onRemoveAccount(account) }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove ${account.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (index < accounts.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        OutlinedButton(
            onClick = onAddAccount,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.size(dimens.spacingSmall))
            Text("Add another account")
        }
        Text(
            text = "You can rename accounts and update balances any time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CurrencyPickerSheet(
    selectedCurrency: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val dimens = LocalAppDimens.current
    AdaptiveSheetDialog(onDismiss = onDismiss) {
        Text(
            text = "Choose your currency",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "You can change this later in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        supportedCurrencies.forEach { currency ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .pressScale()
                    .clickable { onSelect(currency) }
                    .padding(vertical = dimens.spacingLarge, horizontal = dimens.spacingMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)) {
                    Text(
                        text = CurrencyFormatter.symbolFor(currency),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currency,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (currency == selectedCurrency) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (currency != supportedCurrencies.last()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun AddOnboardingAccountSheet(
    currency: String,
    onDismiss: () -> Unit,
    onAdd: (AccountEntity) -> Unit
) {
    val dimens = LocalAppDimens.current
    var name by remember { mutableStateOf("") }
    var balanceInput by remember { mutableStateOf("") }
    var showValidation by remember { mutableStateOf(false) }
    val balance = if (balanceInput.isBlank()) 0.0 else balanceInput.toDoubleOrNull()
    val isNameInvalid = name.isBlank()
    val isBalanceInvalid = balance == null

    AdaptiveSheetDialog(onDismiss = onDismiss) {
        Text(
            text = "Add an account",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Give it a name and add today's balance if you know it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Account name") },
            singleLine = true,
            isError = showValidation && isNameInvalid,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            shape = MaterialTheme.shapes.large
        )
        OutlinedTextField(
            value = balanceInput,
            onValueChange = { value ->
                if (value.isEmpty() || value.matches(Regex("-?\\d*(\\.\\d{0,2})?"))) balanceInput = value
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Balance ($currency)") },
            singleLine = true,
            isError = showValidation && isBalanceInvalid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            shape = MaterialTheme.shapes.large
        )
        if (showValidation && (isNameInvalid || isBalanceInvalid)) {
            Text(
                text = if (isNameInvalid) "Add a name for this account" else "Enter a valid balance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = {
                showValidation = true
                if (!isNameInvalid && !isBalanceInvalid) {
                    val amount = balance ?: 0.0
                    onAdd(
                        AccountEntity(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            balance = amount,
                            initialBalance = amount,
                            colorHex = onboardingAccountColor
                        )
                    )
                    onDismiss()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.primaryButtonHeight),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text("Add account")
        }
    }
}

private fun starterAccount(name: String) = AccountEntity(
    id = UUID.randomUUID().toString(),
    name = name,
    balance = 0.0,
    initialBalance = 0.0,
    colorHex = onboardingAccountColor
)
