package com.makemission.makeflow.domain

object MpesaMessageParser {

    fun isMpesaSender(sender: String?): Boolean {
        val normalized = sender
            ?.trim()
            ?.replace(Regex("[\\s\\p{Pd}]"), "")
            ?.uppercase(java.util.Locale.ROOT)
        return normalized == "MPESA"
    }

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
        val transactionCost: Double?,
        val isRatiba: Boolean = false,
        val rawCounterparty: String? = null
    )

    fun parse(text: String): MpesaParsedMessage? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        if (!trimmed.contains("confirmed", ignoreCase = true)) return null

        val code = extractCode(trimmed) ?: return null

        val isRatiba = Regex("confirmed\\s+M-?PESA\\s+ratiba", RegexOption.IGNORE_CASE)
            .containsMatchIn(trimmed)

        val dt = extractFlexibleDateTime(trimmed) ?: return null
        val (dateStr, timeStr) = dt

        val amounts = extractAmounts(trimmed)
        if (amounts.isEmpty()) return null
        val amount = amounts.first()

        val balance = extractFlexibleBalance(trimmed)
        val cost = extractFlexibleCost(trimmed)

        val typeAndCounterparty = detectTypeAndCounterparty(trimmed, dateStr)
        val type = typeAndCounterparty.first
        val counterpartyRaw = typeAndCounterparty.second

        val phone = extractPhone(counterpartyRaw)

        return MpesaParsedMessage(
            code = code,
            type = type,
            amount = amount,
            counterpartyName = normalizeWhitespace(counterpartyRaw)
                ?.trimEnd('.', ' ')
                ?.takeIf { it.isNotEmpty() },
            counterpartyPhone = phone,
            date = dateStr,
            time = timeStr,
            balance = balance,
            transactionCost = cost,
            isRatiba = isRatiba,
            rawCounterparty = counterpartyRaw?.trimEnd('.', ' ')?.takeIf { it.isNotEmpty() }
        )
    }

    fun normalizeWhitespace(text: String?): String? {
        if (text == null) return null
        return text.replace(Regex("\\s+"), " ").trim()
    }

    private fun extractCode(text: String): String? {
        val match = Regex("""^[A-Z0-9]+""").find(text) ?: return null
        val code = match.value
        return code.takeIf { it.length >= 4 }
    }

    internal fun extractAmounts(text: String): List<Double> {
        return Regex("""Ksh([\d,]+(?:\.\d{1,2})?)""")
            .findAll(text)
            .map { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filterNotNull()
            .toList()
    }

    private fun extractPhone(text: String?): String? {
        if (text == null) return null
        val match = Regex("""(0\d{9,10}|(?:\d+)\*+(?:\d+))""").find(text)
        return match?.value
    }

    private fun extractFlexibleDateTime(text: String): Pair<String, String>? {
        val match = Regex(
            """on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}(?:\s*[AP]M)?)""",
            RegexOption.IGNORE_CASE
        ).find(text) ?: return null
        val rawTime = match.groupValues[2].trim()
        val time = if (rawTime.endsWith("M", ignoreCase = true) || rawTime.endsWith("m", ignoreCase = true)) {
            rawTime
        } else {
            rawTime.uppercase().let { t ->
                when {
                    t.contains("AM") -> t
                    t.contains("PM") -> t
                    else -> "$t AM"
                }
            }
        }
        return match.groupValues[1] to time
    }

    private fun extractFlexibleBalance(text: String): Double? {
        val match = Regex(
            """balance\s+is\s+Ksh([\d,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        ).find(text) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    private fun extractFlexibleCost(text: String): Double? {
        val match = Regex(
            """Transaction\s+cost,?\s*Ksh([\d,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        ).find(text) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    private fun detectTypeAndCounterparty(
        text: String,
        dateStr: String
    ): Pair<TransactionType, String?> {
        val afterConfirmed = text.replaceFirst(Regex("""^[A-Z0-9]+\s+confirmed""", RegexOption.IGNORE_CASE), "")
            .trimStart('.', ' ', '\t')

        val onDatePattern = "on\\s+${Regex.escape(dateStr)}"
        val dateRegex = Regex(onDatePattern, RegexOption.IGNORE_CASE)

        val matchFrom = Regex("""received\s+Ksh[\d,]+(?:\.\d{1,2})?\s+from\s+(.+?)\s+${onDatePattern}""",
            RegexOption.IGNORE_CASE).find(text)
        if (matchFrom != null) {
            val counterparty = matchFrom.groupValues[1].trim()
            return TransactionType.RECEIVED to counterparty
        }

        val matchSent = Regex("""sent\s+to\s+(.+?)\s+${onDatePattern}""",
            RegexOption.IGNORE_CASE).find(text)
        val matchPaid = Regex("""paid\s+to\s+(.+?)\s+${onDatePattern}""",
            RegexOption.IGNORE_CASE).find(text)

        if (matchPaid != null) {
            val counterparty = matchPaid.groupValues[1].trim()
            return TransactionType.PAYBILL to counterparty
        }

        if (matchSent != null) {
            val counterparty = matchSent.groupValues[1].trim()
            return TransactionType.SENT to counterparty
        }

        val matchWithdrawn = Regex("""withdrawn\s+from\s+(.+?)\s+${onDatePattern}""",
            RegexOption.IGNORE_CASE).find(text)
        if (matchWithdrawn != null) {
            val counterparty = matchWithdrawn.groupValues[1].trim()
            return TransactionType.WITHDRAW to counterparty
        }

        if (text.contains("airtime", ignoreCase = true)) {
            return TransactionType.AIRTIME to null
        }

        return TransactionType.UNKNOWN to null
    }
}
