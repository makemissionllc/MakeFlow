package com.makemission.makeflow.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_BUDGET_ALERTS = "budget_alerts"
    const val CHANNEL_BILL_REMINDERS = "bill_reminders"

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

        nm.createNotificationChannels(listOf(budgetChannel, billChannel))
    }
}
