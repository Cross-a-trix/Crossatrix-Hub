package com.crossatrix.hub.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crossatrix.hub.data.LoginResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    session: LoginResponse,
    onOpenUrl: (title: String, url: String) -> Unit,
    onOpenOptions: () -> Unit,
    onLogout: () -> Unit,
    gamesViewModel: GamesViewModel = viewModel(),
) {
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Crossatrix Hub") },
                actions = {
                    Box {
                        TextButton(onClick = { menuOpen = true }) {
                            Text("⋮", style = MaterialTheme.typography.titleLarge)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Options") },
                                onClick = { menuOpen = false; onOpenOptions() },
                            )
                            DropdownMenuItem(
                                text = { Text("Log out") },
                                onClick = { menuOpen = false; onLogout() },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        HomeTab(
            session = session,
            gamesViewModel = gamesViewModel,
            onOpenApp = { app -> onOpenUrl(app.name, app.url) },
            onOpenGame = { game -> onOpenUrl(game.name, game.url) },
            modifier = Modifier.padding(padding),
        )
    }
}
