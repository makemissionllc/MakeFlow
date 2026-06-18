package com.makemission.makeflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
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

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Accounts", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        // Input Fields Card
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name, 
                onValueChange = { name = it }, 
                label = { Text("Account Name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = balance, 
                onValueChange = { balance = it }, 
                label = { Text("Initial Balance") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Account", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(accountsList) { account ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E2124), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(account.name, color = Color.White, fontWeight = FontWeight.Medium)
                    Text(String.format("KES %,.2f", account.balance), color = Color.White, fontWeight = FontWeight.Bold)
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

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Categories", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        
        // Income vs Expense Segment Selector Switch
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            listOf("EXPENSE", "INCOME").forEach { type ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) Color.White else Color(0xFF1E2124), RoundedCornerShape(12.dp))
                        .clickable { selectedType = type; selectedParentId = null }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(type, color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            value = catName, 
            onValueChange = { catName = it }, 
            label = { Text("Category/Subcategory Name") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        Text("Optional Parent (To make it a subcategory):", color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(if(selectedParentId == null) Color.Gray else Color.DarkGray, RoundedCornerShape(8.dp))
                    .clickable { selectedParentId = null }
                    .padding(8.dp)
            ) {
                Text("None", color = Color.White, fontSize = 12.sp)
            }
            topLevelCategories.forEach { parent ->
                Box(
                    modifier = Modifier
                        .background(if(selectedParentId == parent.id) Color.Gray else Color.DarkGray, RoundedCornerShape(8.dp))
                        .clickable { selectedParentId = parent.id }
                        .padding(8.dp)
                ) {
                    Text(parent.name, color = Color.White, fontSize = 12.sp)
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
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Category/Subcategory", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Render Tree List
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(topLevelCategories) { parent ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E2124), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(parent.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    
                    // Filter and render structural subcategories right inside parent layout frame
                    val subs = categoriesList.filter { it.parentCategoryId == parent.id }
                    subs.forEach { sub ->
                        Text(
                            text = "  └─ ${sub.name}", 
                            color = Color.White.copy(alpha = 0.7f), 
                            fontSize = 14.sp, 
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
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

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Budgets Manager", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedTextField(
            value = amount, 
            onValueChange = { amount = it }, 
            label = { Text("Budget Limit Amount") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        Text("Period Track:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Daily", "Weekly", "Monthly").forEach { p ->
                val isSelected = selectedPeriod == p
                Box(
                    modifier = Modifier
                        .background(if (isSelected) Color.White else Color.DarkGray, RoundedCornerShape(8.dp))
                        .clickable { selectedPeriod = p }
                        .padding(8.dp)
                ) {
                    Text(p, color = if (isSelected) Color.Black else Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Select Expense Target Category:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.height(100.dp)) {
            items(expenseCategories) { cat ->
                Text(
                    text = cat.name,
                    color = if (selectedCategory?.id == cat.id) Color.Green else Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCategory = cat }
                        .padding(8.dp)
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
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Budget Target", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(budgetsList) { budget ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E2124), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(budget.categoryName, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${budget.period} Target", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                    val progress = if (budget.amount > 0.0) (budget.spentAmount / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = Color.Red,
                        trackColor = Color.Gray
                    )
                    Text("Spent: KES ${budget.spentAmount} of KES ${budget.amount}", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}
