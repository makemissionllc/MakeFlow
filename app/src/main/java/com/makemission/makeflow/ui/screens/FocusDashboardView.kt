package com.makemission.makeflow.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.ui.haptic.expressiveClickable
import com.makemission.makeflow.ui.haptic.expressiveLongPressTilt
import com.makemission.makeflow.ui.theme.LocalAppDimens
import kotlin.math.min

@Composable
fun FocusDashboardView(
    onboardingName: String = "Developer",
    currency: String = "KSh",
    initialDeposit: Double = 12500.00,
    transactions: List<TransactionEntity> = emptyList(),
    categories: List<CategoryEntity> = emptyList(),
    onSettingsClick: () -> Unit = {},
    onNavigateToSubScreen: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dimens = LocalAppDimens.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, $onboardingName",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Here is your flow overview today.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1E2124), CircleShape)
                        .expressiveClickable { onSettingsClick() }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Open Settings Panel",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .expressiveLongPressTilt()
                    .background(Color(0xFF1E2124), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "VIRTUAL WALLET",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currency,
                        color = Color(0xFF4ADE80),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "••••  ••••  ••••  4209",
                    color = Color.White,
                    fontSize = 22.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "CARD HOLDER",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = onboardingName, color = Color.White, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TOTAL BALANCE",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format("$currency %,.2f", initialDeposit),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Action 1: Accounts Navigation Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1E2124), RoundedCornerShape(24.dp))
                        .expressiveClickable { onNavigateToSubScreen("ACCOUNTS") }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Accounts", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                // Action 2: Budgets Navigation Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1E2124), RoundedCornerShape(24.dp))
                        .expressiveClickable { onNavigateToSubScreen("BUDGETS") }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Budgets", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                // Action 3: Categories Navigation Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1E2124), RoundedCornerShape(24.dp))
                        .expressiveClickable { onNavigateToSubScreen("CATEGORIES") }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Categories", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Recent History",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (transactions.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF1E2124), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80)
                                )
                            }
                            Column {
                                Text(text = "Initial Account Seed", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(text = "Onboarding Deposit", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                            }
                        }
                        Text(
                            text = String.format("+$currency %,.2f", initialDeposit),
                            color = Color(0xFF4ADE80),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val count = min(transactions.size, 5)
                    for (i in 0 until count) {
                        val tx = transactions[i]
                        val category = categories.find { it.id == tx.categoryId }
                        val categoryColor = category?.colorHex?.let {
                            try {
                                Color(android.graphics.Color.parseColor(it))
                            } catch (e: Exception) {
                                Color(0xFF4ADE80)
                            }
                        } ?: Color(0xFF4ADE80)

                        val isIncome = tx.type == "INCOME"
                        val valueColor = if (isIncome) Color(0xFF4ADE80) else Color(0xFFF87171)
                        val prefix = if (isIncome) "+" else "-"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(categoryColor.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconByName(category?.iconName),
                                        contentDescription = null,
                                        tint = categoryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = tx.note.ifEmpty { category?.name ?: "Transaction" },
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = formatTimestamp(tx.timestamp),
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                text = String.format("$prefix$currency %,.2f", tx.amount),
                                color = valueColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2124).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Active Tasks", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "0 completed", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No active focus blocks. Tap + to establish a task track.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

