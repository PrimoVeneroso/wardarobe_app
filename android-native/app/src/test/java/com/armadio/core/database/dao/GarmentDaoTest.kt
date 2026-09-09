package com.armadio.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.armadio.core.database.AppDatabase
import com.armadio.core.database.entity.Garment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GarmentDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: GarmentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.garmentDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private var seq = 0

    private fun garment(name: String, brand: String? = null, notes: String? = null): Garment {
        seq++
        return Garment(
            uuid = "uuid-$seq-$name",
            name = name,
            notes = notes,
            brand = brand,
            categoryId = 1L,
            seasonsMask = 0b1111,
            dressCodeMask = 0b0001,
            condition = "NEW",
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )
    }

    @Test
    fun edit_preservesIdentity_andActiveListExcludesDeletedRows() = runTest {
        val original = garment("Wool coat")
        val id = dao.insert(original)
        val hidden = dao.insert(garment("Old shirt"))
        dao.update(original.copy(id = id, name = "Blue coat", size = "M", updatedAt = 3_000L))
        dao.softDelete(hidden, 4_000L)
        val rows = dao.activeGarments().first()
        assertEquals(1, rows.size)
        assertEquals(id, rows.single().id)
        assertEquals(original.uuid, rows.single().uuid)
        assertEquals("Blue coat", rows.single().name)
        assertEquals("M", rows.single().size)
        assertEquals(3_000L, rows.single().updatedAt)
        assertEquals(1, dao.search("blue").first().size)
        assertEquals(0, dao.search("wool").first().size)
    }

    @Test
    fun insert_then_activeCount_isOne() = runTest {
        dao.insert(garment("Wool coat"))
        assertEquals(1, dao.activeCount().first())
    }

    @Test
    fun softDelete_hidesFromActiveCount_andFlowReemits() = runTest {
        val id = dao.insert(garment("Wool coat"))
        dao.activeCount().test {
            assertEquals(1, awaitItem())
            dao.softDelete(id, timestamp = 2_000L)
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun softDelete_isATombstone_rowIsStillThere() = runTest {
        val id = dao.insert(garment("Wool coat"))
        dao.softDelete(id, timestamp = 2_000L)
        val row = dao.byId(id)
        assertNotNull(row)
        assertEquals(2_000L, row!!.deletedAt)
    }

    @Test
    fun ftsSearch_matchesName() = runTest {
        dao.insert(garment("Cashmere sweater", brand = "Acme", notes = "birthday gift"))
        dao.insert(garment("Linen shirt", brand = "Zeta"))
        val hits = dao.search("cashmere").first()
        assertEquals(listOf("Cashmere sweater"), hits.map { it.name })
    }

    @Test
    fun ftsSearch_matchesBrand_withNullNotes() = runTest {
        // Guards the nullable columns in garments_fts (brand/notes).
        dao.insert(garment("Linen shirt", brand = "Zeta", notes = null))
        assertEquals(1, dao.search("zeta").first().size)
    }

    @Test
    fun ftsSearch_excludesSoftDeleted() = runTest {
        val id = dao.insert(garment("Cashmere sweater"))
        dao.softDelete(id, timestamp = 2_000L)
        assertEquals(0, dao.search("cashmere").first().size)
    }
}
