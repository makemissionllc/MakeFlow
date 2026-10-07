package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val balance: Double,
    val initialBalance: Double,
    val colorHex: String = "#C4D42E",
    val currencyCode: String? = null
)
