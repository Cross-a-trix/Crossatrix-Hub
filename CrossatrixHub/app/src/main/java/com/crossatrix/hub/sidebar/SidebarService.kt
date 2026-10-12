package com.crossatrix.hub.sidebar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import coil.load
import com.crossatrix.hub.MainActivity
import com.crossatrix.hub.R
import com.crossatrix.hub.data.Game
import com.crossatrix.hub.data.GamesApi
import com.crossatrix.hub.data.SessionStore
import com.crossatrix.hub.ui.HubApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a customizable swipe handle on the screen edge (over every app) and, when swiped,
 * a drawer with Crossi apps, account info, top games and the user's chosen device apps.
 */
class SidebarService : Service() {

    private lateinit var wm: WindowManager
    private val prefs by lazy { SidebarPrefs(this) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var handle: View? = null
    private var drawerRoot: View? = null
    private var drawerPanel: View? = null
    private var gamesBox: LinearLayout? = null
    private var panelWidthPx = 0
    private var onRight = true

    // Rebuild the handle live when a setting changes (gives a live preview in Options).
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != "enabled" && handle != null && drawerRoot == null) addHandle()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAsForeground()
        prefs.register(prefListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this) || !prefs.enabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        removeDrawerNow()
        addHandle()
        return START_STICKY
    }

    override fun onDestroy() {
        prefs.unregister(prefListener)
        scope.cancel()
        removeHandle()
        removeDrawerNow()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- foreground notification

    private fun startAsForeground() {
        val channelId = "sidebar"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "Crossi sidebar", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Crossi sidebar is on")
            .setContentText("Swipe from the screen edge to open it.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(open)
            .build()
        ServiceCompat.startForeground(
            this, 1001, notification,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
    }

    // ---------------------------------------------------------------- edge handle

    private fun addHandle() {
        removeHandle()
        val right = prefs.side == SidebarPrefs.SIDE_RIGHT
        val widthPx = dp(prefs.handleWidth)
        val heightPx = dp(prefs.handleHeight)
        val barLen = min(dp(prefs.barLength), heightPx)
        val barThick = min(dp(prefs.barThickness), widthPx)
        val swipePx = dp(prefs.swipeDistance)

        val alpha = (prefs.barOpacity * 255 / 100).coerceIn(0, 255)
        val barColor = (alpha shl 24) or (prefs.barColor and 0x00FFFFFF)

        val container = FrameLayout(this)
        val pill = View(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = barThick / 2f
                setColor(barColor)
            }
        }
        container.addView(
            pill,
            FrameLayout.LayoutParams(barThick, barLen, (if (right) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL),
        )

        var startX = 0f
        var triggered = false
        container.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = e.rawX
                    triggered = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - startX
                    val inward = if (right) -dx else dx
                    if (!triggered && inward > swipePx) {
                        triggered = true
                        openDrawer()
                    }
                }
            }
            true
        }

        val screenH = resources.displayMetrics.heightPixels
        val yPx = ((screenH - heightPx) * prefs.position / 100f).toInt().coerceAtLeast(0)
        val lp = WindowManager.LayoutParams(
            widthPx, heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or (if (right) Gravity.END else Gravity.START)
            x = 0
            y = yPx
        }
        wm.addView(container, lp)
        handle = container

        // Ask the system not to treat swipes here as the Back gesture (best effort, max ~200dp).
        if (Build.VERSION.SDK_INT >= 29) {
            container.post {
                val exH = min(container.height, dp(200))
                val top = (container.height - exH) / 2
                container.setSystemGestureExclusionRects(listOf(Rect(0, top, container.width, top + exH)))
            }
        }
    }

    private fun removeHandle() {
        handle?.let { runCatching { wm.removeView(it) } }
        handle = null
    }

    // ---------------------------------------------------------------- drawer

    private fun openDrawer() {
        if (drawerRoot != null) return
        onRight = prefs.side == SidebarPrefs.SIDE_RIGHT
        val screenW = resources.displayMetrics.widthPixels
        panelWidthPx = min(dp(prefs.drawerWidth), (screenW * 0.9f).toInt())

        val root = FrameLayout(this)
        val scrim = View(this).apply {
            setBackgroundColor(0x99000000.toInt())
            alpha = 0f
            setOnClickListener { closeDrawer() }
        }
        root.addView(scrim, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val panel = buildPanel()
        root.addView(
            panel,
            FrameLayout.LayoutParams(
                panelWidthPx, ViewGroup.LayoutParams.MATCH_PARENT,
                if (onRight) Gravity.END else Gravity.START,
            ),
        )
        panel.translationX = if (onRight) panelWidthPx.toFloat() else -panelWidthPx.toFloat()

        root.isFocusableInTouchMode = true
        root.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                closeDrawer(); true
            } else false
        }

        val lp = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        wm.addView(root, lp)
        drawerRoot = root
        drawerPanel = panel
        root.requestFocus()

        scrim.animate().alpha(1f).setDuration(180).start()
        panel.animate().translationX(0f).setDuration(220).start()

        gamesBox?.let { loadGames(root, it) }
    }

    private fun closeDrawer() {
        val root = drawerRoot ?: return
        val panel = drawerPanel
        drawerRoot = null
        drawerPanel = null
        gamesBox = null
        if (panel == null) {
            runCatching { wm.removeView(root) }
            return
        }
        panel.animate()
            .translationX(if (onRight) panelWidthPx.toFloat() else -panelWidthPx.toFloat())
            .setDuration(180)
            .withEndAction { runCatching { wm.removeView(root) } }
            .start()
    }

    private fun removeDrawerNow() {
        drawerRoot?.let { runCatching { wm.removeView(it) } }
        drawerRoot = null
        drawerPanel = null
        gamesBox = null
    }

    private fun openInHub(title: String, url: String) {
        closeDrawer()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(MainActivity.EXTRA_OPEN_TITLE, title)
                .putExtra(MainActivity.EXTRA_OPEN_URL, url),
        )
    }

    private fun buildPanel(): View {
        val session = SessionStore(this).load()
        val columns = max(2, (panelWidthPx - dp(32)) / dp(92))
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(16),
                systemDim("status_bar_height", 24) + dp(12),
                dp(16),
                systemDim("navigation_bar_height", 48) + dp(16),
            )
        }

        // Header
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(
            text("Crossi", 22f, GOLD, bold = true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        header.addView(text("✕", 20f, MUTED).apply {
            setPadding(dp(12), dp(4), dp(4), dp(4))
            setOnClickListener { closeDrawer() }
        })
        content.addView(header)

        // Account card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(SURFACE)
            }
        }
        card.addView(text("Account", 11f, MUTED))
        card.addView(text(session?.user?.email ?: "Not signed in", 14f, WHITE, bold = true))
        val balance = session?.let { NumberFormat.getInstance().format(it.croins_balance) + " ¢" } ?: "—"
        card.addView(text("Balance", 11f, MUTED).apply { setPadding(0, dp(8), 0, 0) })
        card.addView(text(balance, 18f, GOLD, bold = true))
        content.addView(
            card,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(16) },
        )

        // Crossi apps
        content.addView(sectionTitle("Crossi apps"))
        content.addView(
            grid(
                HubApps.all.map { app ->
                    cell(app.name, ContextCompat.getDrawable(this, app.icon)) { openInHub(app.name, app.url) }
                },
                columns,
            ),
        )

        // Top games (optional, filled in after the drawer is shown)
        gamesBox = null
        if (prefs.showGames) {
            content.addView(sectionTitle("Top games"))
            val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            content.addView(box)
            gamesBox = box
        }

        // Device apps
        content.addView(sectionTitle("Your apps"))
        val pm = packageManager
        val deviceCells = prefs.apps.mapNotNull { pkg ->
            val launch = pm.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null
            val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                .getOrDefault(pkg)
            val icon = runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
            cell(label, icon) {
                closeDrawer()
                startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        if (deviceCells.isEmpty()) {
            content.addView(text("Pick apps in Crossi Hub › Options › Sidebar apps.", 12f, MUTED))
        } else {
            content.addView(grid(deviceCells, columns))
        }

        return ScrollView(this).apply {
            setBackgroundColor(BG)
            isClickable = true
            isVerticalScrollBarEnabled = false
            addView(content)
        }
    }

    private fun loadGames(root: View, box: LinearLayout) {
        box.addView(text("Loading…", 12f, MUTED))
        scope.launch {
            val games = GamesApi.fetch()
            if (drawerRoot !== root) return@launch // drawer was closed meanwhile
            box.removeAllViews()
            when {
                games == null -> box.addView(text("Couldn't load games.", 12f, MUTED))
                games.isEmpty() -> box.addView(text("No games yet.", 12f, MUTED))
                else -> games.take(prefs.gamesCount).forEach { box.addView(gameRow(it)) }
            }
        }
    }

    private fun gameRow(g: Game): View {
        val nf = NumberFormat.getInstance()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
            setOnClickListener { openInHub(g.name, g.url) }
        }
        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(SURFACE)
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(8).toFloat())
                }
            }
            if (!g.thumbnail.isNullOrBlank()) load(g.thumbnail)
        }
        row.addView(thumb, LinearLayout.LayoutParams(dp(72), dp(54)))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(text(g.name, 13f, WHITE, bold = true).apply {
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        })
        col.addView(text("▶ ${nf.format(g.play_count)}   ♥ ${nf.format(g.like_count)}", 11f, GOLD))
        row.addView(
            col,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) },
        )
        return row
    }

    private fun sectionTitle(title: String) = text(title, 13f, GOLD, bold = true).apply {
        setPadding(0, dp(20), 0, dp(6))
    }

    private fun cell(label: String, icon: Drawable?, onClick: () -> Unit): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(8), dp(4), dp(8))
            setOnClickListener { onClick() }
        }
        val ring = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(2), GOLD)
            }
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }
        val iv = ImageView(this).apply {
            setImageDrawable(icon)
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        ring.addView(iv, FrameLayout.LayoutParams(dp(52), dp(52)))
        col.addView(ring, LinearLayout.LayoutParams(dp(58), dp(58)))
        col.addView(
            text(label, 11f, WHITE).apply {
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, dp(6), 0, 0)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        return col
    }

    private fun grid(cells: List<View>, columns: Int): LinearLayout {
        val g = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cells.chunked(columns).forEach { rowCells ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowCells.forEach { c ->
                row.addView(c, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
            repeat(columns - rowCells.size) {
                row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            }
            g.addView(row)
        }
        return g
    }

    private fun text(s: String, sizeSp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = sizeSp
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun systemDim(name: String, fallbackDp: Int): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(fallbackDp)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private companion object {
        val BG = 0xFF061637.toInt()
        val SURFACE = 0xFF0E234E.toInt()
        val GOLD = 0xFFFFC61A.toInt()
        val WHITE = 0xFFF2F5FF.toInt()
        val MUTED = 0xFFAEB9D6.toInt()
    }
}
