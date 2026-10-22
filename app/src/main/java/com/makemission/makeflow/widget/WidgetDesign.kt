package com.makemission.makeflow.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.makemission.makeflow.R

internal object WidgetTokens {
    val compactWidth = 180.dp
    val expandedWidth = 220.dp
    val expandedHeight = 180.dp
    val compactPadding = 4.dp
    val standardPadding = 12.dp
    val sectionSpacing = 8.dp
    val compactSpacing = 4.dp
    val compactActionHeight = 48.dp
    val actionHeight = 48.dp
    val labelSize = 12.sp
    val smallLabelSize = 11.sp
    val bodySize = 14.sp
    val titleSize = 17.sp
    val amountSize = 25.sp
    val compactAmountSize = 20.sp
    val actionTextSize = 14.sp
}

internal object WidgetPalette {
    val text: ColorProvider
        @Composable get() = GlanceTheme.colors.onSurface
    val muted: ColorProvider
        @Composable get() = GlanceTheme.colors.onSurfaceVariant
    val accent = ColorProvider(R.color.widget_accent)
    val expense: ColorProvider
        @Composable get() = GlanceTheme.colors.error
    val actionText = ColorProvider(R.color.widget_action_content)
}

internal fun GlanceModifier.widgetSurface(): GlanceModifier =
    background(ImageProvider(R.drawable.widget_surface))

internal fun widgetPadding(compact: Boolean): Dp =
    if (compact) WidgetTokens.compactPadding else WidgetTokens.standardPadding

@androidx.compose.runtime.Composable
internal fun WidgetActionPill(
    label: String,
    action: Action,
    primary: Boolean,
    modifier: GlanceModifier = GlanceModifier,
    height: Dp = WidgetTokens.actionHeight
) {
    Box(
        modifier = modifier
            .height(height)
            .background(ImageProvider(if (primary) R.drawable.widget_action_primary else R.drawable.widget_action_secondary))
            .clickable(action)
            .padding(horizontal = WidgetTokens.sectionSpacing),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = WidgetPalette.actionText,
                fontSize = WidgetTokens.actionTextSize,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}
