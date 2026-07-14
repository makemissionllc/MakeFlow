package com.makemission.makeflow.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MpesaMessageParserTest {

    // ── 9 real M-PESA fixtures ──

    @Test
    fun `fixture 1 - sent to person, no phone`() {
        val msg = "UGE32B9V7H Confirmed. Ksh30.00 sent to JOHN  KAMAU on 14/7/26 at 6:28 PM. New M-PESA balance is Ksh498.27. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,557.50. Download My OneApp on https://saf.cx/kWQpy"
        val r = MpesaMessageParser.parse(msg)
        assertNotNull(r)
        assertEquals("UGE32B9V7H", r!!.code)
        assertEquals(MpesaMessageParser.TransactionType.SENT, r.type)
        assertEquals(30.00, r.amount, 0.001)
        assertTrue(r.counterpartyName!!.contains("JOHN KAMAU"))
        assertEquals(498.27, r.balance!!, 0.001)
        assertEquals(0.00, r.transactionCost!!, 0.001)
    }

    @Test
    fun `fixture 2 - sent paybill with for account`() {
        val msg = "UGE32B9B0Q Confirmed. Ksh20.00 sent to SAFARICOM DATA BUNDLES for account SAFARICOM DATA BUNDLES on 14/7/26 at 5:25 PM. New M-PESA balance is Ksh528.27. Transaction cost, Ksh0.00."
        val r = MpesaMessageParser.parse(msg)
        assertNotNull(r)
        assertEquals("UGE32B9B0Q", r!!.code)
        assertEquals(MpesaMessageParser.TransactionType.SENT, r.type)
        assertEquals(20.00, r.amount, 0.001)
        assertTrue(r.counterpartyName!!.contains("SAFARICOM DATA BUNDLES"))
        assertEquals(528.27, r.balance!!, 0.001)
        assertEquals(0.00, r.transactionCost!!, 0.001)
    }

    @Test
    fun `fixture 3 - sent to Ziidi, no period before balance`() {
        val msg = "UGE32B922E Confirmed. Ksh18.00 sent to ZIIDI on 14/7/26 at 3:39 PM New M-PESA balance is Ksh548.27. Transaction cost, Ksh0.00.Amount you can transact within the day is 499,607.50. Pay your water/KPLC bill conveniently using M-PESA APP or use Paybill option on Lipa Na M-PESA."
        val r = MpesaMessageParser.parse(msg)
        assertNotNull(r)
        assertEquals("UGE32B922E", r!!.code)
        assertEquals(MpesaMessageParser.TransactionType.SENT, r.type)
        assertEquals(18.00, r.amount, 0.001)
        assertEquals("ZIIDI", r.counterpartyName)
        assertEquals(548.27, r.balance!!, 0.001)
        assertEquals(0.00, r.transactionCost!!, 0.001)
    }

    @Test
    fun `fixture 4 - paid to university, period before capital P in PM`() {
        val msg = "UGE32B8WX6 Confirmed. Ksh180.00 paid to STRATHMORE UNIVERSITY CATERING DEPARTMENT. on 14/7/26 at 3:39 PM.New M-PESA balance is Ksh566.27. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,625.50. Download My OneApp on https://saf.cx/lPKcC"
        val r = MpesaMessageParser.parse(msg)
        assertNotNull(r)
        assertEquals("UGE32B8WX6", r!!.code)
        assertEquals(MpesaMessageParser.TransactionType.PAYBILL, r.type)
        assertEquals(180.00, r.amount, 0.001)
        assertTrue(r.counterpartyName!!.contains("STRATHMORE UNIVERSITY CATERING DEPARTMENT"))
        assertEquals(566.27, r.balance!!, 0.001)
        assertEquals(0.00, r.transactionCost!!, 0.001)
    }

    @Test
    fun `fixture 5 - sent to person with phone`() {
        val msg = "UGE32B831U Confirmed. Ksh50.00 sent to PETER  OTIENO 0711000111 on 14/7/26 at 11:05 AM. New M-PESA balance is Ksh875.77. Transaction cost, Ksh0.00.  Amount you can transact within the day is 499,935.00. Download My OneApp on https://saf.cx/lPKcC"
        val r = MpesaMessageParser.parse(msg)
        assertNotNull(r)
        assertEquals("UGE32B831U", r!!.code)
        assertEquals(MpesaMessageParser.TransactionType.SENT, r.type)
        assertEquals(50.00, r.amount, 0.001)
        assertTrue(r.counterpartyName!!.contains("PETER OTIENO"))
        assertEquals("0711000111", r.counterpartyPhone)
        assertEquals(875.77, r.balance!!, 0.001)
        assertEquals(0.00, r.transactionCost!!, 0.001)
    }

    @Test
    fun `fixture 6 - Ratiba standing order`() {
        val msg = "UGE32B7KXZ Confirmed M-PESA Ratiba. Ksh15.00 sent to ETICA CAPITAL LTD for account 221013FA on 14/7/26 at 9:15 AM New M-PESA balance is Ksh925.77. Transaction cost Ksh5.00.Amount. you can transact within the day is 499,985.00."
        val r = MpesaMessageParser.parse(msg)
        assertNotNull(r)
        assertEquals("UGE32B7KXZ", r!!.code)
        assertEquals(MpesaMessageParser.TransactionType.SENT, r.type)
        assertTrue("isRatiba should be true", r.isRatiba)
        assertEquals(15.00, r.amount, 0.001)
        assertTrue(r.counterpartyName!!.contains("ETICA CAPITAL LTD"))
        assertEquals(925.77, r.balance!!, 0.001)
        assertEquals(5.00, r.transactionCost!!, 0.001)
    }

    @Test
    fun `fixture 7 - received international remittance`() {
        val msg = "UGD32B4KLS confirmed. You have received Ksh3,226.00 from JAMES OKELLO in UG via MFS Africa on 13/7/26 at 1:29 PM. New M-PESA balance is Ksh3,235.07."
        val r = MpesaMessageParser.parse(msg)
        assertNotNull(r)
        assertEquals("UGD32B4KLS", r!!.code)
        assertEquals(MpesaMessageParser.TransactionType.RECEIVED, r.type)
        assertEquals(3226.00, r.amount, 0.001)
        assertTrue("counterpartyName should contain JAMES OKELLO", r.counterpartyName!!.contains("JAMES OKELLO"))
        assertNull(r.counterpartyPhone)
        assertEquals(3235.07, r.balance!!, 0.001)
    }

    @Test
    fun `fixture 8 - received with no space after Confirmed`() {
        val msg = "UGC9RB2IGS Confirmed.You have received Ksh200.00 from DAVID  MUTUA 0733***333 on 12/7/26 at 11:50 PM  New M-PESA balance is Ksh247.05. Download My OneApp on https://saf.cx/lPKcC"
        val r = MpesaMessageParser.parse(msg)
        assertNotNull(r)
        assertEquals("UGC9RB2IGS", r!!.code)
        assertEquals(MpesaMessageParser.TransactionType.RECEIVED, r.type)
        assertEquals(200.00, r.amount, 0.001)
        assertTrue("counterpartyName should contain DAVID MUTUA", r.counterpartyName!!.contains("DAVID MUTUA"))
        assertEquals("0733***333", r.counterpartyPhone)
        assertEquals(247.05, r.balance!!, 0.001)
    }

    @Test
    fun `fixture 9 - sent paybill duplicate pattern`() {
        val msg = "UGD32B3PDR Confirmed. Ksh75.00 sent to SAFARICOM DATA BUNDLES for account SAFARICOM DATA BUNDLES on 13/7/26 at 8:42 AM. New M-PESA balance is Ksh9.07. Transaction cost, Ksh0.00."
        val r = MpesaMessageParser.parse(msg)
        assertNotNull(r)
        assertEquals("UGD32B3PDR", r!!.code)
        assertEquals(MpesaMessageParser.TransactionType.SENT, r.type)
        assertEquals(75.00, r.amount, 0.001)
        assertTrue(r.counterpartyName!!.contains("SAFARICOM DATA BUNDLES"))
        assertEquals(9.07, r.balance!!, 0.001)
        assertEquals(0.00, r.transactionCost!!, 0.001)
    }

    // ── Existing graceful-failure tests ──

    @Test
    fun nonMpesaMessage_returnsNull() {
        assertNull(MpesaMessageParser.parse("Hey, are you coming for lunch today?"))
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
        assertNull(MpesaMessageParser.parse("Confirmed. Ksh from on at. balance is."))
    }

    @Test
    fun randomSmsWithKsh_returnsNull() {
        assertNull(MpesaMessageParser.parse("Your Ksh500 airtime purchase was successful. Call 0800-xxx for help."))
    }

    @Test
    fun partialMpesaAmountOnly_returnsNull() {
        assertNull(MpesaMessageParser.parse("Ksh1000 Ksh2000 balance is Ksh500"))
    }

    // ── normalizeWhitespace ──

    @Test
    fun normalizeWhitespace_collapsesDoubleSpaces() {
        assertEquals("JOHN KAMAU", MpesaMessageParser.normalizeWhitespace("JOHN  KAMAU"))
    }

    @Test
    fun normalizeWhitespace_trimsEdges() {
        assertEquals("JOHN KAMAU", MpesaMessageParser.normalizeWhitespace("  JOHN  KAMAU  "))
    }

    @Test
    fun normalizeWhitespace_handlesNull() {
        assertNull(MpesaMessageParser.normalizeWhitespace(null))
    }
}
