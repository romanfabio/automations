package io.github.romanfabio.automations

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.romanfabio.automations.ui.AutomationsApp
import io.github.romanfabio.automations.ui.AutomationsTheme
import io.github.romanfabio.automations.ui.AutomationsViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AutomationsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Needed for the "automation failed" notification. Phase 5 moves this into a setup screen.
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            AutomationsTheme { AutomationsApp(viewModel) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
