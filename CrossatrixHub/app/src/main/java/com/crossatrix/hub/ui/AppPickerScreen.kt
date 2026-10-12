package com.crossatrix.hub.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.crossatrix.hub.sidebar.SidebarPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class DeviceApp(val label: String, val pkg: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { SidebarPrefs(ctx) }
    val selected = remember { mutableStateListOf<String>().apply { addAll(prefs.apps) } }
    var query by remember { mutableStateOf("") }

    val apps by produceState<List<DeviceApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0)
                .map { DeviceApp(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
                .filter { it.pkg != ctx.packageName }
                .distinctBy { it.pkg }
                .sortedBy { it.label.lowercase() }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Sidebar apps") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            val list = apps
            if (list == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                val filtered = list.filter { it.label.contains(query, ignoreCase = true) }
                androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search apps") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(filtered, key = { it.pkg }) { app ->
                            val checked = app.pkg in selected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selected.remove(app.pkg) else selected.add(app.pkg)
                                        prefs.apps = selected.toList()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                DeviceAppIcon(app.pkg)
                                Text(
                                    app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                )
                                Checkbox(checked = checked, onCheckedChange = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceAppIcon(pkg: String) {
    val ctx = LocalContext.current
    val bitmap = remember(pkg) {
        runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
    } else {
        Box(Modifier.size(40.dp))
    }
}
