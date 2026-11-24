package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RnaAppEntity::class, TvAppCustomEntity::class], version = 2, exportSchema = false)
abstract class RnaDatabase : RoomDatabase() {
    abstract fun rnaAppDao(): RnaAppDao

    companion object {
        @Volatile
        private var INSTANCE: RnaDatabase? = null

        fun getDatabase(context: Context): RnaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RnaDatabase::class.java,
                    "rna_apps_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
