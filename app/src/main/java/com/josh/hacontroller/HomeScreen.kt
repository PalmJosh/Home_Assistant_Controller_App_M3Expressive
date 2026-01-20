package com.josh.hacontroller

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

// Enum to manage Bottom Sheet Content
enum class SheetContent {
    NONE, ROOM_DETAILS, LIGHT_SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val lights by viewModel.lights.collectAsState()
    val areas by viewModel.areas.collectAsState()
    val selectedAreaId by viewModel.selectedArea.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState(initial = Pair("", ""))

    // 0 = Rooms (Default), 1 = Devices
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(settings) {
        val (baseUrl, token) = settings
        if (baseUrl.isNotEmpty() && token.isNotEmpty()) {
            viewModel.initConnection(baseUrl, token)
        }
    }

    // Sheet State
    val sheetState = rememberModalBottomSheetState()
    var currentSheet by remember { mutableStateOf(SheetContent.NONE) }
    var selectedLight by remember { mutableStateOf<HaEntity?>(null) }
    var selectedRoom by remember { mutableStateOf<HaArea?>(null) }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text(if (selectedTab == 0) "My Home" else "All Devices") },
                    actions = {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )

                // Chips only visible on Devices Tab
                if (selectedTab == 1 && areas.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedAreaId == null,
                                onClick = { viewModel.selectArea(null) },
                                label = { Text("All") },
                                leadingIcon = if (selectedAreaId == null) { { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) } } else null
                            )
                        }
                        items(areas) { area ->
                            val isSelected = area.areaId == selectedAreaId
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectArea(area.areaId) },
                                label = { Text(area.name) },
                                leadingIcon = if (isSelected) { { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) } } else null
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Apartment, contentDescription = null) },
                    label = { Text("Rooms") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Devices, contentDescription = null) },
                    label = { Text("Devices") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {

            if (selectedTab == 0) {
                // --- ROOMS TAB ---
                if (areas.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No rooms found.", textAlign = TextAlign.Center)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(areas, key = { it.areaId }) { area ->
                            val lightsInRoom = lights.filter { it.areaId == area.areaId }
                            val isRoomActive = lightsInRoom.any { it.state == "on" }
                            val activeCount = lightsInRoom.count { it.state == "on" }

                            RoomCard(
                                area = area,
                                isActive = isRoomActive,
                                activeCount = activeCount,
                                totalCount = lightsInRoom.size,
                                onToggle = { viewModel.toggleArea(area, settings.second) },
                                onLongPress = {
                                    selectedRoom = area
                                    currentSheet = SheetContent.ROOM_DETAILS
                                }
                            )
                        }
                    }
                }
            } else {
                // --- DEVICES TAB ---
                val visibleLights = remember(lights, selectedAreaId) {
                    if (selectedAreaId == null) lights else lights.filter { it.areaId == selectedAreaId }
                }

                if (visibleLights.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No lights found.", textAlign = TextAlign.Center)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visibleLights, key = { it.entityId }) { light ->
                            LightCard(
                                entity = light,
                                onToggle = { viewModel.toggleLight(light, settings.second) },
                                onLongPress = {
                                    selectedLight = light
                                    currentSheet = SheetContent.LIGHT_SETTINGS
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- BOTTOM SHEET ---
        if (currentSheet != SheetContent.NONE) {
            ModalBottomSheet(
                onDismissRequest = { currentSheet = SheetContent.NONE },
                sheetState = sheetState
            ) {
                when (currentSheet) {
                    // 1. Room Details with Master Slider
                    SheetContent.ROOM_DETAILS -> {
                        selectedRoom?.let { room ->
                            val roomLights = lights.filter { it.areaId == room.areaId }

                            // Calculate avg brightness for slider start
                            val activeLights = roomLights.filter { it.state == "on" && it.attributes.brightness != null }
                            val avgBri = if (activeLights.isNotEmpty()) activeLights.map { it.attributes.brightness!! }.average().toFloat() else 0f
                            var sliderVal by remember { mutableFloatStateOf(avgBri) }

                            Column(Modifier.padding(bottom = 24.dp)) {
                                Text(room.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

                                // Master Slider
                                if (roomLights.isNotEmpty()) {
                                    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.WbSunny, null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Room Brightness", style = MaterialTheme.typography.labelLarge)
                                        }
                                        Slider(
                                            value = sliderVal,
                                            onValueChange = { sliderVal = it },
                                            onValueChangeFinished = {
                                                viewModel.setAreaBrightness(room, sliderVal.toInt(), settings.second)
                                            },
                                            valueRange = 0f..255f
                                        )
                                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                    }
                                }

                                if (roomLights.isEmpty()) {
                                    Text("No lights here", Modifier.padding(24.dp))
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 150.dp),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(roomLights, key = { it.entityId }) { light ->
                                            LightCard(
                                                entity = light,
                                                onToggle = { viewModel.toggleLight(light, settings.second) },
                                                onLongPress = {
                                                    selectedLight = light
                                                    currentSheet = SheetContent.LIGHT_SETTINGS
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Individual Light Settings
                    SheetContent.LIGHT_SETTINGS -> {
                        selectedLight?.let { light ->
                            LightControlSheet(
                                entity = light,
                                onUpdate = { bri, r, g, b ->
                                    viewModel.updateLightState(light.entityId, bri, listOf(r, g, b), settings.second)
                                }
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LightCard(entity: HaEntity, modifier: Modifier = Modifier, onToggle: () -> Unit, onLongPress: () -> Unit) {
    val isOn = entity.state == "on"
    val bg = if (isOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val haptics = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.height(160.dp).clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onToggle() }, onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress() }),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Column(Modifier.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Lightbulb, null, tint = fg, modifier = Modifier.size(32.dp))
                Text(entity.attributes.friendlyName ?: "Unknown", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = fg, maxLines = 2)
                Text(if (isOn) "ON" else "OFF", style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.7f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoomCard(area: HaArea, isActive: Boolean, activeCount: Int, totalCount: Int, onToggle: () -> Unit, onLongPress: () -> Unit) {
    val bg = if (isActive) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isActive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val haptics = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.height(140.dp).clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onToggle() }, onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress() }),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Icon(Icons.Rounded.Apartment, null, tint = fg, modifier = Modifier.size(32.dp).align(Alignment.TopEnd))
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(area.name, style = MaterialTheme.typography.headlineSmall, color = fg, fontWeight = FontWeight.Bold)
                Text(if (totalCount > 0) "$activeCount / $totalCount Lights On" else "No Lights", style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.8f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LightControlSheet(entity: HaEntity, onUpdate: (Int, Int, Int, Int) -> Unit) {
    // 1. Initialize State
    var brightness by remember { mutableFloatStateOf(entity.attributes.brightness?.toFloat() ?: 128f) }

    // Store the ACTUAL RGB values currently active.
    // Default to existing color, or White (255,255,255) if null.
    var currentRed by remember { mutableIntStateOf(entity.attributes.rgbColor?.getOrElse(0) { 255 } ?: 255) }
    var currentGreen by remember { mutableIntStateOf(entity.attributes.rgbColor?.getOrElse(1) { 255 } ?: 255) }
    var currentBlue by remember { mutableIntStateOf(entity.attributes.rgbColor?.getOrElse(2) { 255 } ?: 255) }

    // Only used for the visual slider position, doesn't dictate color logic anymore
    var currentHue by remember { mutableFloatStateOf(0f) }

    // Logic: Hue Slider Moved
    fun updateFromHue(hue: Float) {
        currentHue = hue
        val hsv = floatArrayOf(hue, 1f, 1f)
        val colorInt = android.graphics.Color.HSVToColor(hsv)

        // Update our state
        currentRed = android.graphics.Color.red(colorInt)
        currentGreen = android.graphics.Color.green(colorInt)
        currentBlue = android.graphics.Color.blue(colorInt)

        // Send Command
        onUpdate(brightness.toInt(), currentRed, currentGreen, currentBlue)
    }

    // Logic: Preset Tapped
    fun setPreset(r: Int, g: Int, b: Int) {
        // Update our state
        currentRed = r
        currentGreen = g
        currentBlue = b

        // Send Command
        onUpdate(brightness.toInt(), currentRed, currentGreen, currentBlue)
    }

    Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
        Text(
            text = entity.attributes.friendlyName ?: "Light Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- BRIGHTNESS ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WbSunny, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Brightness", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    onValueChangeFinished = {
                        // FIX: Send the SAVED (current) Red/Green/Blue, not the Hue
                        onUpdate(brightness.toInt(), currentRed, currentGreen, currentBlue)
                    },
                    valueRange = 0f..255f
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- COLOR SLIDER ---
        Text("Color", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Box(contentAlignment = Alignment.Center) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                        )
                    )
            )
            Slider(
                value = currentHue,
                onValueChange = { updateFromHue(it) },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- WHITE PRESETS ---
        Text("Temperature Presets", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            data class Preset(val name: String, val color: Color, val r: Int, val g: Int, val b: Int)

            val whitePresets = listOf(
                Preset("Cool", Color(0xFFD4EBFF), 212, 235, 255),
                Preset("Neutral", Color(0xFFFFFFFF), 255, 255, 255),
                Preset("Warm", Color(0xFFFFD479), 255, 212, 121),
                Preset("Extra Warm", Color(0xFFFF9329), 255, 147, 41)
            )

            items(whitePresets) { preset ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(preset.color)
                            .combinedClickable(onClick = { setPreset(preset.r, preset.g, preset.b) })
                            .border(1.dp, Color.LightGray, CircleShape)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(preset.name, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}