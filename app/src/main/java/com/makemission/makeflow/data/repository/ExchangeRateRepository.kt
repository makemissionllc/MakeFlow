package com.makemission.makeflow.data.repository

import com.makemission.makeflow.data.local.dao.ExchangeRateDao
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    fun getAllRates(): Flow<List<ExchangeRateEntity>>
    suspend fun getAllRatesOnce(): List<ExchangeRateEntity>
    suspend fun getRate(currencyCode: String): ExchangeRateEntity?
    suspend fun upsertRate(rate: ExchangeRateEntity)
    suspend fun replaceRates(rates: List<ExchangeRateEntity>)
    suspend fun deleteRate(currencyCode: String)
    suspend fun clearAll()
}

class ExchangeRateRepositoryImpl(
    private val dao: ExchangeRateDao
) : ExchangeRateRepository {
    override fun getAllRates(): Flow<List<ExchangeRateEntity>> = dao.getAllRates()

    override suspend fun getAllRatesOnce(): List<ExchangeRateEntity> = dao.getAllRatesOnce()

    override suspend fun getRate(currencyCode: String): ExchangeRateEntity? = dao.getRate(currencyCode)

    override suspend fun upsertRate(rate: ExchangeRateEntity) = dao.upsertRate(rate)

    override suspend fun replaceRates(rates: List<ExchangeRateEntity>) = dao.replaceRates(rates)

    override suspend fun deleteRate(currencyCode: String) = dao.deleteRate(currencyCode)

    override suspend fun clearAll() = dao.clearAll()
}
