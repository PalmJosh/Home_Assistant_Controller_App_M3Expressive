package com.josh.hacontroller

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// --- DATA MODELS ---

data class HaEntity(
    @SerializedName("entity_id") val entityId: String,
    val state: String,
    val attributes: HaAttributes,
    var areaId: String? = null
)

data class HaAttributes(
    @SerializedName("friendly_name") val friendlyName: String?,
    val brightness: Int?,
    @SerializedName("rgb_color") val rgbColor: List<Int>?,
    @SerializedName("unit_of_measurement") val unitOfMeasurement: String?,
    @SerializedName("device_class") val deviceClass: String?
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Long,
    @SerializedName("refresh_token") val refreshToken: String?
)

// --- PAYLOADS ---

data class ServicePayload(
    @SerializedName("entity_id") val entityId: String,
    val brightness: Int? = null,
    @SerializedName("rgb_color") val rgbColor: List<Int>? = null
)

data class AreaPayload(
    @SerializedName("area_id") val areaId: String
)

data class AreaBrightnessPayload(
    @SerializedName("area_id") val areaId: String,
    val brightness: Int
)

data class TemplateRequest(val template: String)

// --- INTERNAL MAPPING MODELS ---

data class LightLocation(
    @SerializedName("id") val entityId: String,
    @SerializedName("area_id") val areaId: String?,
    @SerializedName("area_name") val areaName: String?
)

data class HaArea(
    val areaId: String,
    val name: String
)

data class HaConfig(
    val version: String?,
    @SerializedName("location_name") val locationName: String?
)

data class HaUser(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("username") val username: String?
)

// --- API INTERFACE ---

interface HomeAssistantService {

    // Auth
    @FormUrlEncoded
    @POST("auth/token")
    suspend fun getToken(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("client_id") clientId: String,
        @Field("redirect_uri") redirectUri: String
    ): TokenResponse

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String
    ): TokenResponse

    // Data Fetching
    @GET("api/states")
    suspend fun getStates(@Header("Authorization") auth: String): List<HaEntity>

    @POST("api/template")
    suspend fun getLightLocations(
        @Header("Authorization") auth: String,
        @Body body: TemplateRequest
    ): List<LightLocation>

    @GET("api/config")
    suspend fun getConfig(@Header("Authorization") auth: String): HaConfig

    @GET("auth/current_user")
    suspend fun getCurrentUser(@Header("Authorization") auth: String): HaUser

    // Device Control
    @POST("api/services/light/turn_on")
    suspend fun turnOn(@Header("Authorization") auth: String, @Body payload: ServicePayload)

    @POST("api/services/light/turn_off")
    suspend fun turnOff(@Header("Authorization") auth: String, @Body payload: ServicePayload)

    @POST("api/services/homeassistant/turn_on")
    suspend fun turnOnArea(@Header("Authorization") auth: String, @Body payload: AreaPayload)

    @POST("api/services/homeassistant/turn_off")
    suspend fun turnOffArea(@Header("Authorization") auth: String, @Body payload: AreaPayload)

    @POST("api/services/light/turn_on")
    suspend fun turnOnAreaLight(@Header("Authorization") auth: String, @Body payload: AreaBrightnessPayload)
}