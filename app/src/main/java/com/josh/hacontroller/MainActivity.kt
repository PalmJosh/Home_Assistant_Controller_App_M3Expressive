package com.josh.hacontroller

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.josh.hacontroller.ui.theme.HAControllerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            HAControllerTheme {
                MainApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null && uri.toString().startsWith("homeassistant://auth-callback")) {
                viewModel.handleAuthCallback(uri)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is MainViewModel.AuthState.LoggedIn -> {
                if (navController.currentDestination?.route != "home") {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
            is MainViewModel.AuthState.LoggedOut -> {
                if (navController.currentDestination?.route != "login") {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            }
            else -> { }
        }
    }

    // 1. Safety Net: Surface ensures no white background ever shows
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(navController = navController, startDestination = "login") {

            composable(
                route = "login",
                exitTransition = { fadeOut(animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) }
            ) {
                LoginScreen(viewModel = viewModel)
            }

            composable(
                route = "home",
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                // 2. FIXED: When going to Settings, DON'T move Home. Just fade it out slightly.
                // This keeps it underneath the Settings screen sliding up.
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                },
                // When coming back from Settings, just fade back in.
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300))
                }
            ) {
                HomeScreen(navController = navController, viewModel = viewModel)
            }

            composable(
                route = "settings",
                // 3. FIXED: Settings slides UP over the stationary Home screen
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(400)
                    )
                },
                // Settings slides DOWN revealing the Home screen
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(400)
                    )
                }
            ) {
                SettingsScreen(navController = navController, viewModel = viewModel)
            }
        }
    }
}
