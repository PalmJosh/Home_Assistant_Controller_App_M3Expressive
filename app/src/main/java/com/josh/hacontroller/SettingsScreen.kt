package com.josh.hacontroller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    // Collect the new Server Info State
    val (userName, serverVersion) = viewModel.serverInfo.collectAsState().value
    val (url, token) = viewModel.settingsFlow.collectAsState(initial = Pair("", "")).value

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            // --- NEW: SERVER INFO CARD ---
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Connected as", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Home Assistant $serverVersion")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Connection Details", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Read-Only Fields (Since we use OAuth now)
            OutlinedTextField(
                value = url,
                onValueChange = {},
                readOnly = true,
                label = { Text("Server URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = {
                    viewModel.logout()
                    navController.popBackStack()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Disconnect & Logout")
            }
        }
    }
}