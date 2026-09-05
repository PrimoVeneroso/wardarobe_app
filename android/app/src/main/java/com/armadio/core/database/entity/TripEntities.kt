package com.armadio.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val startDate: Long, // epoch day
    val endDate: Long,
    val climate: String? = null, // stable code (COLD | MILD | HOT), not localized
)

/**
 * Packing checklist entries. PERSISTENT user data — never a TEMPORARY table
 * (project anti-pattern list). Packing state is relational state on this
 * entity, not a boolean on Garment.
 */
@Entity(tableName = "trip_items", primaryKeys = ["tripId", "garmentId"])
data class TripItem(
    val tripId: Long,
    val garmentId: Long,
    val plannedQty: Int = 1,
    val packed: Boolean = false,
)

/** Loan ledger — one row per garment while it is out (ADR 0002). */
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey val garmentId: Long,
    val toWhom: String,
    val outDate: Long,          // epoch day
    val expectedBack: Long? = null,
    val returnedAt: Long? = null,
)
