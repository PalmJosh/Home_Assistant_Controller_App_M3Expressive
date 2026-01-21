package com.josh.hacontroller

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val android.content.Context.dataStore by preferencesDataStore(name = "settings")

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val context = app.applicationContext
    private val gson = Gson()
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    private val URL_KEY = stringPreferencesKey("base_url")
    private val TOKEN_KEY = stringPreferencesKey("auth_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")

    private val _lights = MutableStateFlow<List<HaEntity>>(emptyList())
    val lights = _lights.asStateFlow()

    // NEU: Sensoren Flow
    private val _sensors = MutableStateFlow<List<HaEntity>>(emptyList())
    val sensors = _sensors.asStateFlow()

    private val _areas = MutableStateFlow<List<HaArea>>(emptyList())
    val areas = _areas.asStateFlow()

    private val _selectedArea = MutableStateFlow<String?>(null)
    val selectedArea = _selectedArea.asStateFlow()

    private val _serverInfo = MutableStateFlow<Pair<String, String>>(Pair("Loading...", "Loading..."))
    val serverInfo = _serverInfo.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    val settingsFlow = context.dataStore.data.map { prefs ->
        Triple(prefs[URL_KEY] ?: "", prefs[TOKEN_KEY] ?: "", prefs[REFRESH_TOKEN_KEY] ?: "")
    }

    private var apiService: HomeAssistantService? = null
    private val CLIENT_ID = "https://home-assistant.io/android"
    private val REDIRECT_URI = "homeassistant://auth-callback"

    sealed class AuthState {
        object Loading : AuthState()
        object LoggedOut : AuthState()
        object LoggedIn : AuthState()
        data class Error(val message: String) : AuthState()
    }

    init {
        viewModelScope.launch {
            context.dataStore.data.map { prefs ->
                Pair(prefs[URL_KEY], prefs[TOKEN_KEY])
            }.collect { (url, token) ->
                if (!url.isNullOrBlank() && !token.isNullOrBlank()) {
                    initConnection(url, token)
                    _authState.value = AuthState.LoggedIn
                } else {
                    _authState.value = AuthState.LoggedOut
                }
            }
        }
    }

    fun initConnection(baseUrl: String, token: String) {
        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl(cleanUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(HomeAssistantService::class.java)

            // ÄNDERUNG: Wir starten alles parallel, nicht nacheinander!

            // 1. WebSocket verbinden
            viewModelScope.launch {
                connectWebSocket(cleanUrl, token)
            }

            // 2. Server Infos (Name, Version) laden - SOFORT
            viewModelScope.launch {
                fetchServerInfo(token)
            }

            // 3. Geräte laden
            viewModelScope.launch {
                refreshData(token)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchServerInfo(token: String) {
        viewModelScope.launch {
            try {
                // Nur getConfig aufrufen (das enthält Version UND Name des Hauses)
                val config = apiService?.getConfig("Bearer $token")

                val locationName = config?.locationName ?: "My Home"
                val version = config?.version ?: "Unknown"

                // Update UI
                _serverInfo.value = Pair(locationName, version)

            } catch (e: HttpException) {
                // WICHTIG: Token Refresh Logik auch hier einbauen (gegen den 401 Fehler)
                if (e.code() == 401) {
                    Log.w("MainViewModel", "401 in fetchServerInfo - trying refresh")
                    if (attemptTokenRefresh()) {
                        val newToken = context.dataStore.data.first()[TOKEN_KEY]
                        if (newToken != null) fetchServerInfo(newToken) // Retry mit neuem Token
                    }
                } else {
                    Log.e("MainViewModel", "Http Error in fetchServerInfo: ${e.code()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching server info: ${e.message}")
                _serverInfo.value = Pair("Home Assistant", "Unknown")
            }
        }
    }

    private suspend fun refreshData(token: String) {
        try {
            val currentStates = apiService?.getStates("Bearer $token") ?: emptyList()

            // 1. Filter Lights
            val justLights = currentStates.filter { it.entityId.startsWith("light.") }

            // 2. Filter Sensors (Umfassend erweitert)
            val relevantDeviceClasses = listOf(
                // Klima & Analog
                "temperature", "illuminance", "humidity", "pressure", "battery", "power", "energy", "signal_strength",
                // Bewegung & Präsenz
                "motion", "occupancy", "presence",
                // Zugang & Sicherheit
                "door", "garage_door", "window", "opening", "lock",
                // Gefahren (Binary)
                "smoke", "gas", "moisture", "vibration", "safety", "problem", "sound"
            )

            val justSensors = currentStates.filter { entity ->
                val domain = entity.entityId.split(".")[0]
                val dc = entity.attributes.deviceClass
                (domain == "sensor" || domain == "binary_sensor") && dc != null && relevantDeviceClasses.contains(dc)
            }

            // 3. Template für Areas (Jetzt mit allen Sensor-Typen)
            // Hinweis: Wir prüfen im Template nicht mehr jede Klasse einzeln, um den String kurz zu halten.
            // Wir prüfen nur, ob es ein Sensor/Licht ist. Die Filterung passierte ja schon in Schritt 2.
            val templateString = """
                [
                {%- set ns = namespace(first=true) -%}
                {%- for state in states -%}
                  {%- if state.entity_id.startswith('light.') 
                      or state.entity_id.startswith('sensor.') 
                      or state.entity_id.startswith('binary_sensor.') 
                  -%}
                    {%- if not ns.first -%},{%- endif -%}
                    {%- set ns.first = false -%}
                    {
                      "id": "{{ state.entity_id }}",
                      "area_id": "{{ area_id(state.entity_id) }}",
                      "area_name": "{{ area_name(state.entity_id) }}"
                    }
                  {%- endif -%}
                {%- endfor -%}
                ]
            """.trimIndent()

            val locations = try {
                apiService?.getLightLocations("Bearer $token", TemplateRequest(templateString)) ?: emptyList()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Template Error: ${e.message}")
                emptyList()
            }

            val entityToAreaIdMap = locations.associate { it.entityId to it.areaId }

            val uniqueAreas = locations
                .filter { !it.areaId.isNullOrBlank() && !it.areaName.isNullOrBlank() && it.areaId != "None" }
                .distinctBy { it.areaId }
                .map { HaArea(it.areaId!!, it.areaName!!) }
                .sortedBy { it.name }

            val enrichedLights = justLights.map { light ->
                light.apply {
                    val foundId = entityToAreaIdMap[light.entityId]
                    areaId = if (foundId == "None" || foundId.isNullOrBlank()) null else foundId
                }
            }

            val enrichedSensors = justSensors.map { sensor ->
                sensor.apply {
                    val foundId = entityToAreaIdMap[sensor.entityId]
                    areaId = if (foundId == "None" || foundId.isNullOrBlank()) null else foundId
                }
            }

            _areas.value = uniqueAreas
            _lights.value = enrichedLights
            _sensors.value = enrichedSensors

        } catch (e: HttpException) {
            if (e.code() == 401) {
                if (attemptTokenRefresh()) {
                    val newToken = context.dataStore.data.first()[TOKEN_KEY]
                    if (newToken != null) refreshData(newToken)
                } else {
                    logout()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun attemptTokenRefresh(): Boolean {
        // ... (unverändert)
        val prefs = context.dataStore.data.first()
        val refreshToken = prefs[REFRESH_TOKEN_KEY]
        val baseUrl = prefs[URL_KEY]

        if (refreshToken.isNullOrBlank() || baseUrl.isNullOrBlank()) return false

        return try {
            val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val retrofit = Retrofit.Builder()
                .baseUrl(cleanUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val tempService = retrofit.create(HomeAssistantService::class.java)

            val response = tempService.refreshToken(
                refreshToken = refreshToken,
                clientId = CLIENT_ID
            )

            context.dataStore.edit { it[TOKEN_KEY] = response.accessToken }
            initConnection(baseUrl, response.accessToken)
            true
        } catch (e: Exception) {
            Log.e("MainViewModel", "Refresh failed", e)
            false
        }
    }

    fun selectArea(areaId: String?) {
        _selectedArea.value = areaId
    }

    private fun connectWebSocket(baseUrl: String, token: String) {
        webSocket?.close(1000, null)
        val wsUrl = baseUrl.replace("http", "ws") + "api/websocket"
        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleWebSocketMessage(webSocket, text, token)
            }
        })
    }

    private fun handleWebSocketMessage(ws: WebSocket, text: String, token: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)
            val type = json.get("type").asString

            when (type) {
                "auth_required" -> ws.send(gson.toJson(mapOf("type" to "auth", "access_token" to token)))
                "auth_ok" -> ws.send(gson.toJson(mapOf("id" to 1, "type" to "subscribe_events", "event_type" to "state_changed")))
                "event" -> {
                    val event = json.get("event").asJsonObject
                    val data = event.get("data").asJsonObject
                    val entityId = data.get("entity_id").asString

                    if (entityId.startsWith("light.") || entityId.startsWith("sensor.") || entityId.startsWith("binary_sensor.")) {
                        val newStateJson = data.get("new_state").asJsonObject
                        updateLocalList(gson.fromJson(newStateJson, HaEntity::class.java))
                    }
                }
            }
        } catch (e: Exception) { }
    }

    private fun updateLocalList(updatedEntity: HaEntity) {
        // Update Lights
        if (updatedEntity.entityId.startsWith("light.")) {
            val currentList = _lights.value.toMutableList()
            val index = currentList.indexOfFirst { it.entityId == updatedEntity.entityId }
            if (index != -1) {
                updatedEntity.areaId = currentList[index].areaId
                currentList[index] = updatedEntity
                _lights.value = currentList
            }
        }
        // Update Sensors
        else {
            val currentList = _sensors.value.toMutableList()
            val index = currentList.indexOfFirst { it.entityId == updatedEntity.entityId }
            if (index != -1) {
                updatedEntity.areaId = currentList[index].areaId
                currentList[index] = updatedEntity
                _sensors.value = currentList
            }
        }
    }

    // ... (Toggle Funktionen bleiben identisch)
    fun toggleLight(entity: HaEntity, token: String) {
        val wasOn = entity.state == "on"
        val newState = if (wasOn) "off" else "on"
        val optimisticEntity = entity.copy(state = newState)
        optimisticEntity.areaId = entity.areaId
        updateLocalList(optimisticEntity)

        viewModelScope.launch {
            try {
                val payload = ServicePayload(entityId = entity.entityId)
                if (wasOn) apiService?.turnOff("Bearer $token", payload)
                else apiService?.turnOn("Bearer $token", payload)
            } catch (e: Exception) { updateLocalList(entity) }
        }
    }

    fun toggleArea(area: HaArea, token: String) {
        val lightsInArea = _lights.value.filter { it.areaId == area.areaId }
        val isAnyOn = lightsInArea.any { it.state == "on" }
        val newState = if (isAnyOn) "off" else "on"
        val updatedList = _lights.value.map { if (it.areaId == area.areaId) it.copy(state = newState) else it }
        _lights.value = updatedList

        viewModelScope.launch {
            try {
                val payload = AreaPayload(areaId = area.areaId)
                if (isAnyOn) apiService?.turnOffArea("Bearer $token", payload)
                else apiService?.turnOnArea("Bearer $token", payload)
            } catch (e: Exception) { }
        }
    }

    fun updateLightState(entityId: String, brightness: Int, color: List<Int>, token: String) {
        viewModelScope.launch {
            try {
                val payload = ServicePayload(entityId = entityId, brightness = brightness, rgbColor = color)
                apiService?.turnOn("Bearer $token", payload)
            } catch (e: Exception) { }
        }
    }

    fun setAreaBrightness(area: HaArea, brightness: Int, token: String) {
        viewModelScope.launch {
            try {
                val payload = AreaBrightnessPayload(areaId = area.areaId, brightness = brightness)
                apiService?.turnOnAreaLight("Bearer $token", payload)
            } catch (e: Exception) { }
        }
    }

    fun startLoginFlow(baseUrl: String) {
        // ... (unverändert)
        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        viewModelScope.launch {
            context.dataStore.edit { it[URL_KEY] = cleanUrl }
            val authUrl = "$cleanUrl/auth/authorize?response_type=code&client_id=$CLIENT_ID&redirect_uri=$REDIRECT_URI"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun handleAuthCallback(uri: Uri) {
        if (uri.toString().startsWith(REDIRECT_URI)) {
            uri.getQueryParameter("code")?.let { code ->
                viewModelScope.launch { exchangeCodeForToken(code) }
            }
        }
    }

    private suspend fun exchangeCodeForToken(code: String) {
        // ... (unverändert)
        try {
            val prefs = context.dataStore.data.first()
            var baseUrl = prefs[URL_KEY] ?: return
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.dropLast(1)
            val retrofit = Retrofit.Builder().baseUrl("$baseUrl/").addConverterFactory(GsonConverterFactory.create()).build()
            val tempService = retrofit.create(HomeAssistantService::class.java)
            val response = tempService.getToken(code = code, clientId = CLIENT_ID, redirectUri = REDIRECT_URI)

            context.dataStore.edit {
                it[TOKEN_KEY] = response.accessToken
                if (response.refreshToken != null) it[REFRESH_TOKEN_KEY] = response.refreshToken
            }
            initConnection(baseUrl, response.accessToken)
            _authState.value = AuthState.LoggedIn
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Auth Failed")
        }
    }

    fun logout() {
        webSocket?.close(1000, "Logout")
        viewModelScope.launch {
            context.dataStore.edit { it.clear() }
            _lights.value = emptyList()
            _areas.value = emptyList()
            _sensors.value = emptyList() // Clear sensors too
            _authState.value = AuthState.LoggedOut
        }
    }
}