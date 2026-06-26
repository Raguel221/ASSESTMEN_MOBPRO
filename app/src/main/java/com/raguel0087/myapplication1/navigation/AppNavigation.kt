package com.raguel0087.myapplication1.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.raguel0087.myapplication1.ui.LoginScreen
import com.raguel0087.myapplication1.ui.MainScreen
import com.raguel0087.myapplication1.viewmodel.AuthViewModel
import com.raguel0087.myapplication1.viewmodel.MainViewModel

private const val ROUTE_LOGIN = "login"
private const val ROUTE_MAIN = "main"

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    // Determine the start destination based on existing session
    val startDestination = if (authState.currentUser != null) ROUTE_MAIN else ROUTE_LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(ROUTE_LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(ROUTE_MAIN) {
                        popUpTo(ROUTE_LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_MAIN) {
            MainScreen(
                mainViewModel = mainViewModel,
                authViewModel = authViewModel,
                onLogout = {
                    navController.navigate(ROUTE_LOGIN) {
                        popUpTo(ROUTE_MAIN) { inclusive = true }
                    }
                }
            )
        }
    }
}
