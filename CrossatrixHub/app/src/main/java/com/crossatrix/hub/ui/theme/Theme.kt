package com.crossatrix.hub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Background: HSL(220, 80%, 12%)
val HubBackground = Color(0xFF061637)
// Buttons + accent: HSL(45, 100%, 55%)
val HubAccent = Color(0xFFFFC61A)
// Slightly lighter navy for cards / fields: HSL(220, 70%, 18%)
val HubSurface = Color(0xFF0E234E)

private val HubColors = darkColorScheme(
    primary = HubAccent,
    onPrimary = HubBackground,
    secondary = HubAccent,
    secondaryContainer = Color(0xFF1A3570),
    onSecondaryContainer = HubAccent,
    onSecondary = HubBackground,
    tertiary = HubAccent,
    background = HubBackground,
    onBackground = Color(0xFFF2F5FF),
    surface = HubBackground,
    onSurface = Color(0xFFF2F5FF),
    surfaceVariant = HubSurface,
    onSurfaceVariant = Color(0xFFAEB9D6),
    surfaceContainer = HubSurface,
    surfaceContainerHigh = HubSurface,
    surfaceContainerHighest = HubSurface,
    outline = Color(0xFF5C6B94),
    error = Color(0xFFFF6B6B),
)

/** The app always uses the Crossatrix dark-navy / gold style. */
@Composable
fun CrossatrixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HubColors,
        content = content,
    )
}
