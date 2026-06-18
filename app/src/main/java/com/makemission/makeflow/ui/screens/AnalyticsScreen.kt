package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.ui.components.ExpressiveLineChart

@Composable
fun AnalyticsScreen(
    currency: String = "KSh",
    initialDeposit: Double = 0.0,
    transactions: List<TransactionEntity> = emptyList(),
    categories: List<CategoryEntity> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val totalInflow = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalOutflow = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    // Reconstruct the balance timeline
    val startBalance = accounts.sumOf { it.initialBalance }
    val balancePoints = remember(transactions, startBalance) {
        val points = mutableListOf<Double>()
        var rolling = startBalance
        points.add(rolling)
        
        val sortedTxs = transactions.sortedBy { it.timestamp }
        sortedTxs.forEach { tx ->
            if (tx.type == "INCOME") {
                rolling += tx.amount
            } else if (tx.type == "EXPENSE") {
                rolling -= tx.amount
            }
            points.add(rolling)
        }
        
        if (points.size == 1) {
            points.add(rolling)
        }
        points
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp)
    ) {
        Text(
            text = "Analytics",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricsPill(
                title = "Total Inflow",
                value = String.format("$currency %,.2f", totalInflow),
                modifier = Modifier.weight(1f)
            )
            MetricsPill(
                title = "Total Outflow",
                value = String.format("$currency %,.2f", totalOutflow),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Cash Flow Timeline",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        ExpressiveLineChart(
            dataPoints = balancePoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
    }
}

@Composable
fun MetricsPill(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF1E2124), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(text = title, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

