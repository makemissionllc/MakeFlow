package com.makemission.makeflow.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makemission.makeflow.data.local.AppDatabase
import com.makemission.makeflow.data.local.DatabaseInitializer
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.TaskEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import com.makemission.makeflow.ui.components.AddTransactionBottomSheet
import com.makemission.makeflow.ui.haptic.expressiveLongPressTilt
import com.makemission.makeflow.ui.haptic.lightTick
import com.makemission.makeflow.ui.haptic.mediumConfirm
import com.makemission.makeflow.ui.haptic.strongPress
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalExpressiveMotion
import com.makemission.makeflow.ui.viewmodel.CategoriesViewModel
import com.makemission.makeflow.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.min

@Composable
fun DashboardScreen(
    taskViewModel: DashboardViewModel = viewModel(),
    categoriesViewModel: CategoriesViewModel = viewModel()
) {
    Log.d("MakeFlowDebug", "DashboardScreen composing")

    val context = LocalContext.current
    val db = try {
        AppDatabase.getDatabase(context)
    } catch (e: Exception) {
        Log.e("MakeFlowDebug", "DashboardScreen getDatabase failed", e)
        return
    }

    val accounts by db.accountDao().getAllAccounts().collectAsState(initial = emptyList())
    val transactions by db.transactionDao().getAllTransactions().collectAsState(initial = emptyList())
    val taskUiState by taskViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                if (db.accountDao().getAllAccounts().first().isEmpty()) {
                    DatabaseInitializer.populate(db)
                }
            }
        } catch (e: Exception) {
            Log.e("MakeFlowDebug", "Database seed failed", e)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showAddTransaction by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val dimens = LocalAppDimens.current

    val collapseProgress by remember {
        derivedStateOf {
            val scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
            val viewportHeight = listState.layoutInfo.viewportEndOffset.toFloat()
            val threshold = viewportHeight * 0.4f
            (scrollOffset / threshold).coerceIn(0f, 1f)
        }
    }

    val isCollapsed by remember {
        derivedStateOf {
            collapseProgress >= 0.95f
        }
    }

    val totalBalance = accounts.sumOf { it.balance }
    val formatted = formatCurrencyParts(totalBalance)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "balance_card") {
                CollapsingBalanceCard(
                    collapseProgress = collapseProgress,
                    formatted = formatted,
                    accounts = accounts,
                    onActionClick = { }
                )
            }

            item(key = "action_chips") {
                ActionChipsRow()
            }

            item(key = "my_cards_section") {
                MyCardsSection(accounts)
            }

            item(key = "recent_transactions_header") {
                RecentTransactionsHeader()
            }

            items(
                count = min(transactions.size, 5),
                key = { "tx_${transactions[it].id}" }
            ) { index ->
                ExpressiveTransactionRow(tx = transactions[index])
            }

            item(key = "task_section_header") {
                TaskSectionHeader(
                    state = taskUiState,
                    onAddClick = { showAddDialog = true }
                )
            }

            items(
                count = taskUiState.tasks.size,
                key = { taskUiState.tasks[it].id }
            ) { index ->
                val task = taskUiState.tasks[index]
                ExpressiveTaskItem(
                    task = task,
                    onToggle = { taskViewModel.onToggleTaskCompletion(task) },
                    onDelete = { taskViewModel.onDeleteTask(task) }
                )
            }

            if (taskUiState.tasks.isEmpty() && !taskUiState.isLoading) {
                item(key = "empty_tasks") {
                    Text(
                        text = "No tasks yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingSmall)
                    )
                }
            }
        }

        CollapsingBalanceTopBar(
            isVisible = isCollapsed,
            collapseProgress = collapseProgress,
            formatted = formatted,
            onAddTransaction = { showAddTransaction = true }
        )
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description ->
                taskViewModel.onAddTask(title, description)
                showAddDialog = false
            }
        )
    }

    if (showAddTransaction) {
        AddTransactionBottomSheet(
            viewModel = categoriesViewModel,
            onDismiss = { showAddTransaction = false }
        )
    }
}

