package com.makemission.makeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.makemission.makeflow.data.local.entity.TransactionTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTemplateDao {
    @Query("SELECT * FROM transaction_templates ORDER BY name ASC")
    fun getAll(): Flow<List<TransactionTemplateEntity>>

    @Query("SELECT * FROM transaction_templates ORDER BY name ASC")
    suspend fun getAllOnce(): List<TransactionTemplateEntity>

    @Query("SELECT * FROM transaction_templates WHERE id = :id")
    suspend fun getById(id: String): TransactionTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionTemplateEntity)

    @Update
    suspend fun update(entity: TransactionTemplateEntity)

    @Delete
    suspend fun delete(entity: TransactionTemplateEntity)

    @Query("DELETE FROM transaction_templates")
    suspend fun clearAll()
}
