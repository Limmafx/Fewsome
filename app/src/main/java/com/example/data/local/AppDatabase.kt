package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NumberProgressEntity::class,
        SessionHistoryEntity::class,
        DiagnosticSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun numberProgressDao(): NumberProgressDao
    abstract fun sessionHistoryDao(): SessionHistoryDao
    abstract fun diagnosticSnapshotDao(): DiagnosticSnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "french_numbers_trainer.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