@Composable
private fun CollapsingBalanceCard(
    collapseProgress: Float,
    formatted: Pair<String, String>,
    accounts: List<AccountEntity>,
    onActionClick: () -> Unit
) {
    val dimens = LocalAppDimens.current
    val motion = LocalExpressiveMotion.current

    val cardHeight by animateDpAsState(
        targetValue = lerp(
            dimens.cardMinHeight * 3.2f,
            dimens.cardMinHeight * 1.2f,
            collapseProgress
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardHeight"
    )

    val cornerRadius by animateDpAsState(
        targetValue = lerp(
            dimens.shapeCornerExtraLarge,
            dimens.cornerRadiusSmall,
            collapseProgress
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardCornerRadius"
    )

    val contentPadding = lerp(dimens.paddingLarge, dimens.paddingMedium, collapseProgress)

    val titleAlpha = 1f - collapseProgress
    val compressedAlpha = collapseProgress

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingSmall)
            .height(cardHeight),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = lerp(2.dp, 8.dp, collapseProgress)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedVisibility(visible = titleAlpha > 0.1f) {
                Text(
                    text = "TOTAL BALANCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = titleAlpha),
                    letterSpacing = 1.sp
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha }
            ) {
                Text(
                    text = "$${formatted.first}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = ".${formatted.second}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }

            Row(
                modifier = Modifier.graphicsLayer { alpha = compressedAlpha },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${accounts.size} accounts linked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CollapsingBalanceTopBar(
    isVisible: Boolean,
    collapseProgress: Float,
    formatted: Pair<String, String>,
    onAddTransaction: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dimens = LocalAppDimens.current
    val motion = LocalExpressiveMotion.current

    val topBarAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "topBarAlpha"
    )

    val topBarHeight by animateDpAsState(
        targetValue = if (isVisible) 56.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "topBarHeight"
    )

    if (topBarAlpha > 0.01f) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight),
            color = MaterialTheme.colorScheme.surface.copy(alpha = topBarAlpha * 0.95f),
            shadowElevation = if (isVisible) 4.dp else 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.paddingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* profile */ }) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$${formatted.first}.${formatted.second}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = { haptic.mediumConfirm(); onAddTransaction() }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Transaction",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionChipsRow() {
    val dimens = LocalAppDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingSmall)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionChipItem(icon = Icons.AutoMirrored.Filled.ArrowForward, label = "Transfer")
        ActionChipItem(icon = Icons.Filled.Add, label = "Top Up")
        ActionChipItem(icon = Icons.Filled.Refresh, label = "Exchange")
    }
}

