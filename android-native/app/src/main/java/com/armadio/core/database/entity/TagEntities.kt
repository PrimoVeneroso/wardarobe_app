package com.armadio.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // ADR 0002
    val name: String,
)

@Entity(tableName = "garment_tags", primaryKeys = ["garmentId", "tagId"])
data class GarmentTag(
    val garmentId: Long,
    val tagId: Long,
)
