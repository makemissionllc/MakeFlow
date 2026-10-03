package com.makemission.makeflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import java.util.Calendar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.RestorePage
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.notifications.NotificationGate
import com.makemission.makeflow.core.notifications.openAppNotificationSettings
import com.makemission.makeflow.ui.components.CollapsingTopBar
import com.makemission.makeflow.ui.components.LargeTitleItem
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.viewmodel.SettingsViewModel
import com.makemission.makeflow.core.format.CurrencyFormatter

data class ThemeOption(val key: String, val label: String)
data class CurrencyOption(val code: String, val symbol: String)

private val themes = listOf(
    ThemeOption("forest", "Forest"),
    ThemeOption("ocean", "Ocean"),
    ThemeOption("sunset", "Sunset"),
    ThemeOption("purple", "Royal Purple"),
    ThemeOption("dynamic", "Dynamic")
)

private val modeLabels = mapOf(
    "system" to "System",
    "light" to "Light",
    "dark" to "Dark"
)
private val modeOptions = listOf("system", "light", "dark")

private val currencies = listOf(
    CurrencyOption("KES", "KES"),
    CurrencyOption("USD", "$"),
    CurrencyOption("EUR", "\u20AC"),
    CurrencyOption("GBP", "\u00A3"),
    CurrencyOption("UGX", "UGX"),
    CurrencyOption("TZS", "TZS")
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
    hapticManager: HapticManager? = null,
    notificationGate: NotificationGate? = null,
    onOpenRecurringTransactions: () -> Unit = {},
    onOpenSavingsGoals: () -> Unit = {},
    onOpenManageTemplates: () -> Unit = {},
    onOpenManageCategories: () -> Unit = {},
    onOpenExchangeRates: () -> Unit = {},
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    onOpenChangelog: () -> Unit = {}
) {
    val dimens = LocalAppDimens.current
    val currentCurrency by viewModel.selectedCurrency.collectAsState()
    val currentTheme by viewModel.selectedTheme.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val amoledMode by viewModel.amoledMode.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val smartCategorizationEnabled by viewModel.smartCategorizationEnabled.collectAsState()
    val appLockEnabled by viewModel.appLockEnabled.collectAsState()
    val logMpesaCosts by viewModel.logMpesaCosts.collectAsState()
    val hasPromptedNotif by viewModel.hasPromptedNotificationPermission.collectAsState()
    val listState = rememberLazyListState()
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showModePicker by remember { mutableStateOf(false) }
    val isEffectivelyDark = when (currentThemeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showResetPersonalizationConfirm by remember { mutableStateOf(false) }
    var showPdfExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val config = LocalAdaptiveConfig.current
    val reducedMotion = LocalReducedMotion.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = config.horizontalPadding),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + config.barHeight,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
        ) {
            item {
                LargeTitleItem(title = "Settings", scrollState = listState)
            }

            if (config.isTablet && config.isLandscape) {
                item { StaggeredItem(index = 1) { SectionHeader(text = "Preferences") } }

                item {
                    StaggeredItem(index = 2) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(config.cardSpacing)) {
                        Box(Modifier.weight(1f)) {
                        SettingsRow(icon=Icons.Outlined.SwapHoriz, title="Currency", subtitle=currentCurrency, onClick={ showCurrencyPicker = true })
                        }
                        Box(Modifier.weight(1f)) {
                        SettingsRow(icon=Icons.Outlined.Palette, title="Theme", subtitle=themes.find { it.key == currentTheme }?.label ?: "Forest", onClick={ showThemePicker = true })
                        }
                    }
                    }
                }

                item {
                    StaggeredItem(index = 2) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(config.cardSpacing)) {
                        Box(Modifier.weight(1f)) {
                        SettingsRow(icon=Icons.Outlined.DarkMode, title="Mode", subtitle=modeLabels[currentThemeMode] ?: "System", onClick={ showModePicker = true })
                        }
                        Box(Modifier.weight(1f)) {
                        AmoledToggleRow(
                            enabled = amoledMode,
                            isDarkAvailable = isEffectivelyDark,
                            onToggle = { viewModel.saveAmoledMode(it) }
                        )
                        }
                    }
                    }
                }

                item {
                    StaggeredItem(index = 3) {
                    SettingsRow(icon=Icons.Outlined.Info, title="Recurring Transactions", subtitle="Manage scheduled bills & income", onClick={ onOpenRecurringTransactions() })
                    }
                }

                item {
                    StaggeredItem(index = 4) {
                    SettingsRow(icon=Icons.Outlined.Info, title="Manage Templates", subtitle="Create, edit & delete templates", onClick={ onOpenManageTemplates() })
                    }
                }

                item {
                    StaggeredItem(index = 5) {
                    SettingsRow(icon=Icons.Outlined.Info, title="Manage Categories", subtitle="Merge or convert categories", onClick={ onOpenManageCategories() })
                    }
                }

                item {
                    StaggeredItem(index = 6) {
                    SettingsRow(icon=Icons.Outlined.SwapHoriz, title="Exchange Rates", subtitle="Set conversion rates for multi-currency", onClick={ onOpenExchangeRates() })
                    }
                }

                item {
                    StaggeredItem(index = 7) {
                    SettingsRow(icon=Icons.Outlined.EmojiEvents, title="Savings Goals", subtitle="Track and manage savings targets", onClick={ onOpenSavingsGoals() })
                    }
                }

                item {
                    StaggeredItem(index = 8) {
                    SmartCategorizationToggleRow(
                        enabled = smartCategorizationEnabled,
                        onToggle = { viewModel.saveSmartCategorizationEnabled(it) }
                    )
                    }
                }

                item {
                    StaggeredItem(index = 8) {
                    HapticToggleRow(
                        enabled = hapticsEnabled,
                        onToggle = { viewModel.saveHapticsEnabled(it); hapticManager?.fire(HapticEvent.TOGGLE) }
                    )
                    }
                }

                item {
                    StaggeredItem(index = 9) {
                    AppLockToggleRow(
                        enabled = appLockEnabled,
                        onToggle = { viewModel.saveAppLockEnabled(it) }
                    )
                    }
                }

                item {
                    StaggeredItem(index = 10) {
                    MpesaCostsToggleRow(
                        enabled = logMpesaCosts,
                        onToggle = { viewModel.saveLogMpesaCosts(it) }
                    )
                    }
                }

                if (hasPromptedNotif && !isNotificationPermissionGranted(context)) {
                    item {
                        StaggeredItem(index = 11) {
                        Box(Modifier.fillMaxWidth()) {
                            NotificationEnableRow()
                        }
                        }
                    }
                    item { StaggeredItem(index = 11) { Spacer(modifier = Modifier.height(dimens.spacingXSmall)) } }
                }

                item { StaggeredItem(index = 12) { Spacer(modifier = Modifier.height(dimens.spacingXLarge)) } }
                item { StaggeredItem(index = 13) { SectionHeader(text = "Data") } }

                item {
                    StaggeredItem(index = 14) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(config.cardSpacing)) {
                        Box(Modifier.weight(1f)) {
                        SettingsRow(icon=Icons.Outlined.FileDownload, title="Export CSV", subtitle="Download all transactions", onClick={ viewModel.exportCsv(context); Toast.makeText(context, "CSV exported to Downloads", Toast.LENGTH_SHORT).show() })
                        }
                        Box(Modifier.weight(1f)) {
                        SettingsRow(icon=Icons.Outlined.FileDownload, title="Export PDF", subtitle="Statement with date range", onClick={ showPdfExportDialog = true })
                        }
                    }
                    }
                }

                item { StaggeredItem(index = 100) { Spacer(modifier = Modifier.height(dimens.spacingXSmall)) } }

                item {
                    StaggeredItem(index = 101) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(config.cardSpacing)) {
                        Box(Modifier.weight(1f)) {
                        SettingsRow(icon=Icons.Outlined.Save, title="Backup Now", subtitle="Save all data to a file", onClick={ onBackup() })
                        }
                        Box(Modifier.weight(1f)) {
                        SettingsRow(icon=Icons.Outlined.RestorePage, title="Restore", subtitle="Replace from a file", onClick={ onRestore() })
                        }
                    }
                    }
                }

                item { StaggeredItem(index = 102) { Spacer(modifier = Modifier.height(dimens.spacingXSmall)) } }

                item {
                    StaggeredItem(index = 103) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(config.cardSpacing)) {
                        Box(Modifier.weight(1f)) {
                        ResetPersonalizationRow(
                            viewModel = viewModel,
                            showConfirm = showResetPersonalizationConfirm,
                            onShowConfirm = { showResetPersonalizationConfirm = true },
                            onDismissConfirm = { showResetPersonalizationConfirm = false }
                        )
                        }
                        Box(Modifier.weight(1f)) {
                        SettingsRow(icon=Icons.Outlined.Delete, title="Clear All Data", subtitle="Reset everything", onClick={ showClearConfirm = true })
                        }
                    }
                    }
                }

                item { StaggeredItem(index = 104) { Spacer(modifier = Modifier.height(dimens.spacingXLarge)) } }
                item { StaggeredItem(index = 105) { SectionHeader(text = "About") } }

                item {
                    StaggeredItem(index = 106) {
                    SettingsRow(icon=Icons.Outlined.Info, title="Mindful Wallet", subtitle="Version 1.0")
                    }
                }
                item {
                    StaggeredItem(index = 107) {
                    SettingsRow(icon=Icons.Outlined.Campaign, title="What's New", subtitle="See what has changed", onClick = onOpenChangelog)
                    }
                }
                if (reducedMotion) {
                    item {
                        StaggeredItem(index = 108) {
                        ReducedMotionRow()
                        }
                    }
                }
            } else {
                item { StaggeredItem(index = 1) { SectionHeader(text = "Preferences") } }

                item {
                    StaggeredItem(index = 2) {
                    SettingsRow(icon=Icons.Outlined.SwapHoriz, title="Currency", subtitle=currentCurrency, onClick={ showCurrencyPicker = true })
                    }
                }

                item {
                    StaggeredItem(index = 3) {
                    SettingsRow(icon=Icons.Outlined.Palette, title="Theme", subtitle=themes.find { it.key == currentTheme }?.label ?: "Forest", onClick={ showThemePicker = true })
                    }
                }

                item {
                    StaggeredItem(index = 3) {
                    SettingsRow(icon=Icons.Outlined.DarkMode, title="Mode", subtitle=modeLabels[currentThemeMode] ?: "System", onClick={ showModePicker = true })
                    }
                }

                item {
                    StaggeredItem(index = 3) {
                    AmoledToggleRow(
                        enabled = amoledMode,
                        isDarkAvailable = isEffectivelyDark,
                        onToggle = { viewModel.saveAmoledMode(it) }
                    )
                    }
                }

                item {
                    StaggeredItem(index = 4) {
                    SettingsRow(icon=Icons.Outlined.Info, title="Recurring Transactions", subtitle="Manage scheduled bills & income", onClick={ onOpenRecurringTransactions() })
                    }
                }

                item {
                    StaggeredItem(index = 5) {
                    SettingsRow(icon=Icons.Outlined.Info, title="Manage Templates", subtitle="Create, edit & delete templates", onClick={ onOpenManageTemplates() })
                    }
                }

                item {
                    StaggeredItem(index = 6) {
                    SettingsRow(icon=Icons.Outlined.Info, title="Manage Categories", subtitle="Merge or convert categories", onClick={ onOpenManageCategories() })
                    }
                }

                item {
                    StaggeredItem(index = 7) {
                    SettingsRow(icon=Icons.Outlined.SwapHoriz, title="Exchange Rates", subtitle="Set conversion rates for multi-currency", onClick={ onOpenExchangeRates() })
                    }
                }

                item {
                    StaggeredItem(index = 8) {
                    SettingsRow(icon=Icons.Outlined.EmojiEvents, title="Savings Goals", subtitle="Track and manage savings targets", onClick={ onOpenSavingsGoals() })
                    }
                }

                item {
                    StaggeredItem(index = 9) {
                    SmartCategorizationToggleRow(
                        enabled = smartCategorizationEnabled,
                        onToggle = { viewModel.saveSmartCategorizationEnabled(it) }
                    )
                    }
                }

                item {
                    StaggeredItem(index = 9) {
                    HapticToggleRow(
                        enabled = hapticsEnabled,
                        onToggle = { viewModel.saveHapticsEnabled(it); hapticManager?.fire(HapticEvent.TOGGLE) }
                    )
                    }
                }

                item {
                    StaggeredItem(index = 10) {
                    AppLockToggleRow(
                        enabled = appLockEnabled,
                        onToggle = { viewModel.saveAppLockEnabled(it) }
                    )
                    }
                }

                item {
                    StaggeredItem(index = 11) {
                    MpesaCostsToggleRow(
                        enabled = logMpesaCosts,
                        onToggle = { viewModel.saveLogMpesaCosts(it) }
                    )
                    }
                }

                if (hasPromptedNotif && !isNotificationPermissionGranted(context)) {
                    item {
                        StaggeredItem(index = 11) {
                        NotificationEnableRow()
                        }
                    }
                }

                item { StaggeredItem(index = 12) { Spacer(modifier = Modifier.height(dimens.spacingXLarge)) } }
                item { StaggeredItem(index = 13) { SectionHeader(text = "Data") } }

                item {
                    StaggeredItem(index = 14) {
                    SettingsRow(icon=Icons.Outlined.FileDownload, title="Export CSV", subtitle="Download all transactions", onClick={ viewModel.exportCsv(context); Toast.makeText(context, "CSV exported to Downloads", Toast.LENGTH_SHORT).show() })
                    }
                }

                item {
                    StaggeredItem(index = 15) {
                    SettingsRow(icon=Icons.Outlined.FileDownload, title="Export PDF", subtitle="Statement with date range", onClick={ showPdfExportDialog = true })
                    }
                }

                item {
                    StaggeredItem(index = 16) {
                    SettingsRow(icon=Icons.Outlined.Save, title="Backup Now", subtitle="Save all data to a file", onClick={ onBackup() })
                    }
                }

                item {
                    StaggeredItem(index = 17) {
                    SettingsRow(icon=Icons.Outlined.RestorePage, title="Restore from Backup", subtitle="Replace all data from a file", onClick={ onRestore() })
                    }
                }

                item {
                    StaggeredItem(index = 18) {
                    ResetPersonalizationRow(
                        viewModel = viewModel,
                        showConfirm = showResetPersonalizationConfirm,
                        onShowConfirm = { showResetPersonalizationConfirm = true },
                        onDismissConfirm = { showResetPersonalizationConfirm = false }
                    )
                    }
                }

                item {
                    StaggeredItem(index = 19) {
                    SettingsRow(icon=Icons.Outlined.Delete, title="Clear All Data", subtitle="Reset everything", onClick={ showClearConfirm = true })
                    }
                }

                item { StaggeredItem(index = 20) { Spacer(modifier = Modifier.height(dimens.spacingXLarge)) } }
                item { StaggeredItem(index = 21) { SectionHeader(text = "About") } }

                item {
                    StaggeredItem(index = 22) {
                    SettingsRow(icon=Icons.Outlined.Info, title="Mindful Wallet", subtitle="Version 1.0")
                    }
                }
                item {
                    StaggeredItem(index = 23) {
                    SettingsRow(icon=Icons.Outlined.Campaign, title="What's New", subtitle="See what has changed", onClick = onOpenChangelog)
                    }
                }
                if (reducedMotion) {
                    item {
                        StaggeredItem(index = 24) {
                        ReducedMotionRow()
                        }
                    }
                }
            }
        }

        CollapsingTopBar(
            title = "Settings",
            scrollState = listState,
            modifier = Modifier.align(Alignment.TopStart),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    if (showCurrencyPicker) {
        PickerDialog(
            title = "Select Currency",
            items = currencies.map { it.code },
            currentItem = currentCurrency,
            onDismiss = { showCurrencyPicker = false },
            onSelect = {
                viewModel.saveCurrency(it)
                showCurrencyPicker = false
            }
        )
    }

    if (showThemePicker) {
        PickerDialog(
            title = "Select Theme",
            items = themes.map { it.label },
            currentItem = themes.find { it.key == currentTheme }?.label ?: "Forest",
            onDismiss = { showThemePicker = false },
            onSelect = { label ->
                themes.find { it.label == label }?.key?.let { key ->
                    viewModel.saveTheme(key)
                }
                showThemePicker = false
            }
        )
    }

    if (showModePicker) {
        PickerDialog(
            title = "Select Mode",
            items = modeOptions.map { modeLabels[it] ?: it },
            currentItem = modeLabels[currentThemeMode] ?: "System",
            onDismiss = { showModePicker = false },
            onSelect = { label ->
                val key = modeLabels.entries.find { it.value == label }?.key ?: "system"
                viewModel.saveThemeMode(key)
                showModePicker = false
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("Clear All Data?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This will delete all transactions, budgets, accounts, and categories. This action cannot be undone.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearConfirm = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        )
    }

    if (showResetPersonalizationConfirm) {
        AlertDialog(
            onDismissRequest = { showResetPersonalizationConfirm = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("Reset Personalization?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This will clear all correction history and reset the ML model to its default state. Your transactions and categories will not be affected.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetPersonalization()
                    showResetPersonalizationConfirm = false
                }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPersonalizationConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        )
    }

    if (showPdfExportDialog) {
        AlertDialog(
            onDismissRequest = { showPdfExportDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("Export PDF Statement", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Select date range:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 12.dp))
                    PdfRangeOption("This Month", onClick = {
                        showPdfExportDialog = false
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val start = cal.timeInMillis
                        cal.add(Calendar.MONTH, 1)
                        val end = cal.timeInMillis
                        viewModel.exportPdf(context, start, end) { fileName ->
                            Toast.makeText(context, "PDF saved to Downloads/$fileName", Toast.LENGTH_SHORT).show()
                        }
                    })
                    PdfRangeOption("All Time", onClick = {
                        showPdfExportDialog = false
                        viewModel.exportPdf(context, 0L, Long.MAX_VALUE) { fileName ->
                            Toast.makeText(context, "PDF saved to Downloads/$fileName", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            },
            confirmButton = {
                TextButton(onClick = { showPdfExportDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    val dimens = LocalAppDimens.current
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        fontSize = dimens.textS,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressScale() else Modifier)
            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(dimens.shapeCornerXSmall)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textMl, fontWeight = FontWeight.Medium)
                Text(text = subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = dimens.textSm)
            }
        }
    }
}

@Composable
private fun HapticToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(dimens.shapeCornerXSmall)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = "Haptic Feedback", color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textMl, fontWeight = FontWeight.Medium)
                Text(text = if (enabled) "On" else "Off", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = dimens.textSm)
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = LocalMakeFlowColors.current.colorSuccess,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
}

@Composable
private fun AppLockToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(dimens.shapeCornerXSmall)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = "App Lock", color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textMl, fontWeight = FontWeight.Medium)
                Text(text = if (enabled) "On" else "Off", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = dimens.textSm)
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = LocalMakeFlowColors.current.colorSuccess,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
}

@Composable
private fun SmartCategorizationToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(dimens.shapeCornerXSmall)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.TipsAndUpdates,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = "Smart categorization", color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textMl, fontWeight = FontWeight.Medium)
                Text(text = if (enabled) "On" else "Off", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = dimens.textSm)
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = LocalMakeFlowColors.current.colorSuccess,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
}

@Composable
private fun MpesaCostsToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(dimens.shapeCornerXSmall)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = "Log M-PESA fees", color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textMl, fontWeight = FontWeight.Medium)
                Text(text = if (enabled) "Create separate fee transactions" else "Ignore transaction costs", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = dimens.textSm)
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = LocalMakeFlowColors.current.colorSuccess,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
}