@Composable
private fun ActionChipItem(icon: ImageVector, label: String) {
    val haptic = LocalHapticFeedback.current
    val dimens = LocalAppDimens.current
    val motion = LocalExpressiveMotion.current

    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) motion.pressScale else motion.releaseScale,
        animationSpec = motion.scaleAnimationSpec,
        label = "chipScale"
    )

    Surface(
        onClick = { haptic.lightTick() },
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(dimens.cornerRadiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dimens.paddingMedium, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(dimens.iconSizeSmall),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MyCardsSection(accounts: List<AccountEntity>) {
    val dimens = LocalAppDimens.current

    Column(modifier = Modifier.padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Cards",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(onClick = { /* add card */ }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Card",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.cardMinHeight * 2.5f)
                .clip(RoundedCornerShape(dimens.cornerRadiusMedium))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimens.paddingLarge),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "VIRTUAL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "\u2022\u2022\u2022\u2022   4209",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 4.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "CARD HOLDER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "You",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${formatCurrencyParts(totalBalance(accounts)).first}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}

private fun totalBalance(accounts: List<AccountEntity>): Double = accounts.sumOf { it.balance }

@Composable
private fun RecentTransactionsHeader() {
    val dimens = LocalAppDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "View All",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ExpressiveTransactionRow(tx: TransactionEntity) {
    val dimens = LocalAppDimens.current
    val motion = LocalExpressiveMotion.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium, vertical = 4.dp)
            .expressiveLongPressTilt(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimens.iconSizeLarge)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForTransactionCategory(tx.categoryId),
                contentDescription = null,
                modifier = Modifier.size(dimens.iconSizeMedium - 2.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(dimens.paddingSmall + 4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.note.ifEmpty { "Transaction" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = formatTimestamp(tx.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val sign = if (tx.type == "INCOME") "+" else "-"
        Text(
            text = "$sign$${formatAmount(tx.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TaskSectionHeader(
    state: com.makemission.makeflow.ui.state.TaskUiState,
    onAddClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dimens = LocalAppDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Tasks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!state.isLoading) {
                Text(
                    text = "${state.completedCount} of ${state.tasks.size} completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = { haptic.mediumConfirm(); onAddClick() }) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Task",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ExpressiveTaskItem(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dimens = LocalAppDimens.current
    val motion = LocalExpressiveMotion.current

    var isRemoved by remember { mutableStateOf(false) }

    val checkProgress = remember { Animatable(if (task.isCompleted) 1f else 0f) }

    LaunchedEffect(task.isCompleted) {
        checkProgress.animateTo(
            targetValue = if (task.isCompleted) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val textColor by animateColorAsState(
        targetValue = if (task.isCompleted)
            MaterialTheme.colorScheme.onSurfaceVariant
        else
            MaterialTheme.colorScheme.onBackground,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "taskTextColor"
    )

    val fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold
    val textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None

    val rowHeight by animateDpAsState(
        targetValue = if (task.isCompleted) 52.dp else 64.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "taskRowHeight"
    )

    val rowAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.7f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "taskRowAlpha"
    )

    AnimatedVisibility(
        visible = !isRemoved,
        exit = fadeOut(animationSpec = spring(dampingRatio = 0.7f)) +
                shrinkVertically(animationSpec = spring(dampingRatio = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .padding(horizontal = dimens.paddingMedium, vertical = 4.dp)
                .graphicsLayer { alpha = rowAlpha }
                .expressiveLongPressTilt(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(dimens.cornerRadiusSmall))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = checkProgress.value * 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (checkProgress.value > 0.01f) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = checkProgress.value
                                scaleY = checkProgress.value
                                alpha = checkProgress.value
                            },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(dimens.paddingSmall))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = fontWeight,
                    color = textColor,
                    textDecoration = textDecoration
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = rowAlpha),
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = {
                haptic.strongPress()
                isRemoved = true
                onDelete()
            }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private val Color.Companion.onSurfaceVariant: Color
    get() = Color(0xFF49454F)

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { haptic.strongPress(); onConfirm(title, description) },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun iconForTransactionCategory(categoryName: String?): ImageVector {
    return when (categoryName) {
        "Electronics" -> Icons.Filled.ShoppingCart
        "Food & Dining" -> Icons.Filled.ShoppingCart
        "Transportation" -> Icons.Filled.LocationOn
        "Entertainment" -> Icons.Filled.Star
        "Housing" -> Icons.Filled.Home
        "Income" -> Icons.Filled.Check
        else -> Icons.Filled.Check
    }
}

private fun formatCurrencyParts(amount: Double): Pair<String, String> {
    val parts = String.format(Locale.US, "%.2f", amount).split(".")
    return Pair(parts[0].replace(",", ","), parts[1])
}

private fun formatAmount(amount: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    fmt.minimumFractionDigits = 2
    fmt.maximumFractionDigits = 2
    return fmt.format(amount)
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val dateFormat = SimpleDateFormat("MMM dd", Locale.US)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
    val todayFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
    val isToday = todayFormat.format(date) == todayFormat.format(now)
    val prefix = if (isToday) "Today" else "Yesterday"
    return "$prefix, ${timeFormat.format(date)} \u2022 ${dateFormat.format(date)}"
}
