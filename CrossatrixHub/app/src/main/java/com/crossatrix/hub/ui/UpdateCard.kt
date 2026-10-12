package com.crossatrix.hub.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

/** Opens the system installer for the downloaded APK (asks for "install unknown apps" first). */
fun installApk(context: Context, file: File) {
    if (!context.packageManager.canRequestPackageInstalls()) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@Composable
fun UpdateCard(vm: UpdateViewModel = viewModel()) {
    val ctx = LocalContext.current
    val state = vm.state

    // Start the installer as soon as a download finishes.
    LaunchedEffect(state) {
        if (state is UpdateState.Downloaded) installApk(ctx, state.file)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("App updates", style = MaterialTheme.typography.titleMedium)
            Text(
                "Installed version: ${vm.currentVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )

            when (state) {
                UpdateState.Idle -> Button(onClick = vm::check) { Text("Check for updates") }

                UpdateState.Checking -> Text("Checking for updates…", style = MaterialTheme.typography.bodyMedium)

                UpdateState.UpToDate -> {
                    Text("You're up to date.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = vm::check, modifier = Modifier.padding(top = 8.dp)) { Text("Check again") }
                }

                is UpdateState.Available -> {
                    Text(
                        "Version ${state.info.version} is available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Button(
                        onClick = { vm.download(state.info) },
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text("Download & install") }
                }

                is UpdateState.Downloading -> {
                    Text(
                        "Downloading ${state.info.version}… ${state.percent}%",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(
                        progress = { state.percent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }

                is UpdateState.Downloaded -> {
                    Text("Version ${state.info.version} is ready to install.", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "If nothing opened, allow installs from Crossatrix Hub and tap Install.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Button(
                        onClick = { installApk(ctx, state.file) },
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text("Install") }
                }

                is UpdateState.Error -> {
                    Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Button(onClick = vm::check, modifier = Modifier.padding(top = 8.dp)) { Text("Try again") }
                }
            }
        }
    }
}
