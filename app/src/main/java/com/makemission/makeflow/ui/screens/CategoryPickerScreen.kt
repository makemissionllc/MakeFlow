package com.makemission.makeflow.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity

private data class CategoryListItem(
    val entity: CategoryEntity,
    val indentLevel: Int
)

@Composable
fun CategoryPickerScreen(
    selectedType: String,
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    accounts: List<AccountEntity>,
    selectedAccountId: String?,
    currencyCode: String,
    onItemSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val isTransfer = selectedType == "TRANSFER"

    val categoryItems = remember(categories, selectedType) {
        if (isTransfer) emptyList()
        else {
            val parents = categories.filter { it.type == selectedType && it.parentCategoryId == null }
            parents.flatMap { parent ->
                val children = categories.filter { it.parentCategoryId == parent.id }
                listOf(CategoryListItem(parent, 0)) + children.map { CategoryListItem(it, 1) }
            }
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = if (isTransfer) "Select Account" else "Select Category",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (isTransfer) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    val isSelected = account.id == selectedAccountId
                    AccountRow(
                        name = account.name,
                        balance = account.balance,
                        currencyCode = currencyCode,
                        isSelected = isSelected,
                        onClick = { onItemSelected(account.id) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categoryItems, key = { it.entity.id }) { item ->
                    val isSelected = item.entity.id == selectedCategoryId
                    CategoryRow(
                        category = item.entity,
                        indentLevel = item.indentLevel,
                        isSelected = isSelected,
                        onClick = { onItemSelected(item.entity.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    name: String,
    balance: Double,
    currencyCode: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF2C2C2E) else Color(0xFF1B1C1F))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = CurrencyFormatter.format(balance, currencyCode),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryEntity,
    indentLevel: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isSelected -> Color(0xFF2C2C2E)
                    indentLevel == 1 -> Color(0xFF161618)
                    else -> Color(0xFF1B1C1F)
                }
            )
            .clickable(onClick = onClick)
            .padding(start = 12.dp + (indentLevel * 24).dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name,
                color = Color.White,
                fontWeight = if (indentLevel == 0) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = if (indentLevel == 0) 15.sp else 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (indentLevel == 0 && category.monthlyBudget > 0) {
                    "${category.type.lowercase().replaceFirstChar { it.uppercase() }} · Budget: ${CurrencyFormatter.format(category.monthlyBudget, "KSH")}"
                } else {
                    category.type.lowercase().replaceFirstChar { it.uppercase() }
                },
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
