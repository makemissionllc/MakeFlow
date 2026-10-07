package com.makemission.makeflow.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.core.haptics.HapticEvent
import com.makemission.makeflow.core.haptics.HapticManager
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.core.layout.AdaptiveSheetHeader
import com.makemission.makeflow.domain.MonthlyFinanceReview
import com.makemission.makeflow.ui.components.AnimatedProgressBar
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.LocalMakeFlowColors
import com.makemission.makeflow.ui.theme.OverlayTransition
import java.time.format.TextStyle
import java.util.Locale

private const val REVIEW_PAGE_COUNT = 4

@Composable
fun MonthlyFinanceReviewSheet(
    review: MonthlyFinanceReview,
    currencyCode: String,
    onDismiss: () -> Unit,
    hapticManager: HapticManager? = null
) {
    val dimens = LocalAppDimens.current
    val reducedMotion = LocalReducedMotion.current
    var page by remember(review.month) { mutableIntStateOf(0) }
    val monthName = review.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

    AdaptiveSheetDialog(onDismiss = onDismiss, scrollable = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .padding(vertical = dimens.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
        ) {
            AdaptiveSheetHeader(
                title = "$monthName ${review.month.year}",
                eyebrow = "MONTHLY REVIEW",
                onDismiss = onDismiss
            )

            AnimatedProgressBar(
                progress = (page + 1).toFloat() / REVIEW_PAGE_COUNT,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedContent(
                targetState = page,
                modifier = Modifier.fillMaxWidth().weight(1f),
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(OverlayTransition.reducedEnter) togetherWith fadeOut(OverlayTransition.reducedExit)
                    } else {
                        (fadeIn(OverlayTransition.enterSpring) + slideInVertically(
                            animationSpec = OverlayTransition.enterSlide,
                            initialOffsetY = { (it * OverlayTransition.ValueShiftFraction).toInt() }
                        )) togetherWith (fadeOut(OverlayTransition.exitSpring) + slideOutVertically(
                            animationSpec = OverlayTransition.exitSlide,
                            targetOffsetY = { -(it * OverlayTransition.ValueShiftFraction).toInt() }
                        ))
                    }
                },
                label = "monthly-review-story"
            ) { targetPage ->
                ReviewStoryPage(
                    page = targetPage,
                    review = review,
                    monthName = monthName,
                    currencyCode = currencyCode
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (page > 0) {
                    TextButton(
                        onClick = {
                            page--
                            hapticManager?.fire(HapticEvent.NAV_SELECT)
                        }
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
                        Text("Back")
                    }
                } else {
                    Text("${page + 1} of $REVIEW_PAGE_COUNT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        hapticManager?.fire(HapticEvent.NAV_SELECT)
                        if (page == REVIEW_PAGE_COUNT - 1) onDismiss() else page++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (page == REVIEW_PAGE_COUNT - 1) "Done" else "Next")
                    if (page < REVIEW_PAGE_COUNT - 1) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStoryPage(
    page: Int,
    review: MonthlyFinanceReview,
    monthName: String,
    currencyCode: String
) {
    val dimens = LocalAppDimens.current
    val colors = LocalMakeFlowColors.current
    val headlineAmount: Double = when (page) {
        0 -> review.net
        1 -> review.topCategoryAmount
        2 -> review.expenses
        else -> 0.0
    }

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        when (page) {
            0 -> {
                StoryEyebrow("THE BIG PICTURE")
                Text("Your month, in one number", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(dimens.spacingXXLarge))
                Text(
                    text = CurrencyFormatter.format(headlineAmount, currencyCode),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (review.net >= 0.0) colors.colorSuccess else MaterialTheme.colorScheme.error
                )
                Text("net for $monthName", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(dimens.spacingXXLarge))
                StoryValueRow("Income", review.income, currencyCode, colors.colorSuccess)
                StoryValueRow("Spent", review.expenses, currencyCode, MaterialTheme.colorScheme.onSurface)
            }
            1 -> {
                StoryEyebrow("WHERE IT WENT")
                Text("Your biggest spending area", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(dimens.spacingXXLarge))
                Text(
                    text = review.topCategoryName ?: "No spending yet",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (review.topCategoryName != null) CurrencyFormatter.format(review.topCategoryAmount, currencyCode) else "Add transactions to uncover a pattern",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (review.topCategoryName != null) {
                    Text(
                        text = "${review.topCategorySharePercent.toInt()}% of recorded spending",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.size(dimens.spacingXXLarge))
                Text("Largest purchase", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = review.largestExpenseDescription ?: "No expenses recorded",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (review.largestExpenseDescription != null) {
                    Text(CurrencyFormatter.format(review.largestExpenseAmount, currencyCode), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            2 -> {
                StoryEyebrow("A MONTH OVER MONTH")
                Text("A little perspective", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(dimens.spacingXXLarge))
                val change = review.expenseChangePercent
                if (change == null) {
                    Text("You're building a baseline", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Text("There isn't enough previous-month spending to compare yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val improved = change <= 0.0
                    Text(
                        text = if (improved) "${kotlin.math.abs(change).toInt()}% less spent" else "${change.toInt()}% more spent",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (improved) colors.colorSuccess else MaterialTheme.colorScheme.error
                    )
                    Text("compared with last month", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.size(dimens.spacingXXLarge))
                StoryValueRow("This month", headlineAmount, currencyCode, MaterialTheme.colorScheme.onSurface)
                StoryValueRow("Last month", review.previousExpenses, currencyCode, MaterialTheme.colorScheme.onSurfaceVariant)
                val savings = review.savingsRatePercent
                if (savings != null) {
                    Spacer(Modifier.size(dimens.spacingLarge))
                    Text("Savings rate · ${savings.toInt()}%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.colorSuccess)
                }
            }
            else -> {
                StoryEyebrow("YOUR MONEY HABITS")
                Text("Every entry adds clarity", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(dimens.spacingXXLarge))
                Text(
                    text = review.transactionCount.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text("income and expense entries this month", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(dimens.spacingXXLarge))
                if (review.unconvertedTransactionCount > 0) {
                    Text(
                        text = "${review.unconvertedTransactionCount} ${if (review.unconvertedTransactionCount == 1) "entry was" else "entries were"} left out because a conversion rate is unavailable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text("All entries are included in this summary.", style = MaterialTheme.typography.bodyMedium, color = colors.colorSuccess)
                }
                Spacer(Modifier.size(dimens.spacingLarge))
                Text(
                    text = if (review.rateDate != null) "Converted using daily reference rates dated ${review.rateDate}." else "Same-currency entries are exact; no cross-currency rates were available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Older months use the latest saved rates, not historical rates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StoryEyebrow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.size(LocalAppDimens.current.spacingMedium))
}

@Composable
private fun StoryValueRow(label: String, amount: Double, currencyCode: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = LocalAppDimens.current.spacingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(CurrencyFormatter.format(amount, currencyCode), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = color)
    }
}
