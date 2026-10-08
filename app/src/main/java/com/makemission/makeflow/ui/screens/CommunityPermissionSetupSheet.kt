package com.makemission.makeflow.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.makemission.makeflow.BuildConfig
import com.makemission.makeflow.core.layout.AdaptiveSheetDialog
import com.makemission.makeflow.core.layout.AdaptiveSheetHeader
import com.makemission.makeflow.core.layout.AdaptiveSheetPrimaryButton
import com.makemission.makeflow.core.notifications.MpesaAutoImportNotification
import com.makemission.makeflow.core.notifications.openAppNotificationSettings
import com.makemission.makeflow.ui.theme.LocalAppDimens

@Composable
fun CommunityPermissionSetupSheet(
    onComplete: () -> Unit,
    onEnableMpesaAutoImport: () -> Unit
) {
    val context = LocalContext.current
    val dimens = LocalAppDimens.current
    var smsPermissionGranted by remember(context) {
        mutableStateOf(hasPermission(context, Manifest.permission.RECEIVE_SMS))
    }
    var notificationPermissionGranted by remember(context) {
        mutableStateOf(isNotificationPermissionGranted(context))
    }
    var notificationsEnabled by remember(context) {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted || isNotificationPermissionGranted(context)
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        completePermissionSetup(
            context = context,
            smsPermissionGranted = smsPermissionGranted,
            notificationPermissionGranted = notificationPermissionGranted,
            notificationsEnabled = notificationsEnabled,
            onEnableMpesaAutoImport = onEnableMpesaAutoImport,
            onComplete = onComplete
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        smsPermissionGranted = granted || hasPermission(context, Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !isNotificationPermissionGranted(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationPermissionGranted = isNotificationPermissionGranted(context)
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            completePermissionSetup(
                context = context,
                smsPermissionGranted = smsPermissionGranted,
                notificationPermissionGranted = notificationPermissionGranted,
                notificationsEnabled = notificationsEnabled,
                onEnableMpesaAutoImport = onEnableMpesaAutoImport,
                onComplete = onComplete
            )
        }
    }

    val notificationAccessReady = notificationPermissionGranted && notificationsEnabled
    AdaptiveSheetDialog(onDismiss = onComplete) {
        AdaptiveSheetHeader(
            title = "Set up optional access",
            subtitle = "Choose what you want MakeFlow to do",
            onDismiss = onComplete
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingLarge)) {
            PermissionAccessRow(
                icon = Icons.Outlined.Receipt,
                title = "M-PESA message detection",
                description = "Checks new incoming messages for M-PESA confirmations only. Nothing is saved until you review it.",
                enabled = smsPermissionGranted
            )
            PermissionAccessRow(
                icon = Icons.Outlined.Campaign,
                title = "Notifications",
                description = "Shows detected transactions for review and makes weekly reminders available. Reminders stay off until you enable them in Settings.",
                enabled = notificationAccessReady
            )
            Text(
                text = "Android asks separately for each permission. You can decline either one. Photos and backups use Android pickers; biometric unlock and widget pinning use system confirmation when you turn them on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AdaptiveSheetPrimaryButton(
            label = "Enable access",
            onClick = {
                when {
                    BuildConfig.MPESA_SMS_AUTO_IMPORT_ENABLED && !smsPermissionGranted ->
                        smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !notificationPermissionGranted ->
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else -> completePermissionSetup(
                        context = context,
                        smsPermissionGranted = smsPermissionGranted,
                        notificationPermissionGranted = notificationPermissionGranted,
                        notificationsEnabled = notificationsEnabled,
                        onEnableMpesaAutoImport = onEnableMpesaAutoImport,
                        onComplete = onComplete
                    )
                }
            }
        )
        androidx.compose.material3.TextButton(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Maybe later")
        }
    }
}

@Composable
private fun PermissionAccessRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    enabled: Boolean
) {
    val dimens = LocalAppDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimens.spacingXSmall),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(dimens.spacingLarge)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (enabled) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Allowed",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun completePermissionSetup(
    context: Context,
    smsPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    notificationsEnabled: Boolean,
    onEnableMpesaAutoImport: () -> Unit,
    onComplete: () -> Unit
) {
    val mpesaNotificationsAvailable = MpesaAutoImportNotification.hasNotificationAccess(context)
    val canReviewMpesa = BuildConfig.MPESA_SMS_AUTO_IMPORT_ENABLED &&
        smsPermissionGranted && notificationPermissionGranted && notificationsEnabled
    if (canReviewMpesa && mpesaNotificationsAvailable) {
        onEnableMpesaAutoImport()
    } else if (BuildConfig.MPESA_SMS_AUTO_IMPORT_ENABLED &&
        smsPermissionGranted && notificationPermissionGranted && notificationsEnabled
    ) {
        Toast.makeText(
            context,
            "Allow M-PESA review notifications in Android settings to enable detection.",
            Toast.LENGTH_LONG
        ).show()
        openAppNotificationSettings(context)
    } else if (BuildConfig.MPESA_SMS_AUTO_IMPORT_ENABLED &&
        smsPermissionGranted && notificationPermissionGranted && !notificationsEnabled
    ) {
        Toast.makeText(
            context,
            "M-PESA detection stays off until MakeFlow notifications are allowed.",
            Toast.LENGTH_LONG
        ).show()
        openAppNotificationSettings(context)
    } else if (BuildConfig.MPESA_SMS_AUTO_IMPORT_ENABLED &&
        smsPermissionGranted && !notificationsEnabled
    ) {
        Toast.makeText(
            context,
            "M-PESA detection stays off until MakeFlow notifications are allowed.",
            Toast.LENGTH_LONG
        ).show()
    }
    onComplete()
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun isNotificationPermissionGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
