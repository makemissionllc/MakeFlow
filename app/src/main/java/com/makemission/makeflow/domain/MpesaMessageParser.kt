package com.makemission.makeflow.domain

/**
 * Pure-Kotlin M-PESA confirmation message parser.
 * Zero Android dependencies — fully unit-testable.
 *
 * Parses Safaricom M-PESA SMS confirmation messages into structured data.
 * Returns null for any text that does not match a known M-PESA template.
 */
object MpesaMessageParser {

    enum class TransactionType {
        RECEIVED,
        SENT,
        PAYBILL,
        WITHDRAW,
        AIRTIME,
        UNKNOWN
    }

    data class MpesaParsedMessage(
        val code: String,
        val type: TransactionType,
        val amount: Double,
        val counterpartyName: String?,
        val counterpartyPhone: String?,
        val date: String,
        val time: String,
        val balance: Double?,
        val transactionCost: Double?
    )

    /**
     * Attempt to parse an M-PESA confirmation SMS.
     * Returns a [MpesaParsedMessage] if the text matches a known template,
     * or null if it does not — never crashes, never guesses.
     */
    fun parse(text: String): MpesaParsedMessage? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        return tryParseReceived(trimmed)
            ?: tryParseSent(trimmed)
            ?: tryParsePaybill(trimmed)
            ?: tryParseWithdraw(trimmed)
            ?: tryParseAirtime(trimmed)
    }

    // ── Helpers ──

    private fun extractCode(text: String): String? {
        val match = Regex("^[A-Z0-9]+").find(text) ?: return null
        val code = match.value
        // Confirmation codes are typically 8-13 chars; skip very short matches
        // that are just leading digits of an amount.
        if (code.length < 4) return null
        return code
    }

    private fun extractAmount(text: String): Double? {
        // Match "Ksh1,234.56" or "Ksh1234" (with optional comma grouping)
        val match = Regex("Ksh([\\d,]+(?:\\.\\d{1,2})?)").find(text) ?: return null
        val raw = match.groupValues[1].replace(",", "")
        return raw.toDoubleOrNull()
    }

    private fun extractAmounts(text: String): List<Double> {
        return Regex("Ksh([\\d,]+(?:\\.\\d{1,2})?)")
            .findAll(text)
            .map { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filterNotNull()
            .toList()
    }

    private fun extractDateAndTime(text: String): Pair<String, String>? {
        // Matches: "on 1/1/2025 at 10:30 AM" or "on 1/1/2025 at 10:30AM"
        val match = Regex("on (\\d{1,2}/\\d{1,2}/\\d{4}) at (\\d{1,2}:\\d{2}\\s*(?:AM|PM)?)").find(text)
            ?: return null
        return match.groupValues[1] to match.groupValues[2].trim()
    }

    private fun extractBalance(text: String): Double? {
        // "New M-PESA balance is Ksh1,234" — this is typically the LAST Ksh amount
        val match = Regex("balance is Ksh([\\d,]+(?:\\.\\d{1,2})?)").find(text) ?: return null
        val raw = match.groupValues[1].replace(",", "")
        return raw.toDoubleOrNull()
    }

    private fun extractCost(text: String): Double? {
        val match = Regex("Transaction cost,? Ksh([\\d,]+(?:\\.\\d{1,2})?)").find(text)
            ?: return null
        val raw = match.groupValues[1].replace(",", "")
        return raw.toDoubleOrNull()
    }

    private fun extractPersonDetails(text: String): Pair<String, String>? {
        // Matches "from John Doe 0712345678" or "to John Doe 0712345678"
        val match = Regex("(?:from|to) ([A-Za-z .]+?) (0\\d{9,10})").find(text) ?: return null
        return match.groupValues[1].trim() to match.groupValues[2]
    }

    private fun extractBusinessName(text: String): String? {
        // "paid to KPLC Kenya. on" — extract name between "paid to" and ". on"
        val match = Regex("paid to ([^.]+)\\.").find(text) ?: return null
        return match.groupValues[1].trim()
    }

    private fun extractAgentName(text: String): String? {
        // "withdrawn from Agent Name on"
        val match = Regex("from ([A-Za-z .]+?) on").find(text) ?: return null
        return match.groupValues[1].trim()
    }

    private fun parseAmountsAndBalance(
        amounts: List<Double>,
        balance: Double?,
        transactionCost: Double?
    ): Pair<Double, Double?> {
        // The first Ksh amount is the transaction amount.
        // If there's a balance, it's the last Ksh amount (or extracted separately).
        val amount = amounts.firstOrNull() ?: return 0.0 to balance
        return amount to balance
    }

    // ── Template parsers ──

    private fun tryParseReceived(text: String): MpesaParsedMessage? {
        // "[CODE] Confirmed. You have received Ksh[AMOUNT] from [NAME] [PHONE]
        //  on [DATE] at [TIME]. New M-PESA balance is Ksh[BALANCE]."
        if (!text.contains("received", ignoreCase = true)) return null

        val code = extractCode(text) ?: return null
        val amounts = extractAmounts(text)
        if (amounts.isEmpty()) return null
        val amount = amounts.first()
        val balance = extractBalance(text)
        val (date, time) = extractDateAndTime(text) ?: return null
        val (name, phone) = extractPersonDetails(text) ?: (null to null)

        return MpesaParsedMessage(
            code = code,
            type = TransactionType.RECEIVED,
            amount = amount,
            counterpartyName = name,
            counterpartyPhone = phone,
            date = date,
            time = time,
            balance = balance,
            transactionCost = null
        )
    }

    private fun tryParseSent(text: String): MpesaParsedMessage? {
        // "[CODE] Confirmed. Ksh[AMOUNT] sent to [NAME] [PHONE] on [DATE]
        //  at [TIME]. New M-PESA balance is Ksh[BALANCE]. Transaction cost, Ksh[COST]."
        if (!text.contains("sent to", ignoreCase = true)) return null

        val code = extractCode(text) ?: return null
        val amounts = extractAmounts(text)
        if (amounts.isEmpty()) return null
        val amount = amounts.first()
        val balance = extractBalance(text)
        val cost = extractCost(text)
        val (date, time) = extractDateAndTime(text) ?: return null
        val (name, phone) = extractPersonDetails(text) ?: (null to null)

        return MpesaParsedMessage(
            code = code,
            type = TransactionType.SENT,
            amount = amount,
            counterpartyName = name,
            counterpartyPhone = phone,
            date = date,
            time = time,
            balance = balance,
            transactionCost = cost
        )
    }

    private fun tryParsePaybill(text: String): MpesaParsedMessage? {
        // "[CODE] Confirmed. Ksh[AMOUNT] paid to [BUSINESS NAME]. on [DATE]
        //  at [TIME]. New M-PESA balance is Ksh[BALANCE]. Transaction cost, Ksh[COST]."
        if (!text.contains("paid to", ignoreCase = true)) return null

        val code = extractCode(text) ?: return null
        val amounts = extractAmounts(text)
        if (amounts.isEmpty()) return null
        val amount = amounts.first()
        val balance = extractBalance(text)
        val cost = extractCost(text)
        val (date, time) = extractDateAndTime(text) ?: return null
        val businessName = extractBusinessName(text)

        return MpesaParsedMessage(
            code = code,
            type = TransactionType.PAYBILL,
            amount = amount,
            counterpartyName = businessName,
            counterpartyPhone = null,
            date = date,
            time = time,
            balance = balance,
            transactionCost = cost
        )
    }

    private fun tryParseWithdraw(text: String): MpesaParsedMessage? {
        // "[CODE] Confirmed. Ksh[AMOUNT] withdrawn from [AGENT] on [DATE]
        //  at [TIME]. New M-PESA balance is Ksh[BALANCE]. Transaction cost, Ksh[COST]."
        if (!text.contains("withdrawn from", ignoreCase = true)) return null

        val code = extractCode(text) ?: return null
        val amounts = extractAmounts(text)
        if (amounts.isEmpty()) return null
        val amount = amounts.first()
        val balance = extractBalance(text)
        val cost = extractCost(text)
        val (date, time) = extractDateAndTime(text) ?: return null
        val agentName = extractAgentName(text)

        return MpesaParsedMessage(
            code = code,
            type = TransactionType.WITHDRAW,
            amount = amount,
            counterpartyName = agentName,
            counterpartyPhone = null,
            date = date,
            time = time,
            balance = balance,
            transactionCost = cost
        )
    }

    private fun tryParseAirtime(text: String): MpesaParsedMessage? {
        // "[CODE] Confirmed. You bought Ksh[AMOUNT] of airtime on [DATE]
        //  at [TIME]. New M-PESA balance is Ksh[BALANCE]."
        if (!text.contains("airtime", ignoreCase = true)) return null

        val code = extractCode(text) ?: return null
        val amounts = extractAmounts(text)
        if (amounts.isEmpty()) return null
        val amount = amounts.first()
        val balance = extractBalance(text)
        val (date, time) = extractDateAndTime(text) ?: return null

        return MpesaParsedMessage(
            code = code,
            type = TransactionType.AIRTIME,
            amount = amount,
            counterpartyName = null,
            counterpartyPhone = null,
            date = date,
            time = time,
            balance = balance,
            transactionCost = null
        )
    }
}
