@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.makemission.makeflow.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.makemission.makeflow.ui.haptic.lightTick
import com.makemission.makeflow.ui.haptic.mediumConfirm
import com.makemission.makeflow.ui.haptic.strongPress
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.viewmodel.CategoriesViewModel
import com.makemission.makeflow.ui.viewmodel.CategoryDetailState
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private val fastEnter = fadeIn(
    animationSpec = tween(
        durationMillis = 350,
        easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    )
)

private val slowExit = fadeOut(
    animationSpec = tween(
        durationMillis = 400,
        delayMillis = 100,
        easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    )
)

private val fastFadeIn = fadeIn(
    animationSpec = tween(
        durationMillis = 350,
        easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    )
)

private val slowFadeOut = fadeOut(
    animationSpec = tween(
        durationMillis = 200,
        easing = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
    )
)

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = viewModel()
) {
    Log.d("MakeFlowDebug", "CategoriesScreen composing")

    val dimens = LocalAppDimens.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    SharedTransitionLayout {
        AnimatedVisibility(
            visible = detailState == null,
            enter = fastEnter,
            exit = slowExit
        ) {
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

                        Spacer(modifier = Modifier.height(dimens.paddingSmall))

                        uiState.categories.forEach { cat ->
                            CategorySpendingItem(
                                category = cat,
                                spendingMap = uiState.spendingMap,
                                isExpanding = selectedCategoryId == cat.id,
                                onClick = {
                                    selectedCategoryId = cat.id
                                    viewModel.onCategoryClick(cat.id)
                                },
                                onDelete = {
                                    viewModel.onDeleteCategory(cat.id)
                                }
                            )
                            Spacer(modifier = Modifier.height(dimens.paddingSmall))
                        }
                    }
                }

                AddCategoryButton(onClick = { showSheet = true })
            }
        }

        AnimatedVisibility(
            visible = detailState != null,
            enter = fastFadeIn,
            exit = slowFadeOut
        ) {
            detailState?.let { detail ->
                with(this@SharedTransitionLayout) {
                    CategoryDetailView(
                        animatedVisibilityScope = this@AnimatedVisibility,
                        detailState = detail,
                        onBack = {
                            selectedCategoryId = null
                            viewModel.onDismissDetail()
                        },
                        onToggleTask = { viewModel.onToggleTaskInDetailScreen(it) }
                    )
                }
            }
        }
    }

    if (showSheet) {
        AddCategoryBottomSheet(
            viewModel = viewModel,
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
private fun SharedTransitionScope.CategoryDetailView(
    animatedVisibilityScope: AnimatedVisibilityScope,
    detailState: CategoryDetailState,
    onBack: () -> Unit,
    onToggleTask: (TaskEntity) -> Unit
) {
    val haptic = LocalHapticFeedback.current
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

    val containerState = rememberSharedContentState(key = "container_${category.id}")
    val iconState = rememberSharedContentState(key = "icon_${category.id}")
    val nameState = rememberSharedContentState(key = "name_${category.id}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .sharedBounds(
                sharedContentState = containerState,
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = 300f) }
            )
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = dimens.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { haptic.lightTick(); onBack() }) {
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .sharedElement(
                        state = iconState,
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = 300f) }
                    ),
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
                modifier = Modifier
                    .weight(1f)
                    .sharedElement(
                        state = nameState,
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = 300f) }
                    )
            )
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.paddingMedium),
            shape = RoundedCornerShape(dimens.cornerRadiusMedium),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = dimens.cardElevation)
        ) {
            Column(modifier = Modifier.padding(dimens.paddingMedium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isOverBudget) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Over Budget",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(dimens.iconSizeSmall)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$${fmt.format(spent)} / $${fmt.format(budget)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(dimens.paddingSmall))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isOverBudget) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        Text(
            text = "Tasks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = dimens.paddingMedium)
        )

        Spacer(modifier = Modifier.height(dimens.paddingSmall))

        tasks.forEach { task ->
            TaskRowInDetail(
                task = task,
                onToggle = { onToggleTask(task) }
            )
        }

        if (tasks.isEmpty()) {
            Text(
                text = "No tasks for this category",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dimens.paddingMedium)
            )
        }
    }
}

@Composable
private fun TaskRowInDetail(
    task: TaskEntity,
    onToggle: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dimens = LocalAppDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!task.isCompleted) haptic.mediumConfirm() else haptic.lightTick()
                onToggle()
            }
            .padding(horizontal = dimens.paddingMedium, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = {
                if (!task.isCompleted) haptic.mediumConfirm() else haptic.lightTick()
                onToggle()
            }
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
                    .clip(RoundedCornerShape(dimens.barHeight / 2)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(dimens.paddingSmall))

            Text(
                text = "$daysLeft days left in month",
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
    isExpanding: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
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
            .clickable { haptic.lightTick(); onClick() },
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
                        color = if (isOverBudget) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { haptic.strongPress(); onDelete() }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.paddingSmall))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isOverBudget) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun AddCategoryButton(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val dimens = LocalAppDimens.current

    Button(
        onClick = { haptic.strongPress(); onClick() },
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
