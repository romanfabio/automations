package io.github.romanfabio.automations.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** User-editable state of one predefined automation. Rows are created lazily on first change. */
@Entity(tableName = "automation_config")
data class AutomationConfig(
    @PrimaryKey val id: String,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    /** Bit 0 = Monday ... bit 6 = Sunday. */
    val daysMask: Int,
)

enum class RunTrigger { SCHEDULED, MANUAL }

enum class RunStatus { SUCCESS, SKIPPED, FAILED }

@Entity(tableName = "run_log", indices = [Index("automationId", "timestamp")])
data class RunLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val automationId: String,
    /** Epoch millis. */
    val timestamp: Long,
    val trigger: RunTrigger,
    val status: RunStatus,
    val message: String,
)
