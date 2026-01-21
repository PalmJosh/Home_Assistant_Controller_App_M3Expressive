package com.josh.hacontroller

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

// --- ANIMATION SPECS ---
val ExpressiveSpring = spring<Dp>(dampingRatio = 0.8f, stiffness = 350f)

enum class SheetContent {
    NONE, ROOM_DETAILS, LIGHT_SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val lights by viewModel.lights.collectAsState()
    val sensors by viewModel.sensors.collectAsState()
    val areas by viewModel.areas.collectAsState()
    val selectedAreaId by viewModel.selectedArea.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState(initial = Triple("", "", ""))
    val haptics = LocalHapticFeedback.current

    // 0 = Rooms, 1 = Devices, 2 = Sensors
    var selectedTab by remember { mutableIntStateOf(0) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentSheet by remember { mutableStateOf(SheetContent.NONE) }
    var selectedLight by remember { mutableStateOf<HaEntity?>(null) }
    var selectedRoom by remember { mutableStateOf<HaArea?>(null) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when(selectedTab) {
                            0 -> stringResource(R.string.home_title)
                            1 -> stringResource(R.string.devices_title)
                            else -> stringResource(R.string.tab_sensors)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    ExpressiveSettingsButton(onClick = { navController.navigate("settings") })
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            val haptics = LocalHapticFeedback.current
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Apartment, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_rooms)) },
                    selected = selectedTab == 0,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 0
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Light, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_devices)) },
                    selected = selectedTab == 1,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 1
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Sensors, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_sensors)) },
                    selected = selectedTab == 2,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 2
                    }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Chips für Areas (sichtbar bei Tab 1 und 2)
            if (selectedTab != 0 && areas.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    item {
                        FilterChip(
                            selected = selectedAreaId == null,
                            onClick = { viewModel.selectArea(null) },
                            label = { Text(stringResource(R.string.filter_all), style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (selectedAreaId == null) { { Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) } } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    items(areas) { area ->
                        val isSelected = area.areaId == selectedAreaId
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectArea(area.areaId) },
                            label = { Text(area.name, style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (isSelected) { { Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) } } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        if (areas.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_rooms_found), textAlign = TextAlign.Center)
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

                                    ExpressiveRoomCard(
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
                    }
                    1 -> {
                        val visibleLights = remember(lights, selectedAreaId) {
                            if (selectedAreaId == null) lights else lights.filter { it.areaId == selectedAreaId }
                        }

                        if (visibleLights.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_lights_found), textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(visibleLights, key = { it.entityId }) { light ->
                                    ExpressiveLightCard(
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
                    2 -> {
                        val visibleSensors = remember(sensors, selectedAreaId) {
                            val areaFiltered = if (selectedAreaId == null) sensors else sensors.filter { it.areaId == selectedAreaId }
                            areaFiltered.filter { it.attributes.deviceClass != "battery" }
                        }

                        if (visibleSensors.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_sensors_found), textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(visibleSensors, key = { it.entityId }) { sensor ->
                                    ExpressiveSensorCard(entity = sensor)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (currentSheet != SheetContent.NONE) {
            ModalBottomSheet(
                onDismissRequest = { currentSheet = SheetContent.NONE },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                when (currentSheet) {
                    SheetContent.ROOM_DETAILS -> {
                        selectedRoom?.let { room ->
                            val roomLights = lights.filter { it.areaId == room.areaId }
                            val activeLights = roomLights.filter { it.state == "on" && it.attributes.brightness != null }
                            val avgBri = if (activeLights.isNotEmpty()) activeLights.map { it.attributes.brightness!! }.average().toFloat() else 0f
                            var sliderVal by remember { mutableFloatStateOf(avgBri) }

                            Column(Modifier.padding(bottom = 48.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Apartment, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(16.dp))
                                    Text(room.name, style = MaterialTheme.typography.headlineMediumEmphasized)
                                }

                                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                                if (roomLights.isNotEmpty()) {
                                    Column(Modifier.padding(horizontal = 24.dp)) {
                                        Text(stringResource(R.string.master_brightness), style = MaterialTheme.typography.labelMedium)
                                        Spacer(Modifier.height(12.dp))
                                        Slider(
                                            value = sliderVal,
                                            onValueChange = { sliderVal = it },
                                            onValueChangeFinished = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.setAreaBrightness(room, sliderVal.toInt(), settings.second)
                                            },
                                            valueRange = 0f..255f
                                        )
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                                if (roomLights.isNotEmpty()) {
                                    Text(stringResource(R.string.devices_list_title), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 24.dp))
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 140.dp),
                                        contentPadding = PaddingValues(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.heightIn(max = 400.dp)
                                    ) {
                                        items(roomLights, key = { it.entityId }) { light ->
                                            ExpressiveLightCard(
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveSensorCard(
    entity: HaEntity,
    modifier: Modifier = Modifier
) {
    val deviceClass = entity.attributes.deviceClass
    val isBinary = entity.entityId.startsWith("binary_sensor")
    val isActive = entity.state == "on"
    val stateValue = entity.state

    val icon = when (deviceClass) {
        "temperature" -> Icons.Outlined.Thermostat
        "humidity" -> Icons.Outlined.WaterDrop
        "pressure" -> Icons.Outlined.Compress
        "illuminance" -> Icons.Outlined.WbSunny
        "battery" -> Icons.Outlined.BatteryStd
        "power", "energy" -> Icons.Outlined.Bolt
        "motion" -> if (isActive) Icons.Outlined.RunCircle else Icons.Outlined.DirectionsWalk
        "occupancy", "presence" -> if (isActive) Icons.Outlined.Person else Icons.Outlined.PersonOutline
        "door" , "opening" -> if (isActive) Icons.Outlined.MeetingRoom else Icons.Outlined.DoorBack
        "garage_door" -> if (isActive) Icons.Outlined.Garage else Icons.Outlined.DoorSliding
        "window" -> Icons.Outlined.CropSquare
        "lock" -> if (isActive) Icons.Outlined.LockOpen else Icons.Outlined.Lock
        "smoke", "gas" -> Icons.Outlined.SmokeFree
        "moisture" -> Icons.Outlined.WaterDamage
        "vibration" -> Icons.Outlined.Vibration
        "safety", "problem" -> Icons.Outlined.Warning
        else -> Icons.Outlined.Sensors
    }

    val isLowBattery = deviceClass == "battery" && !isBinary && (stateValue.toFloatOrNull() ?: 100f) < 15f
    val useErrorColor = (isBinary && isActive) || isLowBattery

    val containerColor = if (useErrorColor) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (useErrorColor) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer

    val statusLabel = if (isBinary) {
        when (deviceClass) {
            "door", "garage_door", "window", "opening" -> if (isActive) stringResource(R.string.state_open) else stringResource(R.string.state_closed)
            "lock" -> if (isActive) stringResource(R.string.state_unlocked) else stringResource(R.string.state_locked)
            "moisture" -> if (isActive) stringResource(R.string.state_wet) else stringResource(R.string.state_dry)
            "smoke", "gas", "safety", "problem" -> if (isActive) stringResource(R.string.state_alert) else stringResource(R.string.state_ok)
            "battery" -> if (isActive) stringResource(R.string.state_low) else stringResource(R.string.state_ok)
            "plug", "connectivity" -> if (isActive) stringResource(R.string.state_connected) else stringResource(R.string.state_disconnected)
            else -> if (isActive) stringResource(R.string.state_detected) else stringResource(R.string.state_clear)
        }
    } else ""

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
            )

            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = entity.attributes.friendlyName ?: stringResource(R.string.sensor_default_name),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 1
                )

                Spacer(Modifier.height(4.dp))

                if (isBinary) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = stateValue,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = entity.attributes.unitOfMeasurement ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveSettingsButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(if (isPressed) 12.dp else 50.dp, ExpressiveSpring)

    FilledIconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(cornerRadius),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveLightCard(
    entity: HaEntity,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val isOn = entity.state == "on"
    val haptics = LocalHapticFeedback.current

    val statusText = if (isOn) {
        val bri = entity.attributes.brightness
        if (bri != null) {
            stringResource(R.string.brightness_percent, (bri / 255f * 100).toInt())
        } else {
            stringResource(R.string.status_on)
        }
    } else {
        stringResource(R.string.status_off)
    }

    val cornerRadius by animateDpAsState(if (isOn) 32.dp else 16.dp, spring(dampingRatio = 0.8f, stiffness = 350f))
    val containerColor by animateColorAsState(if (isOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    val contentColor by animateColorAsState(if (isOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)

    Card(
        shape = RoundedCornerShape(cornerRadius),
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .combinedClickable(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLongPress()
                }
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val scale by animateFloatAsState(if(isOn) 1.1f else 1.0f, spring(dampingRatio = 0.8f, stiffness = 350f))

                Icon(Icons.Outlined.Lightbulb, null, tint = contentColor, modifier = Modifier.size(36.dp).scale(scale))
                Spacer(Modifier.height(8.dp))
                Text(
                    entity.attributes.friendlyName ?: stringResource(R.string.unknown_device_name),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
            Text(statusText, style = MaterialTheme.typography.labelSmallEmphasized, color = contentColor.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.BottomStart))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveRoomCard(
    area: HaArea,
    isActive: Boolean,
    activeCount: Int,
    totalCount: Int,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val cornerRadius by animateDpAsState(if (isActive) 24.dp else 12.dp, ExpressiveSpring)
    val bgColor by animateColorAsState(if (isActive) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    val fgColor by animateColorAsState(if (isActive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)

    Card(
        shape = RoundedCornerShape(cornerRadius),
        modifier = Modifier
            .height(110.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .combinedClickable(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLongPress()
                }
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Icon(Icons.Outlined.Apartment, null, tint = fgColor, modifier = Modifier.size(28.dp).align(Alignment.TopEnd))
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(area.name, style = MaterialTheme.typography.labelLargeEmphasized, color = fgColor)
                Text(if (totalCount > 0) stringResource(R.string.room_status_on_count, activeCount, totalCount) else stringResource(R.string.room_status_empty), style = MaterialTheme.typography.labelSmallEmphasized, color = fgColor.copy(alpha = 0.8f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LightControlSheet(entity: HaEntity, onUpdate: (Int, Int, Int, Int) -> Unit) {
    var brightness by remember { mutableFloatStateOf(entity.attributes.brightness?.toFloat() ?: 128f) }
    var currentRed by remember { mutableIntStateOf(entity.attributes.rgbColor?.getOrElse(0) { 255 } ?: 255) }
    var currentGreen by remember { mutableIntStateOf(entity.attributes.rgbColor?.getOrElse(1) { 255 } ?: 255) }
    var currentBlue by remember { mutableIntStateOf(entity.attributes.rgbColor?.getOrElse(2) { 255 } ?: 255) }
    var currentHue by remember { mutableFloatStateOf(0f) }

    val haptics = LocalHapticFeedback.current

    fun updateFromHue(hue: Float) {
        currentHue = hue
        val hsv = floatArrayOf(hue, 1f, 1f)
        val colorInt = android.graphics.Color.HSVToColor(hsv)
        currentRed = android.graphics.Color.red(colorInt)
        currentGreen = android.graphics.Color.green(colorInt)
        currentBlue = android.graphics.Color.blue(colorInt)
        onUpdate(brightness.toInt(), currentRed, currentGreen, currentBlue)
    }

    fun setPreset(r: Int, g: Int, b: Int) {
        currentRed = r; currentGreen = g; currentBlue = b
        onUpdate(brightness.toInt(), currentRed, currentGreen, currentBlue)
    }

    Column(modifier = Modifier.padding(bottom = 48.dp).navigationBarsPadding()) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Lightbulb, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(entity.attributes.friendlyName ?: stringResource(R.string.light_settings_title), style = MaterialTheme.typography.headlineMediumEmphasized)
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(stringResource(R.string.brightness_label), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(12.dp))
            Slider(
                value = brightness,
                onValueChange = { brightness = it },
                onValueChangeFinished = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onUpdate(brightness.toInt(), currentRed, currentGreen, currentBlue) },
                valueRange = 0f..255f,
                track = { sliderState -> SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(24.dp), thumbTrackGapSize = 0.dp) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(stringResource(R.string.color_label), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(12.dp))
            Box(contentAlignment = Alignment.Center) {
                Spacer(
                    modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                )
                Slider(
                    value = currentHue,
                    onValueChange = { updateFromHue(it)
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                      },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent),
                    track = { sliderState -> SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(24.dp), colors = SliderDefaults.colors(activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent), thumbTrackGapSize = 0.dp) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(stringResource(R.string.temperature_presets_label), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()) {
                data class Preset(val name: String, val color: Color, val r: Int, val g: Int, val b: Int)
                val whitePresets = listOf(
                    Preset("2000K", Color(0xFFFF9329), 255, 150, 50),
                    Preset("2700K", Color(0xFFFFD479), 255, 170, 80),
                    Preset("3000K", Color(0xFFFFD479), 255, 190, 130),
                    Preset("5000K", Color(0xFFFFFFFF), 255, 228, 205),
                    Preset("6000K", Color(0xFFD4EBFF), 255, 255, 255)
                )
                items(whitePresets) { preset ->
                    val isActive = currentRed == preset.r && currentGreen == preset.g && currentBlue == preset.b
                    ExpressivePresetButton(name = preset.name, color = preset.color, isActive = isActive) { setPreset(preset.r, preset.g, preset.b) }
                }
            }
        }
    }
}

@Composable
fun ExpressivePresetButton(name: String, color: Color, isActive: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    // Animation States
    val cornerPercent by animateIntAsState(
        targetValue = if (isActive) 50 else 20,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1.0f, // Etwas stärkerer Effekt
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                // WICHTIG: graphicsLayer für saubere Skalierung ohne Layout-Verschiebung
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    // Der Schatten wird auch animiert
                    shadowElevation = if (isActive) 8.dp.toPx() else 0f
                    shape = RoundedCornerShape(cornerPercent)
                    clip = true
                }
                .background(color)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        // Leichtes "Tick" beim Farbwechsel
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                )
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}
