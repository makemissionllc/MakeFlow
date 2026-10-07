package com.makemission.makeflow.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.makemission.makeflow.MainActivity
import com.makemission.makeflow.core.logging.AppLogger
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.userPrefDataStore
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object FinanceReviewReminderScheduler {
    private const val UNIQUE_WORK_NAME = "weekly_finance_review_reminder"

    fun setEnabled(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<FinanceReviewReminderWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(7, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

class FinanceReviewReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val preferences = UserPreferencesDataStore(applicationContext.userPrefDataStore)
            if (!preferences.financeReviewRemindersEnabled.first()) return Result.success()
            if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return Result.success()
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return Result.success()

            NotificationChannels.createChannels(applicationContext)
            val name = preferences.userName.first()
            val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val notificationId = 2_030_107
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = androidx.core.app.NotificationCompat.Builder(
                applicationContext,
                NotificationChannels.CHANNEL_FINANCE_REVIEW
            )
                .setSmallIcon(com.makemission.makeflow.R.drawable.ic_notification)
                .setContentTitle("Review your finances")
                .setContentText(FinanceReviewReminderContent.body(name))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
            Result.success()
        } catch (error: Exception) {
            AppLogger.error("FinanceReviewReminder", "Could not send the weekly finance reminder", error)
            Result.retry()
        }
    }
}

object FinanceReviewReminderContent {
    fun body(name: String): String {
        val firstName = name.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        return if (firstName.isNotEmpty()) {
            "$firstName, take a moment to check in on your spending and progress."
        } else {
            "Take a moment to check in on your spending and progress."
        }
    }
}