@Composable
private fun ReducedMotionRow() {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(dimens.shapeCornerXSmall)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = "Reduced motion active", color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textMl, fontWeight = FontWeight.Medium)
                Text(text = "Power saving mode", color = MaterialTheme.colorScheme.error, fontSize = dimens.textSm)
            }
        }
    }
}

@Composable
private fun NotificationEnableRow() {
    val dimens = LocalAppDimens.current
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { openAppNotificationSettings(context) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(dimens.shapeCornerXSmall)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = LocalMakeFlowColors.current.colorWarning,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = "Enable notifications", color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textMl, fontWeight = FontWeight.Medium)
                Text(text = "Notifications are disabled", color = LocalMakeFlowColors.current.colorWarning, fontSize = dimens.textSm)
            }
        }
    }
}

@Composable
private fun PdfRangeOption(label: String, onClick: () -> Unit) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.shapeCornerXSmall))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textMl)
    }
}

private fun isNotificationPermissionGranted(context: android.content.Context): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        android.content.pm.PackageManager.PERMISSION_GRANTED ==
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            )
    } else {
        true
    }
}

@Composable
private fun ResetPersonalizationRow(
    viewModel: SettingsViewModel,
    showConfirm: Boolean,
    onShowConfirm: () -> Unit,
    onDismissConfirm: () -> Unit
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onShowConfirm() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(dimens.shapeCornerXSmall)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = "Reset personalization", color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textMl, fontWeight = FontWeight.Medium)
                Text(text = "Clear corrections & ML checkpoint", color = MaterialTheme.colorScheme.error, fontSize = dimens.textSm)
            }
        }
    }
}

