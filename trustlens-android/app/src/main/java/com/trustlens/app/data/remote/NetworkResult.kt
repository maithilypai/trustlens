package com.trustlens.app.data.remote

/**
 * Encapsulates the UI-ready state of an asynchronous API operation.
 */
sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(
        val message: String,
        val code: String? = null,
        val statusCode: Int? = null
    ) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
}
