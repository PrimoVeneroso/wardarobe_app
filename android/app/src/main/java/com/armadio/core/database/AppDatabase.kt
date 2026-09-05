package com.armadio.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.armadio.core.database.dao.GarmentDao
import com.armadio.core.database.entity.Garment
import com.armadio.core.database.entity.GarmentFts
import com.armadio.core.database.entity.GarmentTag
import com.armadio.core.database.entity.Loan
import com.armadio.core.database.entity.Outfit
import com.armadio.core.database.entity.OutfitItem
import com.armadio.core.database.entity.Tag
import com.armadio.core.database.entity.Trip
import com.armadio.core.database.entity.TripItem
import com.armadio.core.database.entity.WearLog

/**
 * Schema v1 — the full data model is declared up front so the golden schema
 * JSON is stable from day one. Adding DAOs does not change the schema; adding
 * tables/columns requires a Migration + MigrationTestHelper test
 * (fallbackToDestructiveMigration is FORBIDDEN — constraint #7).
 */
@Database(
    entities = [
        Garment::class,
        GarmentFts::class,
        Trip::class,
        TripItem::class,
        Loan::class,
        Outfit::class,
        OutfitItem::class,
        WearLog::class,
        Tag::class,
        GarmentTag::class,
    ],
    version = 1,
    exportSchema = true, // app/schemas is committed
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun garmentDao(): GarmentDao
    // trip/outfit/tag DAOs land with their features (F2/F3).

    companion object {
        const val NAME = "armario.db"
    }
}