@Composable
private fun PickerDialog(
    title: String,
    items: List<String>,
    currentItem: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val dimens = LocalAppDimens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.shapeCornerXSmall))
                            .clickable { onSelect(item) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item, color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.textMl)
                        if (item == currentItem) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = LocalMakeFlowColors.current.colorSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    )
}

@Composable
private fun AmoledToggleRow(
    enabled: Boolean,
    isDarkAvailable: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val dimens = LocalAppDimens.current
    val alpha = if (isDarkAvailable) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDarkAvailable) Modifier.pressScale() else Modifier)
            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha))
            .then(if (isDarkAvailable) Modifier.clickable { onToggle(!enabled) } else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = alpha), RoundedCornerShape(dimens.shapeCornerXSmall)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f * alpha),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = "AMOLED Dark", color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), fontSize = dimens.textMl, fontWeight = FontWeight.Medium)
                Text(
                    text = if (isDarkAvailable) "Pure black for OLED screens" else "Only active in dark mode",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f * alpha),
                    fontSize = dimens.textSm
                )
            }
        }
        Switch(
            checked = enabled && isDarkAvailable,
            onCheckedChange = { if (isDarkAvailable) onToggle(it) },
            enabled = isDarkAvailable,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = LocalMakeFlowColors.current.colorSuccess,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledCheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                disabledCheckedTrackColor = LocalMakeFlowColors.current.colorSuccess.copy(alpha = 0.5f),
                disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
            )
        )
    }
}
