package com.makemission.makeflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queued_notifications")
data class QueuedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val payload: String? = null
)
