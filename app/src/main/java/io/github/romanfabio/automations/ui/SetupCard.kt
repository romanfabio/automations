package io.github.romanfabio.automations.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.romanfabio.automations.setup.SetupItem

/** Checklist of what must be configured on the phone. Open while something is missing, collapsed once all is well. */
@Composable
fun SetupCard(
    items: List<SetupItem>,
    onFix: (SetupItem) -> Unit,
    onDone: (SetupItem) -> Unit,
) {
    val missing = items.count { !it.met }
    var userExpanded by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val expanded = userExpanded ?: (missing > 0)
    val problems = missing > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (problems) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (problems) "Setup: $missing of ${items.size} to do" else "Setup: all good",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = { userExpanded = !expanded }) { Text(if (expanded) "Hide" else "Show") }
            }
            if (expanded) items.forEach { SetupRow(it, onFix, onDone) }
        }
    }
}

@Composable
private fun SetupRow(item: SetupItem, onFix: (SetupItem) -> Unit, onDone: (SetupItem) -> Unit) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (item.met) "✔" else "✖", style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge)
            if (!item.met) {
                Text(item.detail, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.intent != null || item.needsNotificationPermission) {
                        OutlinedButton(onClick = { onFix(item) }) { Text("Open") }
                    }
                    if (item.manual) {
                        OutlinedButton(onClick = { onDone(item) }) { Text("Done") }
                    }
                }
            }
        }
    }
}
