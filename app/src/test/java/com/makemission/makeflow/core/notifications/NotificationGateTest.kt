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

private class FakeInterruptionFilterProvider(private val state: InterruptionState) :
    InterruptionFilterProvider(null) {

    override fun getCurrentState(): InterruptionState = state

    override fun observeState(): Flow<InterruptionState> = throw UnsupportedOperationException("not needed")
}

private class FakeNotificationSender : NotificationSender {
    val sent = mutableListOf<Pair<String, String>>()

    override suspend fun send(title: String, body: String) {
        sent.add(title to body)
    }
}

private class FakeQueuedNotificationDao : QueuedNotificationDao {
    private val items = mutableListOf<QueuedNotificationEntity>()
    private var nextId = 1L

    override suspend fun insert(entity: QueuedNotificationEntity) {
        items.add(entity.copy(id = nextId++))
    }

    override suspend fun getAll(): List<QueuedNotificationEntity> = items.sortedBy { it.createdAt }

    override suspend fun deleteById(id: Long) {
        items.removeAll { it.id == id }
    }

    override suspend fun deleteAll() {
        items.clear()
    }

    override suspend fun count(): Int = items.size

    override suspend fun trimOldest(excessCount: Int) {
        val toRemove = items.sortedBy { it.createdAt }.take(excessCount)
        items.removeAll(toRemove)
    }
}

class NotificationGateTest {

    @Test
    fun `normal state dispatches immediately`() = runBlocking {
        val sender = FakeNotificationSender()
        val gate = NotificationGate(
            interruptionFilterProvider = FakeInterruptionFilterProvider(InterruptionState.NORMAL),
            queuedNotificationDao = FakeQueuedNotificationDao(),
            notificationSender = sender
        )

        gate.dispatchOrQueue(type = "BUDGET_EXCEEDED", title = "Over budget!", body = "You exceeded your budget")

        assertEquals(1, sender.sent.size)
        assertEquals("Over budget!", sender.sent[0].first)
        assertEquals("You exceeded your budget", sender.sent[0].second)
    }

    @Test
    fun `DND state queues instead of sending`() = runBlocking {
        val sender = FakeNotificationSender()
        val dao = FakeQueuedNotificationDao()
        val gate = NotificationGate(
            interruptionFilterProvider = FakeInterruptionFilterProvider(InterruptionState.TOTAL_SILENCE),
            queuedNotificationDao = dao,
            notificationSender = sender
        )

        gate.dispatchOrQueue(type = "BUDGET_EXCEEDED", title = "Over budget!", body = "You exceeded your budget")

        assertEquals(0, sender.sent.size)
        assertEquals(1, dao.count())
    }

    @Test
    fun `queue flush bundles multiple items into a summary`() = runBlocking {
        val sender = FakeNotificationSender()
        val dao = FakeQueuedNotificationDao()
        dao.insert(QueuedNotificationEntity(type = "a", title = "Alert 1", body = "body1", createdAt = 1))
        dao.insert(QueuedNotificationEntity(type = "b", title = "Alert 2", body = "body2", createdAt = 2))
        val gate = NotificationGate(
            interruptionFilterProvider = FakeInterruptionFilterProvider(InterruptionState.NORMAL),
            queuedNotificationDao = dao,
            notificationSender = sender
        )

        gate.flushQueueIfClear()

        assertEquals(1, sender.sent.size)
        assertTrue(sender.sent[0].first.startsWith("You have"))
        assertEquals(0, dao.count())
    }

    @Test
    fun `queue flush sends single item directly`() = runBlocking {
        val sender = FakeNotificationSender()
        val dao = FakeQueuedNotificationDao()
        dao.insert(QueuedNotificationEntity(type = "a", title = "Alert 1", body = "body1", createdAt = 1))
        val gate = NotificationGate(
            interruptionFilterProvider = FakeInterruptionFilterProvider(InterruptionState.NORMAL),
            queuedNotificationDao = dao,
            notificationSender = sender
        )

        gate.flushQueueIfClear()

        assertEquals(1, sender.sent.size)
        assertEquals("Alert 1", sender.sent[0].first)
        assertEquals("body1", sender.sent[0].second)
        assertEquals(0, dao.count())
    }

    @Test
    fun `queue cap drops oldest when exceeded`() = runBlocking {
        val sender = FakeNotificationSender()
        val dao = FakeQueuedNotificationDao()
        val gate = NotificationGate(
            interruptionFilterProvider = FakeInterruptionFilterProvider(InterruptionState.TOTAL_SILENCE),
            queuedNotificationDao = dao,
            notificationSender = sender
        )

        for (i in 1..22) {
            gate.dispatchOrQueue(type = "test", title = "Alert $i", body = "body$i")
        }

        assertEquals(0, sender.sent.size)
        assertEquals(20, dao.count())
        val all = dao.getAll()
        assertEquals("Alert 3", all.first().title)
        assertEquals("Alert 22", all.last().title)
    }
}
