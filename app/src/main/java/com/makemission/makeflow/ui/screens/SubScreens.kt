package com.makemission.makeflow.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.ui.haptic.expressiveClickable
import com.makemission.makeflow.ui.haptic.expressiveLongPressTilt
import com.makemission.makeflow.ui.theme.ExpressiveMotion
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsManagementScreen(
    accountsList: List<AccountEntity>,
    onAddAccount: (AccountEntity) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.expressiveClickable { onBack() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Accounts",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Input Fields Card
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name, 
                onValueChange = { name = it }, 
                label = { Text("Account Name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = balance, 
                onValueChange = { balance = it }, 
                label = { Text("Initial Balance") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    val bal = balance.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onAddAccount(AccountEntity(UUID.randomUUID().toString(), name.trim(), bal, bal))
                        name = ""
                        balance = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .expressiveClickable {
                        val bal = balance.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank()) {
                            onAddAccount(AccountEntity(UUID.randomUUID().toString(), name.trim(), bal, bal))
                            name = ""
                            balance = ""
                        }
                    }
            ) {
                Text("Create Account", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(accountsList, key = { it.id }) { account ->
                val accountColor = try {
                    Color(android.graphics.Color.parseColor(account.colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .expressiveLongPressTilt()
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(accountColor, CircleShape)
                        )
                        Text(
                            text = account.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = String.format("KES %,.2f", account.balance),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesManagementScreen(
    categoriesList: List<CategoryEntity>,
    onAddCategory: (CategoryEntity) -> Unit,
    onBack: () -> Unit
) {
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var catName by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }

    val topLevelCategories = categoriesList.filter { it.parentCategoryId == null && it.type == selectedType }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.expressiveClickable { onBack() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Categories",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Income vs Expense Segment Selector Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            listOf("EXPENSE", "INCOME").forEach { type ->
                val isSelected = selectedType == type
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = ExpressiveMotion.bouncySpring(),
                    label = "typeBgColor"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    animationSpec = ExpressiveMotion.bouncySpring(),
                    label = "typeTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .expressiveClickable { selectedType = type; selectedParentId = null }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = type, color = textColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            value = catName, 
            onValueChange = { catName = it }, 
            label = { Text("Category/Subcategory Name") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Optional Parent (To make it a subcategory):",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val noneSelected = selectedParentId == null
            val noneBgColor by animateColorAsState(
                targetValue = if (noneSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                animationSpec = ExpressiveMotion.bouncySpring()
            )
            val noneTextColor by animateColorAsState(
                targetValue = if (noneSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                animationSpec = ExpressiveMotion.bouncySpring()
            )

            Box(
                modifier = Modifier
                    .background(noneBgColor, RoundedCornerShape(8.dp))
                    .expressiveClickable { selectedParentId = null }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("None", color = noneTextColor, fontSize = 12.sp)
            }

            topLevelCategories.forEach { parent ->
                val isSelected = selectedParentId == parent.id
                val parentBgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    animationSpec = ExpressiveMotion.bouncySpring()
                )
                val parentTextColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    animationSpec = ExpressiveMotion.bouncySpring()
                )

                Box(
                    modifier = Modifier
                        .background(parentBgColor, RoundedCornerShape(8.dp))
                        .expressiveClickable { selectedParentId = parent.id }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(parent.name, color = parentTextColor, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (catName.isNotBlank()) {
                    onAddCategory(CategoryEntity(UUID.randomUUID().toString(), catName.trim(), selectedType, selectedParentId))
                    catName = ""
                    selectedParentId = null
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .expressiveClickable {
                    if (catName.isNotBlank()) {
                        onAddCategory(CategoryEntity(UUID.randomUUID().toString(), catName.trim(), selectedType, selectedParentId))
                        catName = ""
                        selectedParentId = null
                    }
                }
        ) {
            Text("Add Category/Subcategory", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Render Tree List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(topLevelCategories, key = { it.id }) { parent ->
                val categoryColor = try {
                    Color(android.graphics.Color.parseColor(parent.colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .expressiveLongPressTilt()
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(categoryColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parent.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    // Filter and render structural subcategories right inside parent layout frame
                    val subs = categoriesList.filter { it.parentCategoryId == parent.id }
                    subs.forEach { sub ->
                        Text(
                            text = "  └─ ${sub.name}", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontSize = 14.sp, 
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsManagementScreen(
    budgetsList: List<BudgetEntity>,
    categoriesList: List<CategoryEntity>,
    onAddBudget: (BudgetEntity) -> Unit,
    onBack: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf("Monthly") }
    val expenseCategories = categoriesList.filter { it.type == "EXPENSE" }
    var selectedCategory by remember { mutableStateOf(expenseCategories.firstOrNull()) }

    LaunchedEffect(expenseCategories) {
        if (selectedCategory == null || !expenseCategories.contains(selectedCategory)) {
            selectedCategory = expenseCategories.firstOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.expressiveClickable { onBack() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Budgets Manager",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedTextField(
            value = amount, 
            onValueChange = { amount = it }, 
            label = { Text("Budget Limit Amount") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Period Track:",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Daily", "Weekly", "Monthly").forEach { p ->
                val isSelected = selectedPeriod == p
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                    animationSpec = ExpressiveMotion.bouncySpring()
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    animationSpec = ExpressiveMotion.bouncySpring()
                )

                Box(
                    modifier = Modifier
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .expressiveClickable { selectedPeriod = p }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(p, color = textColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Select Expense Target Category:",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.height(100.dp)) {
            items(expenseCategories) { cat ->
                val isSelected = selectedCategory?.id == cat.id
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    animationSpec = ExpressiveMotion.bouncySpring()
                )

                Text(
                    text = cat.name,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCategory = cat }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val cat = selectedCategory
                if (amt > 0.0 && cat != null) {
                    onAddBudget(BudgetEntity(UUID.randomUUID().toString(), cat.id, cat.name, amt, 0.0, selectedPeriod))
                    amount = ""
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .expressiveClickable {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val cat = selectedCategory
                    if (amt > 0.0 && cat != null) {
                        onAddBudget(BudgetEntity(UUID.randomUUID().toString(), cat.id, cat.name, amt, 0.0, selectedPeriod))
                        amount = ""
                    }
                }
        ) {
            Text("Create Budget Target", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(budgetsList, key = { it.id }) { budget ->
                val category = categoriesList.find { it.id == budget.categoryId }
                val categoryColor = category?.colorHex?.let {
                    try {
                        Color(android.graphics.Color.parseColor(it))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                } ?: MaterialTheme.colorScheme.primary

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .expressiveLongPressTilt()
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = budget.categoryName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${budget.period} Target",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    val progress = if (budget.amount > 0.0) (budget.spentAmount / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = categoryColor,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                    Text(
                        text = "Spent: KES ${budget.spentAmount} of KES ${budget.amount}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

