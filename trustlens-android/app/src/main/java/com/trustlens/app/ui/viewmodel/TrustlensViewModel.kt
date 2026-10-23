package com.trustlens.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trustlens.app.data.model.DemoScenario
import com.trustlens.app.data.model.RiskCategory
import com.trustlens.app.data.remote.NetworkResult
import com.trustlens.app.data.repository.TrustlensRepository
import com.trustlens.app.data.repository.TrustlensRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrustlensViewModel(
    private val repository: TrustlensRepository = TrustlensRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TrustlensUiState(currentBaseUrl = repository.getBaseUrl())
    )
    val uiState: StateFlow<TrustlensUiState> = _uiState.asStateFlow()

    fun onUrlInputChanged(newInput: String) {
        _uiState.update { it.copy(urlInput = newInput, errorMessage = null, errorCode = null) }
    }

    fun clearInput() {
        _uiState.update { it.copy(urlInput = "", errorMessage = null, errorCode = null) }
    }

    fun pasteFromClipboard(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            _uiState.update { it.copy(urlInput = trimmed, errorMessage = null, errorCode = null) }
        }
    }

    fun handleSharedIntentUrl(sharedText: String) {
        val extractedUrl = extractUrlFromText(sharedText)
        if (extractedUrl != null) {
            _uiState.update { it.copy(urlInput = extractedUrl, errorMessage = null) }
            analyzeUrl(extractedUrl)
        }
    }

    fun analyzeUrl(url: String = _uiState.value.urlInput) {
        if (_uiState.value.isLoading) return

        val target = url.trim()
        if (target.isEmpty()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Please enter a valid URL to analyze.",
                    errorCode = "EMPTY_INPUT"
                )
            }
            return
        }

        // Basic client-side scheme reminder (full validation is done safely by backend)
        val candidate = if (!target.startsWith("http://") && !target.startsWith("https://")) {
            "https://$target"
        } else {
            target
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                errorCode = null,
                result = null
            )
        }

        viewModelScope.launch {
            try {
                when (val result = repository.analyzeUrl(candidate)) {
                    is NetworkResult.Success -> {
                        val response = result.data
                        val showWarning = response.assessment == RiskCategory.CONFIRMED_THREAT
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                result = response,
                                isWarningDialogVisible = showWarning,
                                errorMessage = null,
                                errorCode = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message,
                                errorCode = result.code,
                                result = null
                            )
                        }
                    }
                    NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "An unexpected error occurred: ${t.localizedMessage ?: "Unknown failure"}",
                        errorCode = "UNEXPECTED_ERROR",
                        result = null
                    )
                }
            } finally {
                // Guaranteed safety: ensure the loading indicator is never left active
                _uiState.update {
                    if (it.isLoading) it.copy(isLoading = false) else it
                }
            }
        }
    }

    fun runDemoScenario(scenario: DemoScenario) {
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                errorCode = null,
                isDemoPickerVisible = false,
                result = null
            )
        }

        viewModelScope.launch {
            try {
                when (val result = repository.runDemo(scenario)) {
                    is NetworkResult.Success -> {
                        val response = result.data
                        val showWarning = response.assessment == RiskCategory.CONFIRMED_THREAT
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                result = response,
                                isWarningDialogVisible = showWarning,
                                urlInput = response.urlValidation.normalizedUrl,
                                errorMessage = null,
                                errorCode = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message,
                                errorCode = result.code,
                                result = null
                            )
                        }
                    }
                    NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Demo execution failed: ${t.localizedMessage ?: "Unknown failure"}",
                        errorCode = "DEMO_ERROR",
                        result = null
                    )
                }
            } finally {
                // Guaranteed safety: ensure the loading indicator is never left active
                _uiState.update {
                    if (it.isLoading) it.copy(isLoading = false) else it
                }
            }
        }
    }

    fun dismissWarningDialog() {
        _uiState.update { it.copy(isWarningDialogVisible = false) }
    }

    fun showWarningDialog() {
        _uiState.update { it.copy(isWarningDialogVisible = true) }
    }

    fun clearResult() {
        _uiState.update {
            it.copy(
                result = null,
                errorMessage = null,
                errorCode = null,
                isWarningDialogVisible = false
            )
        }
    }

    fun toggleDemoPicker(visible: Boolean) {
        _uiState.update { it.copy(isDemoPickerVisible = visible) }
    }

    fun toggleSettings(visible: Boolean) {
        _uiState.update { it.copy(isSettingsVisible = visible) }
    }

    fun updateBaseUrl(newUrl: String) {
        repository.updateBaseUrl(newUrl)
        _uiState.update {
            it.copy(
                currentBaseUrl = repository.getBaseUrl(),
                isSettingsVisible = false
            )
        }
    }

    private fun extractUrlFromText(text: String): String? {
        val trimmed = text.trim()
        val urlRegex = "(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+)".toRegex()
        val match = urlRegex.find(trimmed)
        return match?.value ?: if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else null
    }
}

class TrustlensViewModelFactory(
    private val repository: TrustlensRepository = TrustlensRepositoryImpl()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrustlensViewModel::class.java)) {
            return TrustlensViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
