package com.crossatrix.hub.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Stores the login session (incl. tokens) encrypted on the device. */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "crossatrix_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun save(session: LoginResponse) {
        prefs.edit().putString(KEY, Api.json.encodeToString(LoginResponse.serializer(), session)).apply()
    }

    fun load(): LoginResponse? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching { Api.json.decodeFromString<LoginResponse>(raw) }.getOrNull()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "session"
    }
}
