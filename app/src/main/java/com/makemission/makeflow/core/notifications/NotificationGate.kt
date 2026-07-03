package com.makemission.makeflow.core.notifications

import com.makemission.makeflow.core.device.InterruptionFilterProvider
import com.makemission.makeflow.core.device.InterruptionState
import com.makemission.makeflow.data.local.dao.QueuedNotificationDao
import com.makemission.makeflow.data.local.entity.QueuedNotificationEntity

private const val MAX_QUEUED = 20

interface NotificationSender {
    suspend fun send(title: String, body: String)
}

class NotificationGate(
    private val interruptionFilterProvider: InterruptionFilterProvider,
    private val queuedNotificationDao: QueuedNotificationDao,
    private val notificationSender: NotificationSender
) {
    suspend fun dispatchOrQueue(
        type: String,
        title: String,
        body: String,
        payload: String? = null
    ) {
        val state = try {
            interruptionFilterProvider.getCurrentState()
        } catch (_: Exception) {
            InterruptionState.NORMAL
        }
        if (state == InterruptionState.NORMAL) {
            notificationSender.send(title, body)
        } else {
            queuedNotificationDao.insert(
                QueuedNotificationEntity(
                    type = type, title = title, body = body,
                    createdAt = System.currentTimeMillis(), payload = payload
                )
            )
            val count = queuedNotificationDao.count()
            if (count > MAX_QUEUED) {
                queuedNotificationDao.trimOldest(count - MAX_QUEUED)
            }
        }
    }

    suspend fun flushQueueIfClear() {
        val state = try {
            interruptionFilterProvider.getCurrentState()
        } catch (_: Exception) {
            InterruptionState.NORMAL
        }
        if (state != InterruptionState.NORMAL) return
        val queued = queuedNotificationDao.getAll()
        if (queued.isEmpty()) return
        if (queued.size > 1) {
            notificationSender.send(
                title = "You have ${queued.size} updates",
                body = queued.joinToString(limit = 3) { it.title }
            )
        } else {
            notificationSender.send(queued.first().title, queued.first().body)
        }
        queuedNotificationDao.deleteAll()
    }
}
