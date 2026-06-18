package com.makemission.makeflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MakeFlowTab {
    FOCUS, HISTORY, INSIGHTS
}

@Composable
fun ExpressiveNavDock(
    currentTab: MakeFlowTab,
    onTabSelected: (MakeFlowTab) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = Color(0xFF1E2124),
                    shape = CircleShape
                )
                .padding(6.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MorphingDockItem(
                selected = currentTab == MakeFlowTab.FOCUS,
                activeIcon = Icons.Rounded.Home,
                inactiveIcon = Icons.Outlined.Home,
                label = "Focus",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTabSelected(MakeFlowTab.FOCUS)
                }
            )

            MorphingDockItem(
                selected = currentTab == MakeFlowTab.HISTORY,
                activeIcon = Icons.Rounded.History,
                inactiveIcon = Icons.Outlined.History,
                label = "History",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTabSelected(MakeFlowTab.HISTORY)
                }
            )

            MorphingDockItem(
                selected = currentTab == MakeFlowTab.INSIGHTS,
                activeIcon = Icons.Rounded.BarChart,
                inactiveIcon = Icons.Outlined.BarChart,
                label = "Insights",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTabSelected(MakeFlowTab.INSIGHTS)
                }
            )
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = Color.White,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFabClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add Transaction",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun RowScope.MorphingDockItem(
    selected: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.Black else Color.White.copy(alpha = 0.6f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    Row(
        modifier = Modifier
            .height(44.dp)
            .background(color = containerColor, shape = CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) activeIcon else inactiveIcon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )

        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
        ) {
            Row {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
