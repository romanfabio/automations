package io.github.romanfabio.automations

import android.os.Bundle
import androidx.activity.ComponentActivity
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
        setContent {
            AutomationsTheme { AutomationsApp(viewModel) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
