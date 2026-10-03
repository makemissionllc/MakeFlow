package com.makemission.makeflow.core.notifications

import com.makemission.makeflow.core.device.InterruptionFilterProvider
import com.makemission.makeflow.core.device.InterruptionState
import com.makemission.makeflow.data.local.dao.QueuedNotificationDao
import com.makemission.makeflow.data.local.entity.QueuedNotificationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationGateMutexTest {

    private class ToggleInterruptionFilter : InterruptionFilterProvider(null) {
        var simulatedState: InterruptionState = InterruptionState.TOTAL_SILENCE

        override fun getCurrentState(): InterruptionState = simulatedState

        override fun observeState(): Flow<InterruptionState> = throw UnsupportedOperationException()
    }

    private class CaptureSender : NotificationSender {
        val sent = mutableListOf<Pair<String, String>>()

        override suspend fun send(title: String, body: String) {
            sent.add(title to body)
        }
    }

    @Test
    fun concurrent_ops_preserve_total_item_count() {
        runBlocking {
            val filter = ToggleInterruptionFilter()
            val sender = CaptureSender()
            val dao = FakeQueuedNotificationDao()
            val gate = NotificationGate(
                interruptionFilterProvider = filter,
                queuedNotificationDao = dao,
                notificationSender = sender
            )

            filter.simulatedState = InterruptionState.TOTAL_SILENCE
            gate.dispatchOrQueue("test", "A", "bodyA")
            gate.dispatchOrQueue("test", "B", "bodyB")
            assertEquals(2, dao.count())

            filter.simulatedState = InterruptionState.NORMAL
            gate.flushQueueIfClear()

            assertEquals(1, sender.sent.size)
            assertTrue(sender.sent[0].first.startsWith("You have"))
            assertEquals(0, dao.count())
        }
    }

    @Test
    fun mutex_serializes_dispatch_and_flush() {
        runBlocking {
            val filter = ToggleInterruptionFilter()
            val sender = CaptureSender()
            val dao = FakeQueuedNotificationDao()
            val gate = NotificationGate(
                interruptionFilterProvider = filter,
                queuedNotificationDao = dao,
                notificationSender = sender
            )

            filter.simulatedState = InterruptionState.TOTAL_SILENCE
            gate.dispatchOrQueue("test", "A", "bodyA")
            assertEquals(1, dao.count())

            filter.simulatedState = InterruptionState.NORMAL
            gate.flushQueueIfClear()

            assertEquals(1, sender.sent.size)
            assertEquals(0, dao.count())
        }
    }

    private class FakeQueuedNotificationDao : QueuedNotificationDao {
        val items = mutableListOf<QueuedNotificationEntity>()
        var nextId = 1L

        override suspend fun insert(entity: QueuedNotificationEntity) {
            items.add(entity.copy(id = nextId++))
        }

        override suspend fun getAll(): List<QueuedNotificationEntity> = items.sortedBy { it.createdAt }

        override suspend fun deleteById(id: Long) { items.removeAll { it.id == id } }

        override suspend fun deleteAll() { items.clear() }

        override suspend fun count(): Int = items.size

        override suspend fun trimOldest(excessCount: Int) {
            val toRemove = items.sortedBy { it.createdAt }.take(excessCount)
            items.removeAll(toRemove)
        }
    }
}
