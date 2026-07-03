package com.makemission.makeflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SwapHoriz
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
import com.makemission.makeflow.ui.components.StaggeredItem
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.viewmodel.SettingsViewModel

data class ThemeOption(val key: String, val label: String)
data class CurrencyOption(val code: String, val symbol: String)

private val themes = listOf(
    ThemeOption("forest", "Forest"),
    ThemeOption("ocean", "Ocean"),
    ThemeOption("sunset", "Sunset"),
    ThemeOption("purple", "Royal Purple"),
    ThemeOption("dynamic", "Dynamic")
)

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
    notificationGate: NotificationGate? = null
) {
    val currentCurrency by viewModel.selectedCurrency.collectAsState()
    val currentTheme by viewModel.selectedTheme.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val hasPromptedNotif by viewModel.hasPromptedNotificationPermission.collectAsState()
    val listState = rememberLazyListState()
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val config = LocalAdaptiveConfig.current
    val reducedMotion = LocalReducedMotion.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    StaggeredItem(index = 3) {
                    HapticToggleRow(
                        enabled = hapticsEnabled,
                        onToggle = { viewModel.saveHapticsEnabled(it); hapticManager?.fire(HapticEvent.TOGGLE) }
                    )
                    }
                }

                if (hasPromptedNotif && !isNotificationPermissionGranted(context)) {
                    item {
                        StaggeredItem(index = 4) {
                        Box(Modifier.fillMaxWidth()) {
                            NotificationEnableRow()
                        }
                        }
                    }
                    item { StaggeredItem(index = 5) { Spacer(modifier = Modifier.height(4.dp)) } }
                }

                item { StaggeredItem(index = 6) { Spacer(modifier = Modifier.height(16.dp)) } }
                item { StaggeredItem(index = 7) { SectionHeader(text = "Data") } }

                item {
                    StaggeredItem(index = 8) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(config.cardSpacing)) {
                        Box(Modifier.weight(1f)) {
                        SettingsRow(icon=Icons.Outlined.FileDownload, title="Export CSV", subtitle="Download all transactions", onClick={ viewModel.exportCsv(context); Toast.makeText(context, "CSV exported to Downloads", Toast.LENGTH_SHORT).show() })
                        }
                        Box(Modifier.weight(1f)) {
                        SettingsRow(icon=Icons.Outlined.Delete, title="Clear All Data", subtitle="Reset everything", onClick={ showClearConfirm = true })
                        }
                    }
                    }
                }

                item { StaggeredItem(index = 9) { Spacer(modifier = Modifier.height(16.dp)) } }
                item { StaggeredItem(index = 10) { SectionHeader(text = "About") } }

                item {
                    StaggeredItem(index = 11) {
                    SettingsRow(icon=Icons.Outlined.Info, title="MakeFlow", subtitle="Version 1.0", onClick={})
                    }
                }
                if (reducedMotion) {
                    item {
                        StaggeredItem(index = 12) {
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
                    StaggeredItem(index = 4) {
                    HapticToggleRow(
                        enabled = hapticsEnabled,
                        onToggle = { viewModel.saveHapticsEnabled(it); hapticManager?.fire(HapticEvent.TOGGLE) }
                    )
                    }
                }

                if (hasPromptedNotif && !isNotificationPermissionGranted(context)) {
                    item {
                        StaggeredItem(index = 5) {
                        NotificationEnableRow()
                        }
                    }
                }

                item { StaggeredItem(index = 6) { Spacer(modifier = Modifier.height(16.dp)) } }
                item { StaggeredItem(index = 7) { SectionHeader(text = "Data") } }

                item {
                    StaggeredItem(index = 8) {
                    SettingsRow(icon=Icons.Outlined.FileDownload, title="Export CSV", subtitle="Download all transactions", onClick={ viewModel.exportCsv(context); Toast.makeText(context, "CSV exported to Downloads", Toast.LENGTH_SHORT).show() })
                    }
                }

                item {
                    StaggeredItem(index = 9) {
                    SettingsRow(icon=Icons.Outlined.Delete, title="Clear All Data", subtitle="Reset everything", onClick={ showClearConfirm = true })
                    }
                }

                item { StaggeredItem(index = 10) { Spacer(modifier = Modifier.height(16.dp)) } }
                item { StaggeredItem(index = 11) { SectionHeader(text = "About") } }

                item {
                    StaggeredItem(index = 12) {
                    SettingsRow(icon=Icons.Outlined.Info, title="MakeFlow", subtitle="Version 1.0", onClick={})
                    }
                }
                if (reducedMotion) {
                    item {
                        StaggeredItem(index = 13) {
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
                        tint = Color.White
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

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = Color(0xFF1B1C1F),
            title = {
                Text("Clear All Data?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This will delete all transactions, budgets, accounts, and categories. This action cannot be undone.", color = Color.White.copy(alpha = 0.7f))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearConfirm = false
                }) {
                    Text("Clear", color = Color(0xFFEF5350))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1B1C1F))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF121315), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(text = subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun HapticToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1B1C1F))
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF121315), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = "Haptic Feedback", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(text = if (enabled) "On" else "Off", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50),
                uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                uncheckedTrackColor = Color(0xFF2C2C2C)
            )
        )
    }
}

@Composable
private fun ReducedMotionRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1B1C1F))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF121315), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = "Reduced motion active", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(text = "Power saving mode", color = Color(0xFFEF5350), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun NotificationEnableRow() {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1B1C1F))
            .clickable { openAppNotificationSettings(context) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF121315), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color(0xFFFFA726),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(text = "Enable notifications", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(text = "Notifications are disabled", color = Color(0xFFFFA726), fontSize = 13.sp)
            }
        }
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
private fun PickerDialog(
    title: String,
    items: List<String>,
    currentItem: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B1C1F),
        title = {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(item) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item, color = Color.White, fontSize = 15.sp)
                        if (item == currentItem) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.5f))
            }
        }
    )
}
