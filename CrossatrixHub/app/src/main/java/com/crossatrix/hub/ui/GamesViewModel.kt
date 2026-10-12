package com.crossatrix.hub.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossatrix.hub.data.Game
import com.crossatrix.hub.data.GamesApi
import kotlinx.coroutines.launch

sealed interface GamesState {
    data object Loading : GamesState
    data object Error : GamesState
    data class Success(val games: List<Game>) : GamesState
}

class GamesViewModel : ViewModel() {
    var state: GamesState by mutableStateOf(GamesState.Loading)
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) isRefreshing = true else state = GamesState.Loading
            val result = GamesApi.fetch()
            when {
                result != null -> state = GamesState.Success(result)
                // keep showing old data if a refresh fails
                state is GamesState.Success -> Unit
                else -> state = GamesState.Error
            }
            isRefreshing = false
        }
    }
}
