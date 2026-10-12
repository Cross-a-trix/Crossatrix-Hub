package com.crossatrix.hub.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crossatrix.hub.data.AuthApi
import com.crossatrix.hub.data.LoginResponse
import com.crossatrix.hub.data.LoginResult
import com.crossatrix.hub.data.SessionStore
import com.crossatrix.hub.notify.MessageScheduler
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SessionStore(app)
    private val api = AuthApi()

    /** Current session, or null if logged out / expired. */
    var session: LoginResponse? by mutableStateOf(loadValidSession())
        private set

    var uiState by mutableStateOf(LoginUiState())
        private set

    private fun loadValidSession(): LoginResponse? {
        val s = store.load() ?: return null
        val expiresMs = s.expires_at * 1000
        return if (expiresMs > System.currentTimeMillis()) s else {
            store.clear()
            null
        }
    }

    fun login(identifier: String, password: String, onSuccess: () -> Unit) {
        if (identifier.isBlank() || password.isEmpty()) {
            uiState = LoginUiState(error = "Enter your email/username and password.")
            return
        }
        uiState = LoginUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = api.login(identifier, password)) {
                is LoginResult.Success -> {
                    store.save(result.session)
                    session = result.session
                    MessageScheduler.start(getApplication())
                    uiState = LoginUiState()
                    onSuccess()
                }
                LoginResult.Locked ->
                    uiState = LoginUiState(error = "This account is currently locked.")
                is LoginResult.Failure ->
                    uiState = LoginUiState(error = result.message)
            }
        }
    }

    fun logout() {
        MessageScheduler.stop(getApplication())
        store.clear()
        session = null
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}
