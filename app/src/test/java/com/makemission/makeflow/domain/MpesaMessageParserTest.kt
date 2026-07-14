package com.makemission.makeflow.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MpesaMessageParserTest {

    // ── RECEIVED ──

    @Test
    fun received_basicParsing() {
        val msg = "QAB1XYZ2 Confirmed. You have received Ksh5,000.00 from John Kamau 0712345678 on 1/3/2025 at 10:30 AM. New M-PESA balance is Ksh12,450.00."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertEquals("QAB1XYZ2", result!!.code)
        assertEquals(MpesaMessageParser.TransactionType.RECEIVED, result.type)
        assertEquals(5000.0, result.amount, 0.01)
        assertEquals("John Kamau", result.counterpartyName)
        assertEquals("0712345678", result.counterpartyPhone)
        assertEquals("1/3/2025", result.date)
        assertEquals("10:30 AM", result.time)
        assertEquals(12450.0, result.balance!!, 0.01)
        assertNull(result.transactionCost)
    }

    @Test
    fun received_integerAmount() {
        val msg = "SJK4P8TQ Confirmed. You have received Ksh1200 from Alice Wanjiku 0723456789 on 15/6/2025 at 2:15PM. New M-PESA balance is Ksh3400."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertEquals("SJK4P8TQ", result!!.code)
        assertEquals(1200.0, result.amount, 0.01)
        assertEquals("Alice Wanjiku", result.counterpartyName)
        assertEquals("0723456789", result.counterpartyPhone)
    }

    @Test
    fun received_codeExtraction() {
        val msg = "ABC12DEF3 Confirmed. You have received Ksh500 from Test User 0700000000 on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh1000."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertEquals("ABC12DEF3", result!!.code)
    }

    // ── SENT ──

    @Test
    fun sent_basicParsing() {
        val msg = "QBC3XYZ4 Confirmed. Ksh2,500.00 sent to Mary Njeri 0734567890 on 1/3/2025 at 11:45 AM. New M-PESA balance is Ksh9,950.00. Transaction cost, Ksh35.00."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertEquals("QBC3XYZ4", result!!.code)
        assertEquals(MpesaMessageParser.TransactionType.SENT, result.type)
        assertEquals(2500.0, result.amount, 0.01)
        assertEquals("Mary Njeri", result.counterpartyName)
        assertEquals("0734567890", result.counterpartyPhone)
        assertEquals("1/3/2025", result.date)
        assertEquals("11:45 AM", result.time)
        assertEquals(9950.0, result.balance!!, 0.01)
        assertEquals(35.0, result.transactionCost!!, 0.01)
    }

    @Test
    fun sent_codeExtraction() {
        val msg = "QBC3XYZ4 Confirmed. Ksh100 sent to Test 0700000000 on 1/1/2025 at 1:00 PM. New M-PESA balance is Ksh500. Transaction cost, Ksh10."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertEquals("QBC3XYZ4", result!!.code)
    }

    // ── PAYBILL ──

    @Test
    fun paybill_basicParsing() {
        val msg = "QCD5XYZ6 Confirmed. Ksh3,200.00 paid to KPLC Kenya. on 1/3/2025 at 3:00 PM. New M-PESA balance is Ksh6,750.00. Transaction cost, Ksh40.00."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertEquals("QCD5XYZ6", result!!.code)
        assertEquals(MpesaMessageParser.TransactionType.PAYBILL, result.type)
        assertEquals(3200.0, result.amount, 0.01)
        assertEquals("KPLC Kenya", result.counterpartyName)
        assertNull(result.counterpartyPhone)
        assertEquals("1/3/2025", result.date)
        assertEquals("3:00 PM", result.time)
        assertEquals(6750.0, result.balance!!, 0.01)
        assertEquals(40.0, result.transactionCost!!, 0.01)
    }

    @Test
    fun paybill_codeExtraction() {
        val msg = "QCD5XYZ6 Confirmed. Ksh500 paid to SAFARICOM. on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh1000. Transaction cost, Ksh10."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertEquals("QCD5XYZ6", result!!.code)
    }

    // ── WITHDRAW ──

    @Test
    fun withdraw_basicParsing() {
        val msg = "QEF7XYZ8 Confirmed. Ksh10,000.00 withdrawn from John Agent Nairobi on 1/3/2025 at 4:30 PM. New M-PESA balance is Ksh2,450.00. Transaction cost, Ksh50.00."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertEquals("QEF7XYZ8", result!!.code)
        assertEquals(MpesaMessageParser.TransactionType.WITHDRAW, result.type)
        assertEquals(10000.0, result.amount, 0.01)
        assertEquals("John Agent Nairobi", result.counterpartyName)
        assertNull(result.counterpartyPhone)
        assertEquals("1/3/2025", result.date)
        assertEquals("4:30 PM", result.time)
        assertEquals(2450.0, result.balance!!, 0.01)
        assertEquals(50.0, result.transactionCost!!, 0.01)
    }

    @Test
    fun withdraw_codeExtraction() {
        val msg = "QEF7XYZ8 Confirmed. Ksh500 withdrawn from Agent on 1/1/2025 at 1:00 PM. New M-PESA balance is Ksh1000. Transaction cost, Ksh10."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertEquals("QEF7XYZ8", result!!.code)
    }

    // ── AIRTIME ──

    @Test
    fun airtime_basicParsing() {
        val msg = "QGH9XYZ0 Confirmed. You bought Ksh500.00 of airtime on 1/3/2025 at 9:00 AM. New M-PESA balance is Ksh11,950.00."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertEquals("QGH9XYZ0", result!!.code)
        assertEquals(MpesaMessageParser.TransactionType.AIRTIME, result.type)
        assertEquals(500.0, result.amount, 0.01)
        assertNull(result.counterpartyName)
        assertNull(result.counterpartyPhone)
        assertEquals("1/3/2025", result.date)
        assertEquals("9:00 AM", result.time)
        assertEquals(11950.0, result.balance!!, 0.01)
        assertNull(result.transactionCost)
    }

    @Test
    fun airtime_codeExtraction() {
        val msg = "QGH9XYZ0 Confirmed. You bought Ksh200 of airtime on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh800."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertEquals("QGH9XYZ0", result!!.code)
    }

    // ── Graceful failure ──

    @Test
    fun nonMpesaMessage_returnsNull() {
        val msg = "Hey, are you coming for lunch today?"
        assertNull(MpesaMessageParser.parse(msg))
    }

    @Test
    fun emptyString_returnsNull() {
        assertNull(MpesaMessageParser.parse(""))
    }

    @Test
    fun whitespaceOnly_returnsNull() {
        assertNull(MpesaMessageParser.parse("   \n\t  "))
    }

    @Test
    fun garbledPartialMpesa_returnsNull() {
        val msg = "Confirmed. Ksh from on at. balance is."
        assertNull(MpesaMessageParser.parse(msg))
    }

    @Test
    fun randomSmsWithKsh_returnsNull() {
        val msg = "Your Ksh500 airtime purchase was successful. Call 0800-xxx for help."
        assertNull(MpesaMessageParser.parse(msg))
    }

    @Test
    fun partialMpesaAmountOnly_returnsNull() {
        val msg = "Ksh1000 Ksh2000 balance is Ksh500"
        assertNull(MpesaMessageParser.parse(msg))
    }

    // ── Code extraction edge cases ──

    @Test
    fun code_isExtractedFromAllTypes() {
        val templates = mapOf(
            "RECEIVED" to "CODEXYZ123 Confirmed. You have received Ksh100 from Name 0700000000 on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh500.",
            "SENT" to "CODEXYZ123 Confirmed. Ksh100 sent to Name 0700000000 on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh500. Transaction cost, Ksh10.",
            "PAYBILL" to "CODEXYZ123 Confirmed. Ksh100 paid to Business. on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh500. Transaction cost, Ksh10.",
            "WITHDRAW" to "CODEXYZ123 Confirmed. Ksh100 withdrawn from Agent on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh500. Transaction cost, Ksh10.",
            "AIRTIME" to "CODEXYZ123 Confirmed. You bought Ksh100 of airtime on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh500."
        )
        templates.forEach { (type, msg) ->
            val result = MpesaMessageParser.parse(msg)
            assertNotNull("$type: should parse", result)
            assertEquals("$type: code extraction", "CODEXYZ123", result!!.code)
        }
    }

    // ── Balance extraction ──

    @Test
    fun balance_nullWhenNotPresent() {
        // Minimal message that might match but without balance
        val msg = "CODEXYZ Confirmed. You received Ksh100 from Test 0700000000 on 1/1/2025 at 12:00 PM."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertNull(result!!.balance)
    }

    @Test
    fun balance_integerAmount() {
        val msg = "CODEXYZ Confirmed. You have received Ksh500 from Test 0700000000 on 1/1/2025 at 12:00 PM. New M-PESA balance is Ksh1000."
        val result = MpesaMessageParser.parse(msg)
        assertNotNull(result)
        assertEquals(1000.0, result!!.balance!!, 0.01)
    }
}
