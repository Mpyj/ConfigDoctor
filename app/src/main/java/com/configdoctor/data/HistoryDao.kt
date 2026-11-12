package com.configdoctor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history")
    suspend fun clearAll()
}