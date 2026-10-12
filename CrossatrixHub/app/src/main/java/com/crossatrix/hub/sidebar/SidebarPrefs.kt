package com.crossatrix.hub.sidebar

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import androidx.core.content.ContextCompat

class SidebarPrefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("sidebar", Context.MODE_PRIVATE)

    private fun int(key: String, def: Int, range: IntRange) = p.getInt(key, def).coerceIn(range)

    var enabled: Boolean
        get() = p.getBoolean("enabled", false)
        set(v) = p.edit().putBoolean("enabled", v).apply()

    var side: Int
        get() = p.getInt("side", SIDE_RIGHT)
        set(v) = p.edit().putInt("side", v).apply()

    /** Package names of the device apps shown in the sidebar, in the order they were picked. */
    var apps: List<String>
        get() = p.getString("apps", "").orEmpty().split("\n").filter { it.isNotBlank() }
        set(v) = p.edit().putString("apps", v.joinToString("\n")).apply()

    // ---- Games in the drawer
    var showGames: Boolean
        get() = p.getBoolean("show_games", true)
        set(v) = p.edit().putBoolean("show_games", v).apply()

    var gamesCount: Int
        get() = int("games_count", 5, GAMES_COUNT_RANGE)
        set(v) = p.edit().putInt("games_count", v).apply()

    // ---- Handle bar appearance (all sizes in dp)
    var handleHeight: Int
        get() = int("handle_height", 160, HEIGHT_RANGE)
        set(v) = p.edit().putInt("handle_height", v).apply()

    var handleWidth: Int
        get() = int("handle_width", 24, WIDTH_RANGE)
        set(v) = p.edit().putInt("handle_width", v).apply()

    var barLength: Int
        get() = int("bar_length", 64, BAR_LENGTH_RANGE)
        set(v) = p.edit().putInt("bar_length", v).apply()

    var barThickness: Int
        get() = int("bar_thickness", 4, BAR_THICKNESS_RANGE)
        set(v) = p.edit().putInt("bar_thickness", v).apply()

    /** 0 = top of the screen, 100 = bottom. */
    var position: Int
        get() = int("position", 50, POSITION_RANGE)
        set(v) = p.edit().putInt("position", v).apply()

    var barColor: Int
        get() = p.getInt("bar_color", DEFAULT_COLOR)
        set(v) = p.edit().putInt("bar_color", v).apply()

    /** 0..100 */
    var barOpacity: Int
        get() = int("bar_opacity", 80, OPACITY_RANGE)
        set(v) = p.edit().putInt("bar_opacity", v).apply()

    // ---- Behaviour
    var swipeDistance: Int
        get() = int("swipe_distance", 28, SWIPE_RANGE)
        set(v) = p.edit().putInt("swipe_distance", v).apply()

    var drawerWidth: Int
        get() = int("drawer_width", 320, DRAWER_RANGE)
        set(v) = p.edit().putInt("drawer_width", v).apply()

    /** Resets everything except on/off, edge side and the chosen apps. */
    fun resetCustomization() {
        p.edit()
            .remove("show_games").remove("games_count")
            .remove("handle_height").remove("handle_width")
            .remove("bar_length").remove("bar_thickness")
            .remove("position").remove("bar_color").remove("bar_opacity")
            .remove("swipe_distance").remove("drawer_width")
            .apply()
    }

    fun register(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        p.registerOnSharedPreferenceChangeListener(listener)

    fun unregister(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        p.unregisterOnSharedPreferenceChangeListener(listener)

    companion object {
        const val SIDE_LEFT = 0
        const val SIDE_RIGHT = 1

        val GAMES_COUNT_RANGE = 1..10
        val HEIGHT_RANGE = 80..400
        val WIDTH_RANGE = 12..64
        val BAR_LENGTH_RANGE = 16..200
        val BAR_THICKNESS_RANGE = 2..12
        val POSITION_RANGE = 0..100
        val OPACITY_RANGE = 0..100
        val SWIPE_RANGE = 10..80
        val DRAWER_RANGE = 240..400

        val DEFAULT_COLOR = 0xFFFFC61A.toInt()
        val COLOR_CHOICES = listOf(
            0xFFFFC61A.toInt(), // gold
            0xFFF2F5FF.toInt(), // white
            0xFF22D3EE.toInt(), // cyan
            0xFF4ADE80.toInt(), // green
            0xFFF472B6.toInt(), // pink
            0xFFFF5252.toInt(), // red
            0xFFA78BFA.toInt(), // purple
        )
    }
}

object SidebarController {
    /** Starts or stops the overlay service to match the saved setting + permission. */
    fun apply(context: Context) {
        val intent = Intent(context, SidebarService::class.java)
        if (SidebarPrefs(context).enabled && Settings.canDrawOverlays(context)) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }
}
