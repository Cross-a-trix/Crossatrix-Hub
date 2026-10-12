package com.crossatrix.hub.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val WEB = "web"
    const val OPTIONS = "options"
    const val APP_PICKER = "app_picker"
}

@Composable
fun AppNavigation(
    pendingOpen: Pair<String, String>? = null,
    onPendingConsumed: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val startDestination = remember {
        if (authViewModel.session != null) Routes.MAIN else Routes.LOGIN
    }

    // Page currently opened in the in-app browser (apps and games share one screen).
    var webTitle by rememberSaveable { mutableStateOf("") }
    var webUrl by rememberSaveable { mutableStateOf("") }

    fun openWeb(title: String, url: String) {
        webTitle = title
        webUrl = url
        navController.navigate(Routes.WEB) { launchSingleTop = true }
    }

    // A page requested from the sidebar overlay.
    LaunchedEffect(pendingOpen) {
        val request = pendingOpen
        if (request != null) {
            if (authViewModel.session != null) openWeb(request.first, request.second)
            onPendingConsumed()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                state = authViewModel.uiState,
                onLogin = { id, pw ->
                    authViewModel.login(id, pw) {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                },
                onInputChanged = authViewModel::clearError,
            )
        }
        composable(Routes.MAIN) {
            val session = authViewModel.session
            if (session != null) {
                RequestNotificationPermission()
                MainScreen(
                    session = session,
                    onOpenUrl = { title, url -> openWeb(title, url) },
                    onOpenOptions = { navController.navigate(Routes.OPTIONS) },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    },
                )
            }
        }
        composable(Routes.OPTIONS) {
            OptionsScreen(
                onBack = { navController.popBackStack() },
                onChooseApps = { navController.navigate(Routes.APP_PICKER) },
            )
        }
        composable(Routes.APP_PICKER) {
            AppPickerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.WEB) {
            if (webUrl.isBlank()) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                // key() reloads the page when a different URL is requested while this screen is open.
                key(webUrl) {
                    WebViewScreen(
                        title = webTitle,
                        url = webUrl,
                        onClose = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
