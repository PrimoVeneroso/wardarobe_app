package com.armadio.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One "worn today" event per (day, outfit or garment).
 * Deviation per ADR 0002: surrogate autoincrement id because Room requires a
 * PK; the logical key used by stats and by future export/import is
 * (date, outfitId, garmentId).
 */
@Entity(
    tableName = "wear_log",
    indices = [Index("date"), Index("garmentId")],
)
data class WearLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,           // epoch day
    val outfitId: Long? = null,
    val garmentId: Long? = null,
)
