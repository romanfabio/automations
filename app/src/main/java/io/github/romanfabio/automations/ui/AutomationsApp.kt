package io.github.romanfabio.automations.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Two destinations only (list, detail), so a saved id is enough and no navigation library is needed. */
@Composable
fun AutomationsApp(viewModel: AutomationsViewModel = viewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    val open = items.firstOrNull { it.automation.id == openId }

    if (open == null) {
        AutomationListScreen(
            items = items,
            onOpen = { openId = it.automation.id },
            onToggle = { item, enabled -> viewModel.setEnabled(item.automation, enabled) },
            onRunNow = { viewModel.runNow(it.automation) },
        )
    } else {
        BackHandler { openId = null }
        AutomationDetailScreen(
            item = open,
            history = viewModel.history(open.automation.id),
            onBack = { openId = null },
            onToggle = { viewModel.setEnabled(open.automation, it) },
            onTime = { hour, minute -> viewModel.setTime(open.automation, hour, minute) },
            onDays = { viewModel.setDays(open.automation, it) },
            onRunNow = { viewModel.runNow(open.automation) },
        )
    }
}
