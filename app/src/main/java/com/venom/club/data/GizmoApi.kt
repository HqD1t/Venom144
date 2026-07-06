package com.venom.club.data

import com.venom.club.data.model.GizmoStats
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Клиент к мини-серверу в клубе (gizmo-proxy), который уже сам ходит в Gizmo API.
 * Авторизация — Firebase ID token в заголовке.
 */
interface GizmoProxyApi {
    @GET("stats/by-phone/{phone}")
    suspend fun statsByPhone(
        @Path("phone") phone8: String,
        @Header("Authorization") bearer: String,
    ): GizmoStats

    @GET("hosts/status")
    suspend fun hostsStatus(@Header("Authorization") bearer: String): Map<String, Boolean> // hostId -> занят ли
}

object GizmoClient {
    // TODO: заменить на адрес мини-сервера клуба (белый IP или VPN/туннель)
    var baseUrl: String = "http://192.168.1.10:8017/"

    val api: GizmoProxyApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GizmoProxyApi::class.java)
    }
}
