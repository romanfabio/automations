package io.github.romanfabio.automations.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.romanfabio.automations.automation.isScheduled
import io.github.romanfabio.automations.data.RunStatus
import io.github.romanfabio.automations.setup.SetupItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationListScreen(
    items: List<AutomationItem>,
    setup: List<SetupItem>,
    onFixSetup: (SetupItem) -> Unit,
    onSetupDone: (SetupItem) -> Unit,
    onAbout: () -> Unit,
    onOpen: (AutomationItem) -> Unit,
    onToggle: (AutomationItem, Boolean) -> Unit,
    onRunNow: (AutomationItem) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automations") },
                actions = { TextButton(onClick = onAbout) { Text("About") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SetupCard(setup, onFixSetup, onSetupDone) }
            items(items, key = { it.automation.id }) { item ->
                AutomationCard(item, onOpen, onToggle, onRunNow)
            }
        }
    }
}

@Composable
private fun AutomationCard(
    item: AutomationItem,
    onOpen: (AutomationItem) -> Unit,
    onToggle: (AutomationItem, Boolean) -> Unit,
    onRunNow: (AutomationItem) -> Unit,
) {
    val automation = item.automation
    val config = item.config
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(item) }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(automation.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (automation.isScheduled) {
                            formatScheduleSummary(config.hour, config.minute, config.daysMask)
                        } else {
                            "Manual"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (automation.isScheduled) {
                    Switch(checked = config.enabled, onCheckedChange = { onToggle(item, it) })
                }
            }

            if (automation.isScheduled) {
                Text(
                    when {
                        !config.enabled -> "Disabled"
                        item.nextRun == null -> "No days selected"
                        else -> "Next run: ${formatRelative(item.nextRun)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            item.lastRun?.let { run ->
                Text(
                    "Last run: ${formatTimestamp(run.timestamp)} · ${run.status.label()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (run.status == RunStatus.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            if (item.unmetRequirements.isNotEmpty()) {
                Text(
                    "Setup needed: ${item.unmetRequirements.joinToString { it.title }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { onRunNow(item) },
                    enabled = item.unmetRequirements.isEmpty(),
                ) { Text("Run now") }
            }
        }
    }
}
