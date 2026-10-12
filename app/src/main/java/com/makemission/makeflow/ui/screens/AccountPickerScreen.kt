package com.makemission.makeflow.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.makemission.makeflow.core.format.CurrencyFormatter
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.ui.components.pressScale
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.ui.theme.LocalExpressiveMotion
import com.makemission.makeflow.ui.theme.OverlayTransition

@Composable
fun AccountPickerScreen(
    accounts: List<AccountEntity>,
    selectedAccountId: String?,
    currencyCode: String,
    onAccountSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val dimens = LocalAppDimens.current
    val reducedMotion = LocalReducedMotion.current
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceDim)
    ) {
        Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spacingXLarge, vertical = dimens.spacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Select Account",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = dimens.paddingLarge,
                vertical = dimens.spacingMedium
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingMedium)
        ) {
            items(accounts, key = { it.id }) { account ->
                val isSelected = account.id == selectedAccountId
                val cardShape = RoundedCornerShape(dimens.shapeCornerMedium)
                val accountCurrency = account.currencyCode?.takeIf(String::isNotBlank) ?: currencyCode
                val selectionAnimation = if (reducedMotion) OverlayTransition.reducedColor
                    else LocalExpressiveMotion.current.colorAnimationSpec
                val cardColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                    animationSpec = selectionAnimation,
                    label = "accountSelectionSurface"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = selectionAnimation,
                    label = "accountSelectionBorder"
                )
                val avatarColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    animationSpec = selectionAnimation,
                    label = "accountSelectionAvatar"
                )
                val avatarContentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = selectionAnimation,
                    label = "accountSelectionAvatarContent"
                )
                val accountNameColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                    animationSpec = selectionAnimation,
                    label = "accountSelectionName"
                )
                val accountDetailsColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = selectionAnimation,
                    label = "accountSelectionDetails"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(cardColor)
                        .border(dimens.spacingXSmall, borderColor, cardShape)
                        .pressScale()
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onAccountSelected(account.id) }
                        )
                        .testTag("account_picker_${account.id}")
                        .padding(dimens.paddingLarge),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(dimens.avatarSize)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = account.name.take(1).uppercase(),
                            color = avatarContentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = dimens.textXl
                        )
                    }
                    Spacer(modifier = Modifier.width(dimens.spacingXLarge))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            color = accountNameColor,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = CurrencyFormatter.format(account.balance, accountCurrency),
                            color = accountDetailsColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
