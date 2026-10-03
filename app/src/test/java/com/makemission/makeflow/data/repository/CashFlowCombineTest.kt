package com.makemission.makeflow.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CashFlowCombineTest {

    @Test
    fun `combine emits when sources change and conflates same value`() = runBlocking {
        val flowA = MutableStateFlow(100.0)
        val flowB = MutableStateFlow(200.0)

        val combined = combine(flowA, flowB) { a, _ -> a }

        assertEquals(100.0, combined.first(), 0.001)

        flowA.value = 100.0
        assertEquals(100.0, combined.first(), 0.001)

        flowA.value = 150.0
        assertEquals(150.0, combined.first(), 0.001)
    }

    @Test
    fun `combine returns null when guard condition fails`() = runBlocking {
        val balance = MutableStateFlow(1000.0)
        val txs = MutableStateFlow(emptyList<String>())
        val patterns = MutableStateFlow(emptyList<String>())

        val projection = combine(balance, txs, patterns) { bal, _, _ ->
            if (bal < 500) null else "projected_$bal"
        }

        assertEquals("projected_1000.0", projection.first())

        balance.value = 400.0
        assertNull(projection.first())

        balance.value = 600.0
        assertEquals("projected_600.0", projection.first())
    }

    @Test
    fun `combine with three sources emits on each distinct change`() = runBlocking {
        val flowA = MutableStateFlow(100.0)
        val flowB = MutableStateFlow(listOf("tx1", "tx2"))
        val flowC = MutableStateFlow(3)

        val combined = combine(flowA, flowB, flowC) { a, b, c ->
            "a=$a,b=${b.size},c=$c"
        }

        assertEquals("a=100.0,b=2,c=3", combined.first())

        flowA.value = 200.0
        assertEquals("a=200.0,b=2,c=3", combined.first())

        flowB.value = listOf("tx1", "tx2", "tx3")
        assertEquals("a=200.0,b=3,c=3", combined.first())

        flowC.value = 5
        assertEquals("a=200.0,b=3,c=5", combined.first())
    }
}
