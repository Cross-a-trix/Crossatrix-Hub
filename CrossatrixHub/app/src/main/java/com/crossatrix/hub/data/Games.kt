package com.crossatrix.hub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request
import java.io.IOException

@Serializable
data class Game(
    val name: String,
    val play_count: Long = 0,
    val like_count: Long = 0,
    val thumbnail: String? = null,
    val url: String,
)

object GamesApi {
    private const val URL = "https://crossigames.lovable.app/api/public/top-games"

    /** Returns the top games, or null if the request failed. */
    suspend fun fetch(): List<Game>? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(URL).get().build()
        try {
            Api.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val text = response.body?.string().orEmpty()
                runCatching { Api.json.decodeFromString<List<Game>>(text) }.getOrNull()
            }
        } catch (e: IOException) {
            null
        }
    }
}
