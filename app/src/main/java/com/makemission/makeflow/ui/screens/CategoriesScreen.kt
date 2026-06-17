package com.makemission.makeflow.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.TaskEntity
import com.makemission.makeflow.ui.components.AddCategoryBottomSheet
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.viewmodel.CategoriesViewModel
import com.makemission.makeflow.ui.viewmodel.CategoryDetailState
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = viewModel()
) {
    Log.d("MakeFlowDebug", "CategoriesScreen composing")
    val dimens = LocalAppDimens.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()

    var showSheet by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = detailState != null,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally { it } + fadeIn(animationSpec = tween(350)))
                    .togetherWith(
                        slideOutHorizontally { -it / 3 } + fadeOut(animationSpec = tween(250))
                    )
            } else {
                (slideInHorizontally { -it } + fadeIn(animationSpec = tween(350)))
                    .togetherWith(
                        slideOutHorizontally { it / 3 } + fadeOut(animationSpec = tween(250))
                    )
            }
        },
        label = "category_morph"
    ) { showDetail ->
        if (showDetail) {
            detailState?.let { detail ->
                CategoryDetailView(
                    detailState = detail,
                    onBack = { viewModel.onDismissDetail() },
                    onToggleTask = { viewModel.onToggleTaskInDetailScreen(it) }
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    CategoriesTopBar()
                    Spacer(modifier = Modifier.height(dimens.paddingSmall))

                    if (uiState.isLoading) {
                        Text(
                            text = "Loading categories...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(dimens.paddingMedium)
                        )
                    } else if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(dimens.paddingMedium)
                        )
                    } else {
                        TotalBudgetCard(
                            totalSpent = uiState.totalSpent,
                            totalBudget = uiState.totalBudget
                        )
                        Spacer(modifier = Modifier.height(dimens.paddingMedium))
                        uiState.categories.forEach { category ->
                            CategorySpendingItem(
                                category = category,
                                spendingMap = uiState.spendingMap,
                                onClick = { viewModel.onCategoryClick(category.id) },
                                onDelete = { viewModel.onDeleteCategory(category.id) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Spacer(modifier = Modifier.height(dimens.paddingMedium))
                    }
                }

                AddCategoryButton(onClick = { showSheet = true })
            }

            if (showSheet) {
                AddCategoryBottomSheet(
                    viewModel = viewModel,
                    onDismiss = { showSheet = false }
                )
            }
        }
    }
}

