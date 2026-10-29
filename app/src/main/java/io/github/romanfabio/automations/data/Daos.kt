package io.github.romanfabio.automations.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationConfigDao {
    @Query("SELECT * FROM automation_config")
    fun observeAll(): Flow<List<AutomationConfig>>

    @Query("SELECT * FROM automation_config WHERE id = :id")
    suspend fun get(id: String): AutomationConfig?

    @Upsert
    suspend fun upsert(config: AutomationConfig)
}

@Dao
interface RunLogDao {
    @Insert
    suspend fun insert(log: RunLog)

    @Query("SELECT * FROM run_log WHERE automationId = :id ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun observeHistory(id: String, limit: Int): Flow<List<RunLog>>

    @Query("SELECT * FROM run_log WHERE id IN (SELECT MAX(id) FROM run_log GROUP BY automationId)")
    fun observeLatestPerAutomation(): Flow<List<RunLog>>

    @Query("DELETE FROM run_log WHERE id NOT IN (SELECT id FROM run_log ORDER BY timestamp DESC, id DESC LIMIT :keep)")
    suspend fun prune(keep: Int)
}
