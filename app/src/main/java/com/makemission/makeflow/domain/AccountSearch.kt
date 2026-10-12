package com.makemission.makeflow.domain

import com.makemission.makeflow.data.local.entity.AccountEntity
import java.util.Locale

object AccountSearch {
    fun filter(accounts: List<AccountEntity>, query: String, defaultCurrency: String): List<AccountEntity> {
        val terms = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return accounts
        return accounts.filter { account ->
            val text = "${account.name} ${account.currencyCode ?: defaultCurrency}".lowercase(Locale.ROOT)
            terms.all { it in text }
        }
    }
}
