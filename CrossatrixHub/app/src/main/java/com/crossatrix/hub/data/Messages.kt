package com.crossatrix.hub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.IOException

data class HubMessage(
    val message: String,
    val sender: String,
    /** Raw JSON of the object, used to detect already-notified messages. */
    val key: String,
)

object MessageApi {
    private const val URL = "https://messages.julian-grohmann6.workers.dev/"

    /** Returns the messages, or null if the request failed. */
    suspend fun fetch(userId: String): List<HubMessage>? = withContext(Dispatchers.IO) {
        val url = URL.toHttpUrl().newBuilder().addQueryParameter("key", userId).build()
        val request = Request.Builder().url(url).get().build()
        try {
            Api.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                parse(response.body?.string().orEmpty())
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Accepts a single object, an array of objects, or {"messages":[...]}.
     * Each object looks like {"message":"...","sender":"..."}.
     */
    internal fun parse(text: String): List<HubMessage>? {
        if (text.isBlank()) return emptyList()
        val root = runCatching { Api.json.parseToJsonElement(text) }.getOrNull() ?: return null
        val items: List<JsonElement> = when (root) {
            is JsonArray -> root
            is JsonObject -> when {
                root["messages"] is JsonArray -> root["messages"] as JsonArray
                root.containsKey("message") -> listOf(root)
                else -> emptyList()
            }
            else -> emptyList()
        }
        return items.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val msg = (obj["message"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val sender = (obj["sender"] as? JsonPrimitive)?.contentOrNull ?: "Crossatrix"
            HubMessage(msg, sender, obj.toString())
        }
    }
}
