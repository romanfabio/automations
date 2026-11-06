package io.github.romanfabio.automations.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.romanfabio.automations.setup.SetupItem

/** Two destinations only (list, detail), so a saved id is enough and no navigation library is needed. */
@Composable
fun AutomationsApp(viewModel: AutomationsViewModel = viewModel()) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsStateWithLifecycle()
    val setup by viewModel.setup.collectAsStateWithLifecycle()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val open = items.firstOrNull { it.automation.id == openId }

    fun startSafely(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Some ROMs lack a screen; the app info page always exists.
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData("package:${context.packageName}".toUri()),
            )
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Android stops showing the dialog after repeated denials; then only the settings page can help.
        if (!granted) {
            startSafely(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        }
        viewModel.refresh()
    }

    val fixSetup: (SetupItem) -> Unit = { item ->
        if (item.needsNotificationPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (item.intent != null) {
            startSafely(item.intent)
        }
    }

    if (showAbout) {
        BackHandler { showAbout = false }
        AboutScreen(onBack = { showAbout = false })
    } else if (open == null) {
        AutomationListScreen(
            items = items,
            setup = setup,
            onFixSetup = fixSetup,
            onSetupDone = viewModel::markSetupDone,
            onAbout = { showAbout = true },
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
