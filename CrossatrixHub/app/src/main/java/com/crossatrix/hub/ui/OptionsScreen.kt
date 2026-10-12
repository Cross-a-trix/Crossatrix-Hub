package com.crossatrix.hub.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.crossatrix.hub.sidebar.SidebarController
import com.crossatrix.hub.sidebar.SidebarPrefs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    onBack: () -> Unit,
    onChooseApps: () -> Unit,
) {
    val ctx = LocalContext.current
    val prefs = remember { SidebarPrefs(ctx) }
    var enabled by remember { mutableStateOf(prefs.enabled && Settings.canDrawOverlays(ctx)) }
    var side by remember { mutableIntStateOf(prefs.side) }
    var pendingEnable by remember { mutableStateOf(false) }
    var resetKey by remember { mutableIntStateOf(0) }
    val appCount = remember { prefs.apps.size }

    fun setEnabled(value: Boolean) {
        enabled = value
        prefs.enabled = value
        SidebarController.apply(ctx)
    }

    // Coming back from the system "Display over other apps" screen.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (pendingEnable) {
            pendingEnable = false
            if (Settings.canDrawOverlays(ctx)) setEnabled(true)
        } else if (enabled && !Settings.canDrawOverlays(ctx)) {
            setEnabled(false)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Options") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UpdateCard()

            // ---------------------------------------------------------- on/off
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Crossi sidebar", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Swipe from the screen edge, in any app, to open a drawer with your Crossi apps, balance, top games and favourite apps.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { on ->
                                when {
                                    !on -> setEnabled(false)
                                    Settings.canDrawOverlays(ctx) -> setEnabled(true)
                                    else -> {
                                        pendingEnable = true
                                        ctx.startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${ctx.packageName}"),
                                            ),
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    if (!Settings.canDrawOverlays(ctx)) {
                        Text(
                            "Turning this on opens a system screen where you allow “Display over other apps” for Crossatrix Hub.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            if (enabled) {
                // Re-created when "Reset" is pressed so every control re-reads the defaults.
                key(resetKey) {
                    // ------------------------------------------------------ edge
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Screen edge", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = side == SidebarPrefs.SIDE_LEFT,
                                    onClick = { side = SidebarPrefs.SIDE_LEFT; prefs.side = side },
                                    label = { Text("Left") },
                                )
                                FilterChip(
                                    selected = side == SidebarPrefs.SIDE_RIGHT,
                                    onClick = { side = SidebarPrefs.SIDE_RIGHT; prefs.side = side },
                                    label = { Text("Right") },
                                )
                            }
                        }
                    }

                    // ------------------------------------------------------ games
                    var showGames by remember { mutableStateOf(prefs.showGames) }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Games in sidebar", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Show the top Crossi Games in the drawer.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = showGames,
                                    onCheckedChange = { showGames = it; prefs.showGames = it },
                                )
                            }
                            if (showGames) {
                                PrefSlider(
                                    label = "Number of games",
                                    suffix = "",
                                    value = prefs.gamesCount,
                                    range = SidebarPrefs.GAMES_COUNT_RANGE,
                                    onChange = { prefs.gamesCount = it },
                                )
                            }
                        }
                    }

                    // ------------------------------------------------------ apps
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onChooseApps),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Sidebar apps", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (appCount == 0) "Choose which apps from your device appear in the sidebar"
                                else "$appCount selected · tap to change",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }

                    // ------------------------------------------------------ handle bar
                    var barColor by remember { mutableIntStateOf(prefs.barColor) }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Handle bar", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Changes show up live on the screen edge.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            PrefSlider("Touch area height", "dp", prefs.handleHeight, SidebarPrefs.HEIGHT_RANGE) { prefs.handleHeight = it }
                            PrefSlider("Touch area width", "dp", prefs.handleWidth, SidebarPrefs.WIDTH_RANGE) { prefs.handleWidth = it }
                            PrefSlider("Bar length", "dp", prefs.barLength, SidebarPrefs.BAR_LENGTH_RANGE) { prefs.barLength = it }
                            PrefSlider("Bar thickness", "dp", prefs.barThickness, SidebarPrefs.BAR_THICKNESS_RANGE) { prefs.barThickness = it }
                            PrefSlider("Vertical position", "%", prefs.position, SidebarPrefs.POSITION_RANGE) { prefs.position = it }
                            PrefSlider("Opacity", "%", prefs.barOpacity, SidebarPrefs.OPACITY_RANGE) { prefs.barOpacity = it }

                            Text(
                                "Colour",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SidebarPrefs.COLOR_CHOICES.forEach { c ->
                                    Box(
                                        Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(c))
                                            .border(
                                                width = if (barColor == c) 3.dp else 1.dp,
                                                color = if (barColor == c) Color.White else MaterialTheme.colorScheme.outline,
                                                shape = CircleShape,
                                            )
                                            .clickable { barColor = c; prefs.barColor = c },
                                    )
                                }
                            }
                        }
                    }

                    // ------------------------------------------------------ behaviour
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Behaviour", style = MaterialTheme.typography.titleMedium)
                            PrefSlider("Swipe distance to open", "dp", prefs.swipeDistance, SidebarPrefs.SWIPE_RANGE) { prefs.swipeDistance = it }
                            PrefSlider("Drawer width", "dp", prefs.drawerWidth, SidebarPrefs.DRAWER_RANGE) { prefs.drawerWidth = it }
                        }
                    }

                    TextButton(
                        onClick = { prefs.resetCustomization(); resetKey++ },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Reset customization") }
                }
            }
        }
    }
}

@Composable
private fun PrefSlider(
    label: String,
    suffix: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    var v by remember { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.padding(top = 8.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "${v.roundToInt()}${if (suffix.isEmpty()) "" else " $suffix"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = v,
            onValueChange = {
                v = it
                onChange(it.roundToInt())
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}
