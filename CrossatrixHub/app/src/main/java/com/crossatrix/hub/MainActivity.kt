package com.crossatrix.hub

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crossatrix.hub.data.SessionStore
import com.crossatrix.hub.notify.MessageScheduler
import com.crossatrix.hub.sidebar.SidebarController
import com.crossatrix.hub.ui.AppNavigation
import com.crossatrix.hub.ui.theme.CrossatrixTheme

class MainActivity : ComponentActivity() {

    /** Page requested from outside (e.g. the sidebar): title to url. */
    private var pendingOpen by mutableStateOf<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark background -> always use light system bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        handleIntent(intent)
        setContent {
            CrossatrixTheme {
                AppNavigation(
                    pendingOpen = pendingOpen,
                    onPendingConsumed = { pendingOpen = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val url = intent?.getStringExtra(EXTRA_OPEN_URL)
        if (!url.isNullOrBlank()) {
            pendingOpen = (intent.getStringExtra(EXTRA_OPEN_TITLE) ?: "") to url
            intent.removeExtra(EXTRA_OPEN_URL)
            intent.removeExtra(EXTRA_OPEN_TITLE)
        }
    }

    override fun onStart() {
        super.onStart()
        // Check for messages every time the app is opened, and keep the 15-min check scheduled.
        if (SessionStore(this).load() != null) MessageScheduler.start(this)
        // Keep the Crossi sidebar in sync with its setting.
        SidebarController.apply(this)
    }

    companion object {
        const val EXTRA_OPEN_TITLE = "open_title"
        const val EXTRA_OPEN_URL = "open_url"
    }
}
