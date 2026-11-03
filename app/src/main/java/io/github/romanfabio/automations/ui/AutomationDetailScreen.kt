package io.github.romanfabio.automations.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.romanfabio.automations.automation.isScheduled
import io.github.romanfabio.automations.data.RunLog
import io.github.romanfabio.automations.data.RunStatus
import io.github.romanfabio.automations.schedule.Weekdays
import java.time.DayOfWeek
import java.time.format.TextStyle
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AutomationDetailScreen(
    item: AutomationItem,
    history: Flow<List<RunLog>>,
    onBack: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onTime: (hour: Int, minute: Int) -> Unit,
    onDays: (Int) -> Unit,
    onRunNow: () -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val runs by history.collectAsStateWithLifecycle(initialValue = emptyList())
    val automation = item.automation
    val config = item.config

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(automation.title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text(automation.description, style = MaterialTheme.typography.bodyLarge) }

            if (automation.isScheduled) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Enabled", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Switch(checked = config.enabled, onCheckedChange = onToggle)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Time", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute -> onTime(hour, minute) },
                                    config.hour,
                                    config.minute,
                                    true,
                                ).show()
                            }) { Text(formatTime(config.hour, config.minute)) }
                        }
                        Text("Days", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DayOfWeek.entries.forEach { day ->
                                val selected = Weekdays.has(config.daysMask, day)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        val next = Weekdays.toggle(config.daysMask, day)
                                        // At least one day must stay selected, or the schedule would never fire.
                                        if (next != 0) onDays(next)
                                    },
                                    label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                                )
                            }
                        }
                        Text(
                            when {
                                !config.enabled -> "Disabled"
                                item.nextRun == null -> "No days selected"
                                else -> "Next run: ${formatRelative(item.nextRun)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (item.unmetRequirements.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Setup needed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        item.unmetRequirements.forEach { requirement ->
                            Column {
                                Text(requirement.title, style = MaterialTheme.typography.bodyLarge)
                                Text(requirement.howToFix, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onRunNow,
                    enabled = item.unmetRequirements.isEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Run now") }
            }

            item { Text("History", style = MaterialTheme.typography.titleMedium) }
            if (runs.isEmpty()) {
                item { Text("No runs yet", style = MaterialTheme.typography.bodyMedium) }
            }
            items(runs, key = { it.id }) { run ->
                Column {
                    Row {
                        Text(formatTimestamp(run.timestamp), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${run.status.label()} · ${run.trigger.name.lowercase()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (run.status == RunStatus.FAILED) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Text(run.message, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
