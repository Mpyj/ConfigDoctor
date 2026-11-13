package com.configdoctor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val link: String,
    val timestamp: Long,
    val status: String,
    val outputIp: String?,
    val outputCountry: String?,
    val realDelayMs: Long?,
    val packetLossPercent: Float? = null,   // ← جدید
    val jitterMs: Long? = null,             // ← جدید
    val avgPingMs: Long? = null             // ← جدید
)