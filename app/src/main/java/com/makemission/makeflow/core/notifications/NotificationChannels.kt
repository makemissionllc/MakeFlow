package com.makemission.makeflow.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_BUDGET_ALERTS = "budget_alerts"
    const val CHANNEL_BILL_REMINDERS = "bill_reminders"
    const val CHANNEL_MPESA_IMPORTS = "mpesa_imports"
    const val CHANNEL_FINANCE_REVIEW = "finance_review_reminders"
    const val CHANNEL_APP_UPDATES = "app_updates"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val budgetChannel = NotificationChannel(
            CHANNEL_BUDGET_ALERTS,
            "Budget Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when you exceed a budget"
        }

        val billChannel = NotificationChannel(
            CHANNEL_BILL_REMINDERS,
            "Bill Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for upcoming recurring bills"
        }

        val mpesaChannel = NotificationChannel(
            CHANNEL_MPESA_IMPORTS,
            "M-PESA Import Reviews",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Review new M-PESA transaction detections before saving"
        }

        val financeReviewChannel = NotificationChannel(
            CHANNEL_FINANCE_REVIEW,
            "Finance Review Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Optional reminders to review your finances"
        }

        val appUpdatesChannel = NotificationChannel(
            CHANNEL_APP_UPDATES,
            "App updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Downloaded MakeFlow Community updates ready to install"
        }

        nm.createNotificationChannels(listOf(budgetChannel, billChannel, mpesaChannel, financeReviewChannel, appUpdatesChannel))
    }
}
