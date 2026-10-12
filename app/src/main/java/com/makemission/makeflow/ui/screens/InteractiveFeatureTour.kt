package com.makemission.makeflow.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.makemission.makeflow.R
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.data.changelog.FeatureGuideEntry
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalExpressiveMotion
import com.makemission.makeflow.ui.theme.OverlayTransition

@Composable
fun InteractiveFeatureTour(
    entries: List<FeatureGuideEntry>,
    layoutMode: DeviceLayoutMode,
    reducedMotion: Boolean,
    hapticManager: HapticManager?,
    onExit: () -> Unit,
    onTryTransactionEntry: () -> Unit
) {
    val dimens = LocalAppDimens.current
    var selectedStep by rememberSaveable { mutableIntStateOf(0) }
    val expanded = layoutMode == DeviceLayoutMode.EXPANDED
    val step = selectedStep.coerceIn(0, entries.lastIndex)

    if (expanded) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(0.34f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(dimens.cornerRadiusLarge))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(dimens.spacingSmall),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingXSmall)
            ) {
                itemsIndexed(entries, key = { _, entry -> entry.title }) { index, entry ->
                    val isSelected = index == step
                    Text(
                        text = entry.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .clickable {
                                hapticManager?.fire(HapticEvent.NAV_SELECT)
                                selectedStep = index
                            }
                            .padding(horizontal = dimens.paddingMedium, vertical = dimens.spacingMedium),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(0.66f)
                    .fillMaxHeight()
                    .widthIn(max = dimens.onboardingContentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
            ) {
                TourStepContent(
                    entry = entries[step],
                    step = step,
                    totalSteps = entries.size,
                    reducedMotion = reducedMotion,
                    hapticManager = hapticManager,
                    onTryTransactionEntry = onTryTransactionEntry
                )
                TourControls(
                    step = step,
                    totalSteps = entries.size,
                    hapticManager = hapticManager,
                    onPrevious = { selectedStep = (step - 1).coerceAtLeast(0) },
                    onNext = { selectedStep = (step + 1).coerceAtMost(entries.lastIndex) },
                    onExit = onExit
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = dimens.onboardingContentMaxWidth)
                .padding(horizontal = dimens.paddingLarge),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
        ) {
            TourStepContent(
                entry = entries[step],
                step = step,
                totalSteps = entries.size,
                reducedMotion = reducedMotion,
                hapticManager = hapticManager,
                onTryTransactionEntry = onTryTransactionEntry,
                modifier = Modifier.weight(1f)
            )
            TourControls(
                step = step,
                totalSteps = entries.size,
                hapticManager = hapticManager,
                onPrevious = { selectedStep = (step - 1).coerceAtLeast(0) },
                onNext = { selectedStep = (step + 1).coerceAtMost(entries.lastIndex) },
                onExit = onExit
            )
        }
    }
}

@Composable
private fun TourStepContent(
    entry: FeatureGuideEntry,
    step: Int,
    totalSteps: Int,
    reducedMotion: Boolean,
    hapticManager: HapticManager?,
    onTryTransactionEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalAppDimens.current
    val expressiveMotion = LocalExpressiveMotion.current
    val illustrationScale by animateFloatAsState(
        targetValue = if (reducedMotion || step % 2 == 0) 1f else 1f + (1f - expressiveMotion.pressScale),
        animationSpec = if (reducedMotion) OverlayTransition.reducedEnter else expressiveMotion.scaleAnimationSpec,
        label = "tourIllustrationScale"
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
    ) {
        Text(
            text = stringResource(R.string.tour_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.tour_step_of, step + 1, totalSteps),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val transition: ContentTransform = if (reducedMotion) {
                    fadeIn(OverlayTransition.reducedEnter) togetherWith fadeOut(OverlayTransition.reducedExit)
                } else {
                    val direction = if (targetState >= initialState) 1 else -1
                    (fadeIn(OverlayTransition.enterSpring) + slideInHorizontally(
                        animationSpec = OverlayTransition.enterSlide,
                        initialOffsetX = { direction * it / 5 }
                    )) togetherWith (fadeOut(OverlayTransition.exitSpring) + slideOutHorizontally(
                        animationSpec = OverlayTransition.exitSlide,
                        targetOffsetX = { -direction * it / 5 }
                    ))
                }
                transition
            },
            label = "featureTourStep"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = illustrationScale
                            scaleY = illustrationScale
                        }
                        .clip(RoundedCornerShape(dimens.cornerRadiusLarge))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = dimens.paddingLarge, vertical = dimens.paddingMedium),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.title,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("tour_step_features"),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
                ) {
                    itemsIndexed(entry.features, key = { index, feature -> "$index-$feature" }) { _, feature ->
                        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingMedium)) {
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = feature,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
        if (entry.title == "Transaction entry") {
            Button(onClick = {
                hapticManager?.fire(HapticEvent.NAV_SELECT)
                onTryTransactionEntry()
            }) {
                Text(stringResource(R.string.tour_try_transaction))
            }
        }
    }
}

@Composable
private fun TourControls(
    step: Int,
    totalSteps: Int,
    hapticManager: HapticManager?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = {
            hapticManager?.fire(HapticEvent.NAV_SELECT)
            if (step == 0) onExit() else onPrevious()
        }) {
            Text(stringResource(if (step == 0) R.string.tour_back_to_guide else R.string.tour_previous))
        }
        if (step < totalSteps - 1) {
            Button(onClick = {
                hapticManager?.fire(HapticEvent.NAV_SELECT)
                onNext()
            }) {
                Text(stringResource(R.string.tour_next))
            }
        } else {
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(dimens.shapeCornerPill)),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                TextButton(onClick = {
                    hapticManager?.fire(HapticEvent.NAV_SELECT)
                    onExit()
                }) {
                    Text(stringResource(R.string.tour_finish))
                }
            }
        }
    }
}
