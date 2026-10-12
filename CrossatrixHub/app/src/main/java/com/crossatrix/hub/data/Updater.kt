package com.crossatrix.hub.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Request
import java.io.File
import java.io.IOException

data class UpdateInfo(
    val name: String,
    val version: String,
    val url: String?,
)

sealed interface UpdateCheck {
    data class Available(val info: UpdateInfo) : UpdateCheck
    data object UpToDate : UpdateCheck
    data class Failure(val message: String) : UpdateCheck
}

object Updater {
    const val ENDPOINT = "https://jeedygkznztigosfsvzd.lovable.cloud/functions/v1/files"

    // Version files are named like "v.1.2.0" (also accepts "v1.2.0", "v.1.2.0.apk").
    private val VERSION_RE = Regex("""(?i)(?<![a-z])v\.?\s*(\d+(?:\.\d+)*)""")
    private val NAME_KEYS = listOf("name", "filename", "file_name", "fileName", "title", "path")
    private val URL_KEYS = listOf(
        "url", "download_url", "downloadUrl", "public_url", "publicUrl",
        "signed_url", "signedUrl", "link", "href",
    )

    fun installedVersion(context: Context): String =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0.0.0"

    private fun parseVersion(text: String): List<Int>? =
        VERSION_RE.find(text)?.groupValues?.get(1)?.split(".")?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }

    private fun compare(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    private data class Candidate(val label: String, val version: List<Int>, val url: String?)

    private fun parseEntry(el: JsonElement): Candidate? {
        var name: String? = null
        var url: String? = null
        when (el) {
            is JsonPrimitive -> {
                val s = el.contentOrNull ?: return null
                if (s.startsWith("http")) url = s else name = s
            }
            is JsonObject -> {
                for (k in NAME_KEYS) {
                    val v = (el[k] as? JsonPrimitive)?.contentOrNull
                    if (name == null && v != null) name = v
                }
                for (k in URL_KEYS) {
                    val v = (el[k] as? JsonPrimitive)?.contentOrNull
                    if (url == null && v != null && v.startsWith("http")) url = v
                }
                if (url == null) {
                    for (v in el.values) {
                        val s = (v as? JsonPrimitive)?.contentOrNull
                        if (s != null && s.startsWith("http")) { url = s; break }
                    }
                }
            }
            else -> return null
        }
        val label = name
            ?: url?.substringAfterLast('/')?.substringBefore('?')?.let { Uri.decode(it) }
            ?: return null
        val version = parseVersion(label) ?: return null
        return Candidate(label, version, url)
    }

    suspend fun check(currentVersion: String): UpdateCheck = withContext(Dispatchers.IO) {
        val current = parseVersion("v$currentVersion") ?: listOf(0)
        val request = Request.Builder().url(ENDPOINT).get().build()
        try {
            Api.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheck.Failure("Update server returned HTTP ${response.code}.")
                }
                val body = response.body?.string().orEmpty()
                val root = runCatching { Api.json.parseToJsonElement(body) }.getOrNull()
                    ?: return@withContext UpdateCheck.Failure("Unexpected response from the update server.")
                val list: JsonArray = when (root) {
                    is JsonArray -> root
                    is JsonObject -> (root["files"] as? JsonArray)
                        ?: root.values.filterIsInstance<JsonArray>().firstOrNull()
                        ?: JsonArray(emptyList())
                    else -> JsonArray(emptyList())
                }
                val best = list.mapNotNull { parseEntry(it) }
                    .filter { compare(it.version, current) > 0 }
                    .maxWithOrNull { a, b -> compare(a.version, b.version) }
                if (best == null) UpdateCheck.UpToDate
                else UpdateCheck.Available(UpdateInfo(best.label, best.version.joinToString("."), best.url))
            }
        } catch (e: IOException) {
            UpdateCheck.Failure("Network error. Check your connection.")
        }
    }

    /** Downloads the APK into the app cache and checks it is really an update of this app. */
    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val url = info.url ?: throw IOException("The server gave no download link for ${info.name}.")
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "crossatrix-hub-${info.version}.apk")

        val request = Request.Builder().url(url).get().build()
        Api.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download failed (HTTP ${response.code}).")
            val body = response.body ?: throw IOException("Empty download.")
            val total = body.contentLength()
            var done = 0L
            var lastPct = -1
            body.byteStream().use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) {
                            val pct = (done * 100 / total).toInt()
                            if (pct != lastPct) { lastPct = pct; onProgress(pct) }
                        }
                    }
                }
            }
        }

        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        if (archive == null || archive.packageName != context.packageName) {
            file.delete()
            throw IOException("The downloaded file is not a valid Crossatrix Hub update.")
        }
        file
    }
}
