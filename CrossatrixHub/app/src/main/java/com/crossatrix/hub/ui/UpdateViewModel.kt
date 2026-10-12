package com.crossatrix.hub.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crossatrix.hub.data.UpdateCheck
import com.crossatrix.hub.data.UpdateInfo
import com.crossatrix.hub.data.Updater
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val percent: Int) : UpdateState
    data class Downloaded(val info: UpdateInfo, val file: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    val currentVersion: String = Updater.installedVersion(app)

    var state: UpdateState by mutableStateOf(UpdateState.Idle)
        private set

    fun check() {
        if (state is UpdateState.Checking || state is UpdateState.Downloading) return
        state = UpdateState.Checking
        viewModelScope.launch {
            state = when (val r = Updater.check(currentVersion)) {
                is UpdateCheck.Available -> UpdateState.Available(r.info)
                UpdateCheck.UpToDate -> UpdateState.UpToDate
                is UpdateCheck.Failure -> UpdateState.Error(r.message)
            }
        }
    }

    fun download(info: UpdateInfo) {
        if (state is UpdateState.Downloading) return
        state = UpdateState.Downloading(info, 0)
        viewModelScope.launch {
            state = try {
                val file = Updater.download(getApplication(), info) { pct ->
                    state = UpdateState.Downloading(info, pct)
                }
                UpdateState.Downloaded(info, file)
            } catch (e: Exception) {
                UpdateState.Error(e.message ?: "Download failed.")
            }
        }
    }
}
