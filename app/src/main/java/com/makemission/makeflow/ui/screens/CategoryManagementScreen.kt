package com.makemission.makeflow.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.ui.components.CollapsingTopBar
import com.makemission.makeflow.ui.components.LargeTitleItem
import com.makemission.makeflow.ui.components.LocalAdaptiveConfig
import com.makemission.makeflow.ui.components.StaggeredItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CategoryManagementScreen(
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onMerge: suspend (sourceId: String, targetId: String) -> String?,
    onConvertType: suspend (categoryId: String, newType: String) -> String?,
    onCreate: suspend (CategoryEntity) -> String?,
    onDelete: suspend (categoryId: String) -> String?
) {
    val listState = rememberLazyListState()
    val config = LocalAdaptiveConfig.current
    val scope = rememberCoroutineScope()

    var mergeSource by remember { mutableStateOf<CategoryEntity?>(null) }
    var convertTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var expandedMenuId by remember { mutableStateOf<String?>(null) }

    val expenseCategories = categories.filter { it.type == "EXPENSE" }
    val incomeCategories = categories.filter { it.type == "INCOME" }

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
                LargeTitleItem(title = "Manage Categories", scrollState = listState)
            }

            item { StaggeredItem(index = 1) { SectionHeader(text = "EXPENSE CATEGORIES") } }
            if (expenseCategories.isEmpty()) {
                item {
                    Text(
                        text = "No expense categories",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            } else {
                items(expenseCategories) { category ->
                    CategoryRow(
                        category = category,
                        isExpanded = expandedMenuId == category.id,
                        onToggleMenu = { expandedMenuId = if (expandedMenuId == category.id) null else category.id },
                        onMerge = { mergeSource = category; expandedMenuId = null },
                        onConvertType = {
                            convertTarget = category
                            expandedMenuId = null
                        },
                        onDelete = {
                            deleteTarget = category
                            expandedMenuId = null
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { StaggeredItem(index = 2) { SectionHeader(text = "INCOME CATEGORIES") } }
            if (incomeCategories.isEmpty()) {
                item {
                    Text(
                        text = "No income categories",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            } else {
                items(incomeCategories) { category ->
                    CategoryRow(
                        category = category,
                        isExpanded = expandedMenuId == category.id,
                        onToggleMenu = { expandedMenuId = if (expandedMenuId == category.id) null else category.id },
                        onMerge = { mergeSource = category; expandedMenuId = null },
                        onConvertType = {
                            convertTarget = category
                            expandedMenuId = null
                        },
                        onDelete = {
                            deleteTarget = category
                            expandedMenuId = null
                        }
                    )
                }
            }
        }

        CollapsingTopBar(
            title = "Manage Categories",
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
            },
            actions = {
                IconButton(onClick = { showCreateSheet = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New Category",
                        tint = Color.White
                    )
                }
            }
        )
    }

    mergeSource?.let { source ->
        val sameType = categories.filter { it.type == source.type && it.id != source.id }
        if (sameType.isEmpty()) {
            AlertDialog(
                onDismissRequest = { mergeSource = null },
                containerColor = Color(0xFF1B1C1F),
                title = { Text("Cannot Merge", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("No other categories of type \"${source.type}\" available to merge into.", color = Color.White.copy(alpha = 0.7f)) },
                confirmButton = { TextButton(onClick = { mergeSource = null }) { Text("OK", color = Color.White) } }
            )
        } else {
            MergeTargetDialog(
                source = source,
                targets = sameType,
                onDismiss = { mergeSource = null },
                onConfirm = { target ->
                    scope.launch(Dispatchers.IO) {
                        val error = onMerge(source.id, target.id)
                        if (error != null) resultMessage = error
                    }
                    mergeSource = null
                }
            )
        }
    }

    convertTarget?.let { category ->
        val newType = if (category.type == "EXPENSE") "INCOME" else "EXPENSE"
        AlertDialog(
            onDismissRequest = { convertTarget = null },
            containerColor = Color(0xFF1B1C1F),
            title = { Text("Convert Type", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Change \"${category.name}\" from ${category.type} to $newType?",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val error = onConvertType(category.id, newType)
                        if (error != null) resultMessage = error
                    }
                    convertTarget = null
                }) {
                    Text("Convert", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { convertTarget = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }
            }
        )
    }

    deleteTarget?.let { category ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = Color(0xFF1B1C1F),
            title = { Text("Delete Category", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Delete \"${category.name}\"? This action cannot be undone.",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val error = onDelete(category.id)
                        if (error != null) resultMessage = error
                    }
                    deleteTarget = null
                }) {
                    Text("Delete", color = Color(0xFFEF5350))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }
            }
        )
    }

    if (showCreateSheet) {
        CreateCategoryBottomSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { entity ->
                val error = onCreate(entity)
                if (error != null) resultMessage = error
                null
            },
            parentCategories = categories,
            existingType = "EXPENSE"
        )
    }

    resultMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            containerColor = Color(0xFF1B1C1F),
            title = { Text("Info", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = { TextButton(onClick = { resultMessage = null }) { Text("OK", color = Color.White) } }
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
private fun CategoryRow(
    category: CategoryEntity,
    isExpanded: Boolean,
    onToggleMenu: () -> Unit,
    onMerge: () -> Unit,
    onConvertType: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1B1C1F))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF121315), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (category.parentCategoryId != null) {
                Text("—", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
            } else {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = category.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (category.parentCategoryId != null) {
                Text(
                    text = "Subcategory",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 12.sp
                )
            }
        }
        Box {
            IconButton(onClick = onToggleMenu) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Actions",
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = onToggleMenu,
                containerColor = Color(0xFF2C2C2C)
            ) {
                DropdownMenuItem(
                    text = { Text("Merge into...", color = Color.White, fontSize = 14.sp) },
                    onClick = onMerge
                )
                val newTypeLabel = if (category.type == "EXPENSE") "Convert to INCOME" else "Convert to EXPENSE"
                DropdownMenuItem(
                    text = { Text(newTypeLabel, color = Color.White, fontSize = 14.sp) },
                    onClick = onConvertType
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFEF5350), fontSize = 14.sp) },
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun MergeTargetDialog(
    source: CategoryEntity,
    targets: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (CategoryEntity) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B1C1F),
        title = {
            Text(
                "Merge \"${source.name}\" into...",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "All transactions and budgets assigned to \"${source.name}\" will be reassigned to the target. The source category will be deleted.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                targets.forEach { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onConfirm(target) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = target.name,
                            color = Color.White,
                            fontSize = 15.sp
                        )
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
