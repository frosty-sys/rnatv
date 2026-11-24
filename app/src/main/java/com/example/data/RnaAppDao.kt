package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RnaAppDao {
    @Query("SELECT * FROM rna_apps ORDER BY favoriteOrder ASC, lastLaunchedAt DESC")
    fun getAllApps(): Flow<List<RnaAppEntity>>

    @Query("SELECT * FROM rna_apps WHERE id = :id LIMIT 1")
    suspend fun getAppById(id: String): RnaAppEntity?

    @Query("SELECT * FROM rna_apps WHERE assignedSlot = :slot LIMIT 1")
    suspend fun getAppBySlot(slot: Int): RnaAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: RnaAppEntity)

    @Update
    suspend fun updateApp(app: RnaAppEntity)

    @Query("DELETE FROM rna_apps WHERE id = :id")
    suspend fun deleteAppById(id: String)

    @Query("UPDATE rna_apps SET lastLaunchedAt = :timestamp WHERE id = :id")
    suspend fun updateLastLaunched(id: String, timestamp: Long)

    @Query("SELECT assignedSlot FROM rna_apps WHERE assignedSlot IS NOT NULL")
    suspend fun getUsedSlots(): List<Int>

    // TV App Customizations
    @Query("SELECT * FROM tv_app_customizations")
    fun getAllTvCustomizations(): Flow<List<TvAppCustomEntity>>

    @Query("SELECT * FROM tv_app_customizations WHERE packageName = :pkg LIMIT 1")
    suspend fun getTvCustomization(pkg: String): TvAppCustomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTvCustomization(entity: TvAppCustomEntity)
}