@Composable
private fun CategoryDetailView(
    detailState: CategoryDetailState,
    onBack: () -> Unit,
    onToggleTask: (TaskEntity) -> Unit
) {
    val dimens = LocalAppDimens.current
    val category = detailState.categoryWithTasks.category
    val tasks = detailState.categoryWithTasks.tasks
    val spent = detailState.spent
    val budget = detailState.budget
    val progress = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1f) else 0f
    val isOverBudget = budget > 0 && spent > budget
    val fmt = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = dimens.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(dimens.iconSizeLarge)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForCategoryName(category.iconName, category.name),
                    contentDescription = category.name,
                    modifier = Modifier.size(dimens.iconSizeMedium - 2.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(dimens.paddingSmall + 4.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.paddingMedium),
            shape = RoundedCornerShape(dimens.cornerRadiusLarge),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = dimens.cardElevation)
        ) {
            Column(modifier = Modifier.padding(dimens.paddingLarge)) {
                Text(
                    text = "BUDGET",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(dimens.paddingSmall))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$${fmt.format(spent)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(dimens.paddingSmall))
                    Text(
                        text = "/ $${fmt.format(budget)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(dimens.paddingSmall + 4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.barHeight)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isOverBudget) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                if (isOverBudget) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Over budget",
                            modifier = Modifier.size(dimens.iconSizeSmall),
                            tint = Color(0xFFE53935)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Over by $${fmt.format(spent - budget)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(dimens.paddingLarge))

        Text(
            text = "Tasks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = dimens.paddingMedium)
        )
        Spacer(modifier = Modifier.height(dimens.paddingSmall))

        if (tasks.isEmpty()) {
            Text(
                text = "No tasks assigned to this category.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dimens.paddingMedium)
            )
        } else {
            tasks.forEach { task ->
                TaskRowInDetail(
                    task = task,
                    onToggle = { onToggleTask(task) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(dimens.paddingLarge))
    }
}

@Composable
private fun TaskRowInDetail(
    task: TaskEntity,
    onToggle: () -> Unit
) {
    val dimens = LocalAppDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = dimens.paddingMedium, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() }
        )
        Spacer(modifier = Modifier.width(dimens.paddingSmall))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                color = if (task.isCompleted)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onBackground,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CategoriesTopBar() {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingSmall + 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun TotalBudgetCard(totalSpent: Double, totalBudget: Double) {
    val dimens = LocalAppDimens.current
    val percentage = if (totalBudget > 0) (totalSpent / totalBudget * 100).toInt() else 0
    val progress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
    val fmt = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val daysLeft = remember {
        val cal = Calendar.getInstance()
        cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingSmall),
        shape = RoundedCornerShape(dimens.cornerRadiusLarge),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = dimens.cardElevation)
    ) {
        Column(modifier = Modifier.padding(dimens.paddingLarge)) {
            Text(
                text = "TOTAL SPENT THIS MONTH",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(dimens.paddingSmall))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$${fmt.format(totalSpent)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(dimens.paddingSmall))
                Text(
                    text = "/ $${fmt.format(totalBudget)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$percentage% of budget",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(dimens.paddingSmall + 4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.barHeight)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progress >= 1f) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(dimens.paddingSmall))
            Text(
                text = "$daysLeft days left",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategorySpendingItem(
    category: CategoryEntity,
    spendingMap: Map<String, Double>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dimens = LocalAppDimens.current
    val fmt = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val spent = spendingMap[category.id] ?: 0.0
    val isOverBudget = category.monthlyBudget > 0 && spent > category.monthlyBudget
    val progress = if (category.monthlyBudget > 0)
        (spent / category.monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium)
            .clickable { onClick() },
        shape = RoundedCornerShape(dimens.cornerRadiusMedium),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = dimens.cardElevation)
    ) {
        Column(modifier = Modifier.padding(dimens.paddingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(dimens.iconSizeLarge)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForCategoryName(category.iconName, category.name),
                        contentDescription = category.name,
                        modifier = Modifier.size(dimens.iconSizeMedium - 2.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(dimens.paddingSmall + 4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$${fmt.format(spent)} / $${fmt.format(category.monthlyBudget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(dimens.iconSizeMedium - 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(dimens.paddingSmall + 4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.barHeight - 2.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOverBudget) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (isOverBudget) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Over budget",
                        modifier = Modifier.size(dimens.iconSizeSmall),
                        tint = Color(0xFFE53935)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Over by $${fmt.format(spent - category.monthlyBudget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCategoryButton(onClick: () -> Unit) {
    val dimens = LocalAppDimens.current
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingSmall + 4.dp),
        shape = RoundedCornerShape(dimens.cornerRadiusMedium),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(dimens.iconSizeMedium - 2.dp)
        )
        Spacer(modifier = Modifier.width(dimens.paddingSmall))
        Text(
            text = "Add New Category",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun iconForCategoryName(iconName: String, categoryName: String): ImageVector {
    if (iconName.isNotBlank()) {
        return when (iconName) {
            "Home" -> Icons.Filled.Home
            "ShoppingCart" -> Icons.Filled.ShoppingCart
            "LocationOn" -> Icons.Filled.LocationOn
            "Star" -> Icons.Filled.Star
            "Check" -> Icons.Filled.Check
            else -> Icons.Filled.Check
        }
    }
    return when (categoryName) {
        "Electronics", "Food & Dining" -> Icons.Filled.ShoppingCart
        "Transportation" -> Icons.Filled.LocationOn
        "Entertainment" -> Icons.Filled.Star
        "Housing" -> Icons.Filled.Home
        "Income" -> Icons.Filled.Check
        else -> Icons.Filled.Check
    }
}
