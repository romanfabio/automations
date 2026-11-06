package io.github.romanfabio.automations.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val REPOSITORY_URL = "https://github.com/romanfabio/automations"

/**
 * App version, the project's own license and the licenses of the libraries it ships.
 * The texts are the repository's own files, bundled as assets by the Gradle build (see copyLicenseAssets).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName.orEmpty()
    }
    val documents by produceState<List<Pair<String, List<LicenseBlock>>>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) {
            listOf(
                "License" to readLicense(context, "LICENSE"),
                "Third-party software" to readLicense(context, "THIRD_PARTY_LICENSES.md"),
                "Apache License 2.0" to readLicense(context, "APACHE-2.0.txt"),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Automations", style = MaterialTheme.typography.headlineSmall)
                Text("Version $versionName", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Text(
                    "Runs small predefined automations on a schedule or on demand. " +
                        "It has no internet permission: nothing leaves your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                OutlinedButton(onClick = { openRepository(context) }) { Text("Source code on GitHub") }
            }
            documents.forEach { (title, blocks) ->
                item {
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                    Text(title, style = MaterialTheme.typography.titleLarge)
                }
                items(blocks) { block -> LicenseBlockView(block) }
            }
        }
    }
}

@Composable
private fun LicenseBlockView(block: LicenseBlock) {
    when (block) {
        is LicenseBlock.Heading -> Text(block.text, style = MaterialTheme.typography.titleMedium)
        is LicenseBlock.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("•", style = MaterialTheme.typography.bodyMedium)
            Text(block.text, style = MaterialTheme.typography.bodyMedium)
        }
        is LicenseBlock.Paragraph -> Text(block.text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun readLicense(context: Context, assetName: String): List<LicenseBlock> =
    parseLicenseText(context.assets.open(assetName).bufferedReader().use { it.readText() })

private fun openRepository(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, REPOSITORY_URL.toUri()))
    } catch (_: ActivityNotFoundException) {
        // No browser installed: nothing useful to do.
    }
}
