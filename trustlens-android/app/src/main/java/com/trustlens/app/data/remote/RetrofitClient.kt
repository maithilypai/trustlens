package com.trustlens.app.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.trustlens.app.data.model.ApiErrorResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton networking provider for TRUSTLENS.
 *
 * Defaults to `http://10.0.2.2:8000/` which is the standard alias used by the Android
 * Emulator to communicate with `localhost:8000` on the development host.
 */
object RetrofitClient {

    const val DEFAULT_EMULATOR_BASE_URL = "http://10.0.2.2:8000/"

    private var currentBaseUrl: String = DEFAULT_EMULATOR_BASE_URL
    private var apiService: TrustlensApiService? = null

    val gson: Gson = GsonBuilder().setLenient().create()

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Get or initialize the Retrofit API service.
     */
    fun getApiService(baseUrl: String = currentBaseUrl): TrustlensApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (apiService == null || currentBaseUrl != normalizedUrl) {
            currentBaseUrl = normalizedUrl
            val retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            apiService = retrofit.create(TrustlensApiService::class.java)
        }
        return apiService!!
    }

    /**
     * Set a custom base URL (e.g., for physical phone testing via WiFi IP).
     */
    fun setBaseUrl(newBaseUrl: String) {
        val normalized = if (newBaseUrl.endsWith("/")) newBaseUrl else "$newBaseUrl/"
        if (currentBaseUrl != normalized) {
            currentBaseUrl = normalized
            apiService = null
        }
    }

    fun getBaseUrl(): String = currentBaseUrl

    /**
     * Helper to safely extract structured error messages from failed Retrofit responses.
     */
    fun <T> parseError(response: Response<T>): ApiErrorResponse {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val parsed = gson.fromJson(errorBody, ApiErrorResponse::class.java)
                if (parsed != null) {
                    val resolvedMsg = parsed.message
                        ?: parsed.error
                        ?: "Server returned HTTP ${response.code()}"
                    parsed.copy(message = resolvedMsg)
                } else {
                    ApiErrorResponse(message = "Server returned HTTP ${response.code()}")
                }
            } else {
                ApiErrorResponse(message = "Server returned HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            ApiErrorResponse(message = "HTTP ${response.code()} ${response.message()}")
        }
    }
}
