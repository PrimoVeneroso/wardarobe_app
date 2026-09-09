package com.armadio.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.armadio.core.database.entity.Garment
import kotlinx.coroutines.flow.Flow

/**
 * F0 scope: insert, soft delete, counts, FTS search — enough to smoke-test
 * Room + Flow end to end. Filtering by nullable params (the
 * `(:x IS NULL OR ...)` pattern) and Paging arrive in F1.
 *
 * Note for F1: user input fed to MATCH must be sanitized (quoted tokens,
 * wildcard escaping) — never concatenated raw (project anti-pattern).
 */
@Dao
interface GarmentDao {

    @Insert
    suspend fun insert(garment: Garment): Long

    @Update
    suspend fun update(garment: Garment)

    @Query("SELECT * FROM garments WHERE deletedAt IS NULL ORDER BY updatedAt DESC, id DESC")
    fun activeGarments(): Flow<List<Garment>>

    @Query("UPDATE garments SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM garments WHERE deletedAt IS NULL")
    fun activeCount(): Flow<Int>

    @Query("SELECT * FROM garments WHERE id = :id")
    suspend fun byId(id: Long): Garment?

    @Query(
        """
        SELECT garments.* FROM garments
        JOIN garments_fts ON garments.id = garments_fts.docid
        WHERE garments_fts MATCH :query
          AND garments.deletedAt IS NULL
        ORDER BY garments.name
        """
    )
    fun search(query: String): Flow<List<Garment>>
}
