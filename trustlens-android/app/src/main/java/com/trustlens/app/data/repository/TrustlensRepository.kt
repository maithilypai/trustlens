package com.trustlens.app.data.repository

import com.trustlens.app.data.model.AnalyzeResponse
import com.trustlens.app.data.model.ComparisonDemoResponse
import com.trustlens.app.data.model.DemoAnalyzeRequest
import com.trustlens.app.data.model.DemoScenario
import com.trustlens.app.data.model.HealthResponse
import com.trustlens.app.data.model.MessageAnalyzeRequest
import com.trustlens.app.data.model.MessageAnalyzeResponse
import com.trustlens.app.data.model.QrAnalyzeRequest
import com.trustlens.app.data.model.QrAnalyzeResponse
import com.trustlens.app.data.model.UrlAnalyzeRequest
import com.trustlens.app.data.remote.NetworkResult
import com.trustlens.app.data.remote.RetrofitClient
import com.trustlens.app.data.remote.TrustlensApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

interface TrustlensRepository {
    suspend fun analyzeUrl(url: String): NetworkResult<AnalyzeResponse>
    suspend fun analyzeMessage(message: String, sender: String? = null): NetworkResult<MessageAnalyzeResponse>
    suspend fun analyzeQr(payload: String): NetworkResult<QrAnalyzeResponse>
    suspend fun runDemo(scenario: DemoScenario, customUrl: String? = null): NetworkResult<AnalyzeResponse>
    suspend fun getComparisonDemo(): NetworkResult<ComparisonDemoResponse>
    suspend fun checkHealth(): NetworkResult<HealthResponse>
    fun updateBaseUrl(newBaseUrl: String)
    fun getBaseUrl(): String
}

class TrustlensRepositoryImpl(
    private var apiService: TrustlensApiService = RetrofitClient.getApiService()
) : TrustlensRepository {

    override fun updateBaseUrl(newBaseUrl: String) {
        RetrofitClient.setBaseUrl(newBaseUrl)
        apiService = RetrofitClient.getApiService()
    }

    override fun getBaseUrl(): String = RetrofitClient.getBaseUrl()

    override suspend fun analyzeUrl(url: String): NetworkResult<AnalyzeResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.analyzeUrl(UrlAnalyzeRequest(url = url.trim()))
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val error = RetrofitClient.parseError(response)
                val displayMsg = error.message ?: "Analysis failed with HTTP ${response.code()}"
                NetworkResult.Error(
                    message = displayMsg,
                    code = error.code ?: "HTTP_${response.code()}",
                    statusCode = response.code()
                )
            }
        } catch (t: Throwable) {
            handleException(t)
        }
    }

    override suspend fun analyzeMessage(message: String, sender: String?): NetworkResult<MessageAnalyzeResponse> =
        withContext(Dispatchers.IO) {
            try {
                val request = MessageAnalyzeRequest(message = message.trim(), sender = sender?.trim())
                val response = apiService.analyzeMessage(request)
                if (response.isSuccessful && response.body() != null) {
                    NetworkResult.Success(response.body()!!)
                } else {
                    val error = RetrofitClient.parseError(response)
                    NetworkResult.Error(
                        message = error.message ?: "Message inspection failed with HTTP ${response.code()}",
                        code = error.code ?: "HTTP_${response.code()}",
                        statusCode = response.code()
                    )
                }
            } catch (t: Throwable) {
                handleException(t)
            }
        }

    override suspend fun analyzeQr(payload: String): NetworkResult<QrAnalyzeResponse> = withContext(Dispatchers.IO) {
        try {
            val request = QrAnalyzeRequest(payload = payload.trim())
            val response = apiService.analyzeQr(request)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val error = RetrofitClient.parseError(response)
                NetworkResult.Error(
                    message = error.message ?: "QR analysis failed with HTTP ${response.code()}",
                    code = error.code ?: "HTTP_${response.code()}",
                    statusCode = response.code()
                )
            }
        } catch (t: Throwable) {
            handleException(t)
        }
    }

    override suspend fun runDemo(scenario: DemoScenario, customUrl: String?): NetworkResult<AnalyzeResponse> =
        withContext(Dispatchers.IO) {
            try {
                val request = DemoAnalyzeRequest(scenario = scenario, url = customUrl)
                val response = apiService.runDemo(request)
                if (response.isSuccessful && response.body() != null) {
                    NetworkResult.Success(response.body()!!)
                } else {
                    val error = RetrofitClient.parseError(response)
                    NetworkResult.Error(
                        message = error.message ?: "Demo scenario failed with HTTP ${response.code()}",
                        code = error.code ?: "HTTP_${response.code()}",
                        statusCode = response.code()
                    )
                }
            } catch (t: Throwable) {
                handleException(t)
            }
        }

    override suspend fun getComparisonDemo(): NetworkResult<ComparisonDemoResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getComparisonDemo()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                val error = RetrofitClient.parseError(response)
                NetworkResult.Error(
                    message = error.message ?: "Comparison demo failed with HTTP ${response.code()}",
                    code = error.code ?: "HTTP_${response.code()}",
                    statusCode = response.code()
                )
            }
        } catch (t: Throwable) {
            handleException(t)
        }
    }

    override suspend fun checkHealth(): NetworkResult<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getHealth()
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Backend health check failed (${response.code()})")
            }
        } catch (t: Throwable) {
            handleException(t)
        }
    }

    private fun handleException(e: Throwable): NetworkResult.Error {
        val message = e.localizedMessage ?: e.message ?: ""
        if (message.contains("CLEARTEXT", ignoreCase = true) || message.contains("Cleartext", ignoreCase = true)) {
            return NetworkResult.Error(
                message = "Cleartext HTTP traffic is not permitted by policy. Ensure network security configuration allows cleartext for local development.",
                code = "CLEARTEXT_NOT_PERMITTED"
            )
        }
        return when (e) {
            is SocketTimeoutException -> NetworkResult.Error(
                message = "Connection timed out. The backend server took too long to respond.",
                code = "TIMEOUT"
            )
            is ConnectException -> NetworkResult.Error(
                message = "Unable to connect to backend at ${RetrofitClient.getBaseUrl()}. " +
                        "Verify that the FastAPI server is running (http://127.0.0.1:8000 on host). " +
                        "If running on an Android Emulator, use http://10.0.2.2:8000/.",
                code = "CONNECTION_REFUSED"
            )
            is UnknownHostException -> NetworkResult.Error(
                message = "Cannot resolve backend hostname. Check your network connection and server address.",
                code = "UNKNOWN_HOST"
            )
            is IOException -> NetworkResult.Error(
                message = "Network error: ${e.localizedMessage ?: "Unknown I/O failure"}",
                code = "NETWORK_ERROR"
            )
            else -> NetworkResult.Error(
                message = "Unexpected error: ${e.localizedMessage ?: "Internal failure"}",
                code = "UNEXPECTED"
            )
        }
    }
}
