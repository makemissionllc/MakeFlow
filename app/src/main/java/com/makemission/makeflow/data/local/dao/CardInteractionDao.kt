package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.makemission.makeflow.data.local.entity.CardInteractionEntity

data class CardHourCount(
    val cardType: String,
    val hourOfDay: Int,
    val count: Int
)

@Dao
interface CardInteractionDao {
    @Insert
    suspend fun insert(entity: CardInteractionEntity)

    @Query("""
        SELECT cardType, hourOfDay, COUNT(*) AS count
        FROM card_interactions
        GROUP BY cardType, hourOfDay
    """)
    suspend fun getInteractionCounts(): List<CardHourCount>

    @Query("DELETE FROM card_interactions WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
}
