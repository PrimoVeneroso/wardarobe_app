package com.armadio.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfits")
data class Outfit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val seasonsMask: Int,
    val dressCodeMask: Int,
    val rating: Int? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null, // tombstone, same semantics as Garment
)

/** Fixed-slot layout, persisted (F2: TOP | BOTTOM | SHOES | ... stable codes). */
@Entity(tableName = "outfit_items", primaryKeys = ["outfitId", "garmentId"])
data class OutfitItem(
    val outfitId: Long,
    val garmentId: Long,
    val slot: String,
    val sortOrder: Int,
)
