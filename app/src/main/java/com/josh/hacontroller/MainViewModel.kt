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
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
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

    // State Flows
    private val _lights = MutableStateFlow<List<HaEntity>>(emptyList())
    val lights = _lights.asStateFlow()

    private val _areas = MutableStateFlow<List<HaArea>>(emptyList())
    val areas = _areas.asStateFlow()

    private val _selectedArea = MutableStateFlow<String?>(null)
    val selectedArea = _selectedArea.asStateFlow()

    private val _serverInfo = MutableStateFlow<Pair<String, String>>(Pair("Loading...", "Loading..."))
    val serverInfo = _serverInfo.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    val settingsFlow = context.dataStore.data.map { prefs ->
        Pair(prefs[URL_KEY] ?: "", prefs[TOKEN_KEY] ?: "")
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
        // Auto-login
        viewModelScope.launch {
            context.dataStore.data.map { prefs -> Pair(prefs[URL_KEY], prefs[TOKEN_KEY]) }
                .collect { (url, token) ->
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

            viewModelScope.launch {
                refreshData(token)
                connectWebSocket(cleanUrl, token)
                fetchServerInfo(token)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun refreshData(token: String) {
        try {
            // 1. Fetch States
            val currentStates = apiService?.getStates("Bearer $token") ?: emptyList()
            val justLights = currentStates.filter { it.entityId.startsWith("light.") }

            // 2. Fetch Locations via Template
            val templateString = """
                [
                {%- for state in states.light -%}
                  {
                    "id": "{{ state.entity_id }}",
                    "area_id": "{{ area_id(state.entity_id) }}",
                    "area_name": "{{ area_name(state.entity_id) }}"
                  }
                  {%- if not loop.last -%},{%- endif -%}
                {%- endfor -%}
                ]
            """.trimIndent()

            val locations = try {
                apiService?.getLightLocations("Bearer $token", TemplateRequest(templateString)) ?: emptyList()
            } catch (e: Exception) { emptyList() }

            // 3. Process Data
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

            _areas.value = uniqueAreas
            _lights.value = enrichedLights

        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun fetchServerInfo(token: String) {
        viewModelScope.launch {
            try {
                val config = apiService?.getConfig("Bearer $token")
                val user = apiService?.getCurrentUser("Bearer $token")
                _serverInfo.value = Pair(user?.name ?: "User", config?.version ?: "Unknown")
            } catch (e: Exception) { }
        }
    }

    fun selectArea(areaId: String?) {
        _selectedArea.value = areaId
    }

    // --- WEBSOCKET ---
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

                    if (entityId.startsWith("light.")) {
                        val newStateJson = data.get("new_state").asJsonObject
                        val newEntity = gson.fromJson(newStateJson, HaEntity::class.java)
                        updateLocalList(newEntity)
                    }
                }
            }
        } catch (e: Exception) { }
    }

    private fun updateLocalList(updatedEntity: HaEntity) {
        val currentList = _lights.value.toMutableList()
        val index = currentList.indexOfFirst { it.entityId == updatedEntity.entityId }

        if (index != -1) {
            val existingItem = currentList[index]
            updatedEntity.areaId = existingItem.areaId // Preserve Area ID
            currentList[index] = updatedEntity
            _lights.value = currentList
        }
    }

    // --- ACTIONS ---

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
            } catch (e: Exception) {
                updateLocalList(entity) // Revert
            }
        }
    }

    fun toggleArea(area: HaArea, token: String) {
        val lightsInArea = _lights.value.filter { it.areaId == area.areaId }
        val isAnyOn = lightsInArea.any { it.state == "on" }
        val newState = if (isAnyOn) "off" else "on"

        // Optimistic
        val updatedList = _lights.value.map {
            if (it.areaId == area.areaId) it.copy(state = newState) else it
        }
        _lights.value = updatedList

        viewModelScope.launch {
            try {
                val payload = AreaPayload(areaId = area.areaId)
                if (isAnyOn) apiService?.turnOffArea("Bearer $token", payload)
                else apiService?.turnOnArea("Bearer $token", payload)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun updateLightState(entityId: String, brightness: Int, color: List<Int>, token: String) {
        viewModelScope.launch {
            val payload = ServicePayload(entityId = entityId, brightness = brightness, rgbColor = color)
            apiService?.turnOn("Bearer $token", payload)
        }
    }

    // NEW: Master Brightness Slider
    fun setAreaBrightness(area: HaArea, brightness: Int, token: String) {
        // Optimistic
        val currentList = _lights.value.toMutableList()
        val updatedList = currentList.map { item ->
            if (item.areaId == area.areaId) {
                val newAttribs = item.attributes.copy(brightness = brightness)
                item.copy(state = "on", attributes = newAttribs)
            } else {
                item
            }
        }
        _lights.value = updatedList

        viewModelScope.launch {
            try {
                val payload = AreaBrightnessPayload(areaId = area.areaId, brightness = brightness)
                apiService?.turnOnAreaLight("Bearer $token", payload)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- AUTH FLOW ---
    fun startLoginFlow(baseUrl: String) {
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
        try {
            val prefs = context.dataStore.data.first()
            var baseUrl = prefs[URL_KEY] ?: return
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.dropLast(1)
            val retrofit = Retrofit.Builder().baseUrl("$baseUrl/").addConverterFactory(GsonConverterFactory.create()).build()
            val tempService = retrofit.create(HomeAssistantService::class.java)
            val response = tempService.getToken(code = code, clientId = CLIENT_ID, redirectUri = REDIRECT_URI)
            context.dataStore.edit { it[TOKEN_KEY] = response.accessToken }
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
            _authState.value = AuthState.LoggedOut
        }
    }
}