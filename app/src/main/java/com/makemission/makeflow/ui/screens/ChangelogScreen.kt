package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.makemission.makeflow.R
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.data.changelog.changelogEntries
import com.makemission.makeflow.data.changelog.featureGuideEntries
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.PrimaryDockContentInset
import com.makemission.makeflow.ui.theme.LocalAppDimens

@Composable
fun ChangelogScreen(
    onBack: () -> Unit,
    hapticManager: HapticManager? = null,
    onStartTransactionEntry: () -> Unit = {}
) {
    val dimens = LocalAppDimens.current
    val config = LocalAdaptiveConfig.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showInteractiveTour by rememberSaveable { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotion.current
    val layoutMode = LocalDeviceLayoutMode.current
    val updatesState = rememberLazyListState()
    val guideState = rememberLazyListState()
    val contentWidth = if (showInteractiveTour && layoutMode == com.makemission.makeflow.core.layout.DeviceLayoutMode.EXPANDED) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.widthIn(max = dimens.onboardingContentMaxWidth).fillMaxWidth()
    }
    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        Box(modifier = Modifier.fillMaxSize().padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())) {
            Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxHeight()
                .then(contentWidth)) {
                Row(modifier = Modifier.fillMaxWidth().heightIn(min = config.barHeight)
                    .padding(horizontal = dimens.paddingMedium, vertical = dimens.spacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                    IconButton(onClick = {
                        hapticManager?.fire(HapticEvent.NAV_SELECT)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.whats_new_back))
                    }
                    Text(stringResource(R.string.whats_new_title), style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
                    listOf(R.string.whats_new_updates, R.string.whats_new_guide).forEachIndexed { index, label ->
                        Tab(selected = selectedTab == index, onClick = {
                            hapticManager?.fire(HapticEvent.TAB_SWITCH)
                            selectedTab = index
                        }, text = { Text(stringResource(label)) })
                    }
                }
                if (showInteractiveTour) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(dimens.paddingLarge)) {
                        InteractiveFeatureTour(
                            entries = featureGuideEntries,
                            layoutMode = layoutMode,
                            reducedMotion = reducedMotion,
                            hapticManager = hapticManager,
                            onExit = { showInteractiveTour = false },
                            onTryTransactionEntry = onStartTransactionEntry
                        )
                    }
                } else {
                    LazyColumn(
                        state = if (selectedTab == 0) updatesState else guideState,
                        modifier = Modifier.weight(1f).fillMaxWidth().testTag("whats_new_list"),
                        contentPadding = PaddingValues(start = dimens.paddingLarge, end = dimens.paddingLarge,
                            top = dimens.paddingLarge, bottom = PrimaryDockContentInset()),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingXLarge)
                    ) {
                        if (selectedTab == 0) {
                            items(changelogEntries, key = { it.version }) { entry ->
                                ChangeSection("v${entry.version}", entry.date, entry.changes)
                            }
                        } else {
                            item(key = "interactive_tour") {
                                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSmall)) {
                                    Text(
                                        text = stringResource(R.string.tour_start_description),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = {
                                            hapticManager?.fire(HapticEvent.NAV_SELECT)
                                            showInteractiveTour = true
                                        },
                                        modifier = Modifier.testTag("start_interactive_tour")
                                    ) {
                                        Text(stringResource(R.string.tour_start))
                                    }
                                }
                            }
                            items(featureGuideEntries, key = { it.title }) { entry ->
                                ChangeSection(entry.title, null, entry.features)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeSection(title: String, subtitle: String?, changes: List<String>) {
    val dimens = LocalAppDimens.current
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        changes.forEach { change ->
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                Text("\u2022", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(change, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = dimens.spacingMedium))
    }
}
