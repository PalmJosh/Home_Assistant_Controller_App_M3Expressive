package com.josh.hacontroller

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels // Ensure this import exists
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    // Use viewModels() so the instance survives config changes and is ready immediately
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for Cold Start (App launched from link while closed)
        handleIntent(intent)

        setContent {
            AppTheme {
                MainApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Check for Warm Start (App brought to front by link)
        setIntent(intent) // Update the activity's intent property
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            // Check if this is the Home Assistant callback
            if (uri != null && uri.toString().startsWith("homeassistant://auth-callback")) {
                viewModel.handleAuthCallback(uri)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    // Collect the AuthState as a State object so Compose reacts to changes
    val authState by viewModel.authState.collectAsState()

    // Listen for state changes to navigate
    LaunchedEffect(authState) {
        when (authState) {
            is MainViewModel.AuthState.LoggedIn -> {
                // Navigate only if we are not already on home
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
            else -> { /* Loading or Error - stay put or show snackbar */ }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(viewModel = viewModel)
        }
        composable("home") {
            HomeScreen(navController = navController, viewModel = viewModel)
        }
        composable("settings") {
            SettingsScreen(navController = navController, viewModel = viewModel)
        }
    }
}

// --- Keep your existing AppTheme here ---
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}