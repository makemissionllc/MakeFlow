package com.makemission.makeflow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MakeFlowTab {
    HOME, INSIGHTS, HISTORY
}

@Composable
fun MainScreen(
    username: String,
    onLogout: () -> Unit
) {
    var activeTab by remember { mutableStateOf(MakeFlowTab.HOME) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121315))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            when (activeTab) {
                MakeFlowTab.HOME -> {
                    Text(text = "Home Dashboard for $username", color = Color.White, fontSize = 20.sp)
                }
                MakeFlowTab.INSIGHTS -> {
                    Text(text = "Insights Analytics Metric Frame", color = Color.White, fontSize = 20.sp)
                }
                MakeFlowTab.HISTORY -> {
                    Text(text = "Transaction History Ledger Track", color = Color.White, fontSize = 20.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            FloatingCapsuleNavBar(
                selectedTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        }
    }
}

@Composable
fun FloatingCapsuleNavBar(
    selectedTab: MakeFlowTab,
    onTabSelected: (MakeFlowTab) -> Unit
) {
    val tabs = MakeFlowTab.values()

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF24262B))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(tab) }
                    .padding(horizontal = if (isSelected) 16.dp else 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (tab) {
                        MakeFlowTab.HOME -> Icons.Rounded.SpaceDashboard
                        MakeFlowTab.INSIGHTS -> Icons.Rounded.Insights
                        MakeFlowTab.HISTORY -> Icons.Rounded.History
                    },
                    contentDescription = tab.name,
                    tint = if (isSelected) Color(0xFF121315) else Color.White,
                    modifier = Modifier.size(20.dp)
                )

                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh))
                ) {
                    Text(
                        text = when (tab) {
                            MakeFlowTab.HOME -> "Home"
                            MakeFlowTab.INSIGHTS -> "Insights"
                            MakeFlowTab.HISTORY -> "History"
                        },
                        color = Color(0xFF121315),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
