package com.trustlens.app.data.remote

import com.trustlens.app.data.model.AnalyzeResponse
import com.trustlens.app.data.model.ComparisonDemoResponse
import com.trustlens.app.data.model.DemoAnalyzeRequest
import com.trustlens.app.data.model.HealthResponse
import com.trustlens.app.data.model.MessageAnalyzeRequest
import com.trustlens.app.data.model.MessageAnalyzeResponse
import com.trustlens.app.data.model.QrAnalyzeRequest
import com.trustlens.app.data.model.QrAnalyzeResponse
import com.trustlens.app.data.model.UrlAnalyzeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit contract for TRUSTLENS backend REST API.
 */
interface TrustlensApiService {

    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>

    @POST("api/v1/analyze")
    suspend fun analyzeUrl(
        @Body request: UrlAnalyzeRequest
    ): Response<AnalyzeResponse>

    @POST("api/v1/analyze/message")
    suspend fun analyzeMessage(
        @Body request: MessageAnalyzeRequest
    ): Response<MessageAnalyzeResponse>

    @POST("api/v1/analyze/qr")
    suspend fun analyzeQr(
        @Body request: QrAnalyzeRequest
    ): Response<QrAnalyzeResponse>

    @POST("api/v1/demo")
    suspend fun runDemo(
        @Body request: DemoAnalyzeRequest
    ): Response<AnalyzeResponse>

    @GET("api/v1/demo/compare")
    suspend fun getComparisonDemo(): Response<ComparisonDemoResponse>
}
