package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NumberProgressDao {
    @Query("SELECT * FROM number_progress")
    fun observeAll(): Flow<List<NumberProgressEntity>>

    @Query("SELECT * FROM number_progress")
    suspend fun getAllOnce(): List<NumberProgressEntity>

    @Query("SELECT * FROM number_progress WHERE number = :number LIMIT 1")
    suspend fun getByNumber(number: Int): NumberProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NumberProgressEntity)

    @Query("DELETE FROM number_progress")
    suspend fun deleteAll()
}

@Dao
interface SessionHistoryDao {
    @Query("SELECT * FROM session_history ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<SessionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SessionHistoryEntity)

    @Query("DELETE FROM session_history")
    suspend fun deleteAll()
}

@Dao
interface DiagnosticSnapshotDao {
    @Query("SELECT * FROM diagnostic_snapshots ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<DiagnosticSnapshotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DiagnosticSnapshotEntity)

    @Query("DELETE FROM diagnostic_snapshots")
    suspend fun deleteAll()
}
