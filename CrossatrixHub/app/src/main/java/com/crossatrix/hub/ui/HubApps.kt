package com.crossatrix.hub.ui

import androidx.annotation.DrawableRes
import com.crossatrix.hub.R

data class HubApp(
    val id: String,
    val name: String,
    val url: String,
    @DrawableRes val icon: Int,
)

object HubApps {
    val all = listOf(
        HubApp("cross-chat", "Cross Chat", "https://cross-a-chat.lovable.app", R.drawable.app_cross_chat),
        HubApp("crossi-ai", "Crossi AI", "https://cross-a-ai.lovable.app", R.drawable.app_crossi_ai),
        HubApp("crossi-search", "Crossi Search", "https://crossisearch.lovable.app", R.drawable.app_crossi_search),
        HubApp("dashboard", "Crossatrix Dashboard", "https://crossatrix.lovable.app", R.drawable.app_crossatrix_dashboard),
        HubApp("crossi-games", "Crossi Games", "https://crossigames.lovable.app", R.drawable.app_crossi_games),
        HubApp("crosssino", "Crosssino", "https://crosssino.lovable.app", R.drawable.app_crosssino),
    )

    fun byId(id: String?): HubApp? = all.firstOrNull { it.id == id }
}
