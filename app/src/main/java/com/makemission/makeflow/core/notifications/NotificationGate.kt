package com.makemission.makeflow.core.notifications

import com.makemission.makeflow.core.device.InterruptionFilterProvider
import com.makemission.makeflow.core.device.InterruptionState
import com.makemission.makeflow.data.local.dao.QueuedNotificationDao
import com.makemission.makeflow.data.local.entity.QueuedNotificationEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MAX_QUEUED = 20

interface NotificationSender {
    suspend fun send(title: String, body: String)
}

class NotificationGate(
    private val interruptionFilterProvider: InterruptionFilterProvider,
    private val queuedNotificationDao: QueuedNotificationDao,
    private val notificationSender: NotificationSender
) {
    private val mutex = Mutex()
    suspend fun dispatchOrQueue(
        type: String,
        title: String,
        body: String,
        payload: String? = null
    ) = mutex.withLock {
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

    suspend fun flushQueueIfClear() = mutex.withLock {
        val state = try {
            interruptionFilterProvider.getCurrentState()
        } catch (_: Exception) {
            InterruptionState.NORMAL
        }
        if (state != InterruptionState.NORMAL) return@withLock
        val queued = queuedNotificationDao.getAll()
        if (queued.isEmpty()) return@withLock
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
