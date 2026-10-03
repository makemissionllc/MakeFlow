package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transaction_templates")
data class TransactionTemplateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val type: String,
    val categoryId: String? = null,
    val accountId: String = "",
    val note: String = "",
    val iconOrColor: String? = null
)
