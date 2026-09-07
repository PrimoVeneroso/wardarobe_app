package com.armadio.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A wardrobe item.
 *
 * Rules encoded here (project constraints):
 * - Dynamic state (packed, loaned out, worn) lives in relational tables
 *   (trip_items, loans, wear_log) — NEVER in boolean columns on this entity.
 * - [imageFile]/[thumbFile] are file NAMES only, resolved at runtime against
 *   context.filesDir/wardrobe. No absolute paths in the DB, no BLOBs.
 * - Money is [priceCents] (minor units) + [currency] (ISO 4217). Never Float.
 * - [condition], [colorFamily], etc. are stable codes, never localized text.
 * - [deletedAt] is a tombstone for soft delete + merge (last-write-wins on
 *   [updatedAt] during import).
 */
@Entity(
    tableName = "garments",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["categoryId"]),
        Index(value = ["deletedAt"]),
    ],
)
data class Garment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val notes: String? = null,
    val imageFile: String? = null,
    val thumbFile: String? = null,
    val categoryId: Long,
    val brand: String? = null,
    val size: String? = null,
    val colorPrimary: Int? = null,   // ARGB
    val colorFamily: String? = null, // derived (white/black/gray/...) for filter chips
    val colorSecondary: Int? = null,
    val pattern: String? = null,
    val fabric: String? = null,
    val careFlags: Int = 0,          // bitmask of GINETEX care symbols
    val seasonsMask: Int,            // bitmask, never a mono-value enum
    val dressCodeMask: Int,          // bitmask
    val condition: String,           // NEW | GOOD | WORN | REPLACE
    val quantity: Int = 1,           // basics: 5 identical socks = 1 record
    val priceCents: Int? = null,
    val currency: String? = null,
    val purchaseDate: Long? = null,  // epoch day
    val createdAt: Long,             // epoch millis
    val updatedAt: Long,             // epoch millis, merge last-write-wins
    val deletedAt: Long? = null,     // epoch millis of the soft delete
)

/** FTS mirror of [Garment]; Room keeps it in sync via generated triggers. */
@Fts4(contentEntity = Garment::class)
@Entity(tableName = "garments_fts")
data class GarmentFts(
    val name: String,
    val brand: String?,
    val notes: String?,
)
