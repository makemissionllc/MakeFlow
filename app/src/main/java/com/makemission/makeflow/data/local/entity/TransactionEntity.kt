package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val type: String,
    val categoryId: String? = null,
    val accountId: String = "",
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
