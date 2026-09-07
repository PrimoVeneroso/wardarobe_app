package com.armadio.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guards the golden schema JSON in app/schemas (constraint #7). Every future
 * schema change must extend this file with MigrationTestHelper tests —
 * destructive migrations are forbidden.
 */
@RunWith(RobolectricTestRunner::class)
class SchemaV1Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun v1_goldenSchema_containsEveryTable() {
        val expected = setOf(
            "garments", "garments_fts", "trips", "trip_items", "loans",
            "outfits", "outfit_items", "wear_log", "tags", "garment_tags",
        )
        helper.createDatabase("schema-tables.db", 1).use { db ->
            val tables = mutableSetOf<String>()
            db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
                while (cursor.moveToNext()) tables.add(cursor.getString(0))
            }
            tables.removeAll { it.startsWith("sqlite_") }
            org.junit.Assert.assertTrue(
                "Mancano tabelle attese: ${expected - tables}",
                tables.containsAll(expected)
            )
        }
    }

    @Test
    fun v1_goldenSchema_isSelfConsistent() {
        helper.createDatabase("schema-consistency.db", 1).close()
        helper.runMigrationsAndValidate("schema-consistency.db", 1, true)
    }

    @Test
    fun v1_roomRuntimeSchema_matchesGoldenSchemaJson() {
        // Creates the db through Room's own builder, then validates the file
        // against the committed golden JSON: any drift between code and
        // schema fails here.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "schema-runtime.db")
            .allowMainThreadQueries()
            .build()
        db.openHelper.writableDatabase // force schema creation
        db.close()
        helper.runMigrationsAndValidate("schema-runtime.db", 1, true)
    }
}
