package com.josh.hacontroller

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val (userName, serverVersion) = viewModel.serverInfo.collectAsState().value
    val (url, _, _) = viewModel.settingsFlow.collectAsState(initial = Triple("", "", "")).value

    // Daten für Battery Tab
    val sensors by viewModel.sensors.collectAsState()

    // 0 = General, 1 = Batteries
    var selectedTab by remember { mutableIntStateOf(0) }
    val titles = listOf(
        stringResource(R.string.settings_tab_general),
        stringResource(R.string.settings_tab_batteries)
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // --- HEADER (Bleibt immer sichtbar) ---
            Box(Modifier.fillMaxWidth().height(260.dp)) {
                Box(Modifier.offset(100.dp, (-50).dp).size(300.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape))
                Box(Modifier.offset((-100).dp, 50.dp).size(200.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape))

                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.Start) {
                    Surface(
                        onClick = { navController.popBackStack() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(0.5f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowBack, stringResource(R.string.settings_back))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                // Zeigt den ersten Buchstaben des Hausnamens (z.B. "Z" für Zuhause)
                                Text(text = userName.take(1).uppercase(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(text = userName, style = MaterialTheme.typography.headlineLargeEmphasized)

                        }
                    }
                }
            }

            // --- TABS ---
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
                // Keine 'indicator' Definition nötig -> Standard wird verwendet
            ) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    )
                }
            }

            // --- CONTENT ---
            Crossfade(targetState = selectedTab, label = "SettingsTab") { tabIndex ->
                when (tabIndex) {
                    0 -> GeneralSettingsTab(
                        serverVersion = serverVersion,
                        url = url,
                        onLogout = {
                            viewModel.logout()
                            navController.popBackStack()
                        }
                    )
                    1 -> BatterySettingsTab(
                        sensors = sensors
                    )
                }
            }
        }
    }
}

// --- TAB 1: GENERAL (Dein alter Code) ---
@Composable
fun GeneralSettingsTab(
    serverVersion: String,
    url: String,
    onLogout: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Icons.Rounded.Dns, stringResource(R.string.settings_core), serverVersion.ifEmpty { stringResource(R.string.settings_not_available) }, Modifier.weight(1f))
            StatCard(Icons.Rounded.Link, stringResource(R.string.settings_server), stringResource(R.string.settings_online), Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        Text(stringResource(R.string.settings_connection), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.settings_instance_url), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(url, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Logout, null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.settings_disconnect), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- TAB 2: BATTERIES (Neu) ---
@Composable
fun BatterySettingsTab(sensors: List<HaEntity>) {
    // Filtern nach device_class: battery und Sortieren (Niedrigste zuerst)
    val batterySensors = remember(sensors) {
        sensors.filter {
            it.attributes.deviceClass == "battery"
        }.sortedBy {
            it.state.toFloatOrNull() ?: 100f
        }
    }

    if (batterySensors.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.BatteryUnknown, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.settings_no_batteries), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(stringResource(R.string.settings_device_status), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(batterySensors) { sensor ->
                BatteryRow(sensor)
            }
        }
    }
}

@Composable
fun BatteryRow(sensor: HaEntity) {
    val level = sensor.state.toFloatOrNull() ?: 0f

    // Farblogik
    val color = when {
        level <= 20 -> MaterialTheme.colorScheme.error
        level <= 40 -> Color(0xFFFFA000) // Orange
        else -> MaterialTheme.colorScheme.primary
    }

    val icon = when {
        level <= 20 -> Icons.Rounded.BatteryAlert
        level >= 95 -> Icons.Rounded.BatteryFull
        else -> Icons.Rounded.BatteryStd
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().height(80.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sensor.attributes.friendlyName ?: stringResource(R.string.settings_unknown_device),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { level / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Spacer(Modifier.width(16.dp))

            Text(
                stringResource(R.string.settings_battery_level, level.toInt()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(20.dp), modifier = modifier.height(100.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}
