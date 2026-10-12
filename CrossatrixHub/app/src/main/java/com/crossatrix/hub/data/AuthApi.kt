package com.crossatrix.hub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object Api {
    const val BASE_URL = "https://digjxtmzafzcgytgcwmb.supabase.co/functions/v1"

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
}

class AuthApi {

    suspend fun login(identifier: String, password: String): LoginResult =
        withContext(Dispatchers.IO) {
            // The API accepts "email", "username" or "loginname".
            val key = if (identifier.contains("@")) "email" else "username"
            val body = JsonObject(
                mapOf(
                    key to JsonPrimitive(identifier.trim()),
                    "password" to JsonPrimitive(password),
                )
            ).toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${Api.BASE_URL}/crossatrix-login")
                .post(body)
                .build()

            try {
                Api.client.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    when {
                        response.isSuccessful -> {
                            try {
                                LoginResult.Success(Api.json.decodeFromString<LoginResponse>(text))
                            } catch (e: Exception) {
                                LoginResult.Failure("Unexpected server response.")
                            }
                        }
                        response.code == 423 -> LoginResult.Locked
                        else -> {
                            val err = runCatching {
                                Api.json.decodeFromString<ErrorResponse>(text)
                            }.getOrNull()
                            val msg = when (response.code) {
                                401 -> "Invalid credentials."
                                404 -> "Service is currently unavailable."
                                else -> err?.error ?: "Login failed (HTTP ${response.code})."
                            }
                            LoginResult.Failure(msg)
                        }
                    }
                }
            } catch (e: IOException) {
                LoginResult.Failure("Network error. Check your connection.")
            }
        }
}
