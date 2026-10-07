package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    @Query("SELECT * FROM exchange_rates")
    fun getAllRates(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates")
    suspend fun getAllRatesOnce(): List<ExchangeRateEntity>

    @Query("SELECT * FROM exchange_rates WHERE currencyCode = :currencyCode")
    suspend fun getRate(currencyCode: String): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRate(rate: ExchangeRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateEntity>)

    @Transaction
    suspend fun replaceRates(rates: List<ExchangeRateEntity>) {
        clearAll()
        insertRates(rates)
    }

    @Query("DELETE FROM exchange_rates WHERE currencyCode = :currencyCode")
    suspend fun deleteRate(currencyCode: String)

    @Query("DELETE FROM exchange_rates")
    suspend fun clearAll()
}
