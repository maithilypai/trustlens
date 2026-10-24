package com.trustlens.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trustlens.app.data.model.DemoScenario
import com.trustlens.app.data.model.RiskCategory
import com.trustlens.app.data.model.ScreenDestination
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

    init {
        checkBackendHealth()
    }

    // Navigation
    fun navigateTo(destination: ScreenDestination) {
        _uiState.update { it.copy(currentScreen = destination) }
    }

    // Backend Connectivity Check
    fun checkBackendHealth() {
        _uiState.update { it.copy(backendStatus = "checking") }
        viewModelScope.launch {
            when (val health = repository.checkHealth()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            backendStatus = "online",
                            backendVersion = health.data.version,
                            isThreatIntelActive = health.data.threatIntelConfigured
                        )
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            backendStatus = "offline",
                            backendVersion = null,
                            isThreatIntelActive = false
                        )
                    }
                }
            }
        }
    }

    // URL Quick Scanner
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
        val trimmed = sharedText.trim()
        val urls = extractAllUrlsFromText(trimmed)
        if (urls.size == 1) {
            // Single URL: load into URL scanner and analyze
            _uiState.update { it.copy(urlInput = urls.first(), errorMessage = null, currentScreen = ScreenDestination.HOME) }
            analyzeUrl(urls.first())
        } else if (urls.size > 1 || trimmed.length > 80 || containsMessageLures(trimmed)) {
            // Multiple URLs or SMS message text: navigate to message inspector
            _uiState.update {
                it.copy(
                    messageInput = trimmed,
                    messageErrorMessage = null,
                    currentScreen = ScreenDestination.MESSAGE_ANALYZER
                )
            }
        } else {
            // Raw text
            val single = extractUrlFromText(trimmed) ?: trimmed
            _uiState.update { it.copy(urlInput = single, errorMessage = null, currentScreen = ScreenDestination.HOME) }
            if (single.startsWith("http://") || single.startsWith("https://")) {
                analyzeUrl(single)
            }
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
                                currentScreen = ScreenDestination.RESULT,
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
                _uiState.update {
                    if (it.isLoading) it.copy(isLoading = false) else it
                }
            }
        }
    }

    // Safe Browser Module
    fun onBrowserUrlChanged(newUrl: String) {
        _uiState.update { it.copy(browserUrlInput = newUrl) }
    }

    fun navigateSafeBrowser(urlToVisit: String = _uiState.value.browserUrlInput) {
        val trimmed = urlToVisit.trim()
        if (trimmed.isEmpty()) return

        // Reject unsupported schemes
        val lower = trimmed.lowercase()
        if (lower.startsWith("javascript:") || lower.startsWith("file:") || lower.startsWith("data:") || lower.startsWith("content:")) {
            _uiState.update {
                it.copy(
                    browserCautionMessage = "Blocked unsafe scheme: Only 'http://' and 'https://' are supported in Safe Browser.",
                    browserBlockedThreat = null
                )
            }
            return
        }

        val targetUrl = if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }

        _uiState.update {
            it.copy(
                browserUrlInput = targetUrl,
                isBrowserVerifying = true,
                browserBlockedThreat = null,
                browserCautionMessage = null
            )
        }

        viewModelScope.launch {
            try {
                when (val result = repository.analyzeUrl(targetUrl)) {
                    is NetworkResult.Success -> {
                        val response = result.data
                        when (response.assessment) {
                            RiskCategory.CONFIRMED_THREAT, RiskCategory.SUSPICIOUS_INDICATORS -> {
                                // Block navigation entirely! Show blocking warning
                                _uiState.update {
                                    it.copy(
                                        isBrowserVerifying = false,
                                        browserBlockedThreat = response,
                                        browserActiveUrl = null
                                    )
                                }
                            }
                            RiskCategory.INCONCLUSIVE -> {
                                // Allow navigation with prominent caution banner
                                _uiState.update {
                                    it.copy(
                                        isBrowserVerifying = false,
                                        browserBlockedThreat = null,
                                        browserCautionMessage = "Caution: Reputation verification is unavailable for this domain.",
                                        browserActiveUrl = targetUrl
                                    )
                                }
                            }
                            RiskCategory.NO_KNOWN_THREAT -> {
                                // Safe to navigate
                                _uiState.update {
                                    it.copy(
                                        isBrowserVerifying = false,
                                        browserBlockedThreat = null,
                                        browserCautionMessage = null,
                                        browserActiveUrl = targetUrl
                                    )
                                }
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        // Backend unreachable or network error
                        _uiState.update {
                            it.copy(
                                isBrowserVerifying = false,
                                browserBlockedThreat = null,
                                browserCautionMessage = "Backend security check failed (${result.message}). Proceed with caution.",
                                browserActiveUrl = targetUrl
                            )
                        }
                    }
                    NetworkResult.Loading -> {
                        _uiState.update { it.copy(isBrowserVerifying = true) }
                    }
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isBrowserVerifying = false,
                        browserCautionMessage = "Verification error: ${t.localizedMessage ?: "Unknown failure"}",
                        browserActiveUrl = targetUrl
                    )
                }
            }
        }
    }

    fun dismissBrowserThreatBlock() {
        _uiState.update {
            it.copy(
                browserBlockedThreat = null,
                browserUrlInput = "https://"
            )
        }
    }

    fun dismissBrowserCaution() {
        _uiState.update { it.copy(browserCautionMessage = null) }
    }

    // Message & SMS Inspection Module
    fun onMessageInputChanged(text: String) {
        _uiState.update { it.copy(messageInput = text, messageErrorMessage = null) }
    }

    fun onMessageSenderChanged(sender: String) {
        _uiState.update { it.copy(messageSenderInput = sender) }
    }

    fun clearMessage() {
        _uiState.update {
            it.copy(
                messageInput = "",
                messageSenderInput = "",
                messageResult = null,
                messageErrorMessage = null
            )
        }
    }

    fun loadSampleMessage(message: String, sender: String = "+1 (800) 555-0199") {
        _uiState.update {
            it.copy(
                messageInput = message,
                messageSenderInput = sender,
                messageErrorMessage = null
            )
        }
    }

    fun analyzeMessage() {
        val text = _uiState.value.messageInput.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(messageErrorMessage = "Please enter or paste message content to analyze.") }
            return
        }

        _uiState.update {
            it.copy(
                isMessageLoading = true,
                messageErrorMessage = null,
                messageResult = null
            )
        }

        viewModelScope.launch {
            try {
                when (val result = repository.analyzeMessage(text, _uiState.value.messageSenderInput)) {
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isMessageLoading = false,
                                messageResult = result.data,
                                messageErrorMessage = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isMessageLoading = false,
                                messageErrorMessage = result.message
                            )
                        }
                    }
                    NetworkResult.Loading -> {
                        _uiState.update { it.copy(isMessageLoading = true) }
                    }
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isMessageLoading = false,
                        messageErrorMessage = "Message inspection failed: ${t.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    // QR Code / Quishing Inspection Module
    fun onQrInputChanged(payload: String) {
        _uiState.update { it.copy(qrPayloadInput = payload, qrErrorMessage = null) }
    }

    fun clearQr() {
        _uiState.update {
            it.copy(
                qrPayloadInput = "",
                qrResult = null,
                qrErrorMessage = null
            )
        }
    }

    fun loadSampleQr(payload: String) {
        _uiState.update {
            it.copy(
                qrPayloadInput = payload,
                qrErrorMessage = null
            )
        }
    }

    fun analyzeQrPayload() {
        val payload = _uiState.value.qrPayloadInput.trim()
        if (payload.isEmpty()) {
            _uiState.update { it.copy(qrErrorMessage = "Please enter or paste a decoded QR payload string.") }
            return
        }

        _uiState.update {
            it.copy(
                isQrLoading = true,
                qrErrorMessage = null,
                qrResult = null
            )
        }

        viewModelScope.launch {
            try {
                when (val result = repository.analyzeQr(payload)) {
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isQrLoading = false,
                                qrResult = result.data,
                                qrErrorMessage = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isQrLoading = false,
                                qrErrorMessage = result.message
                            )
                        }
                    }
                    NetworkResult.Loading -> {
                        _uiState.update { it.copy(isQrLoading = true) }
                    }
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isQrLoading = false,
                        qrErrorMessage = "QR inspection failed: ${t.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    // Educational Demos & Comparison
    fun runDemoScenario(scenario: DemoScenario) {
        if (_uiState.value.isLoading) return

        if (scenario == DemoScenario.COMPARISON) {
            runComparisonDemo()
            return
        }

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
                                currentScreen = ScreenDestination.RESULT,
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
                _uiState.update {
                    if (it.isLoading) it.copy(isLoading = false) else it
                }
            }
        }
    }

    fun runComparisonDemo() {
        _uiState.update {
            it.copy(
                isComparisonLoading = true,
                isDemoPickerVisible = false,
                currentScreen = ScreenDestination.DEMOS
            )
        }

        viewModelScope.launch {
            try {
                when (val result = repository.getComparisonDemo()) {
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isComparisonLoading = false,
                                comparisonResult = result.data
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isComparisonLoading = false,
                                errorMessage = result.message
                            )
                        }
                    }
                    NetworkResult.Loading -> {
                        _uiState.update { it.copy(isComparisonLoading = true) }
                    }
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isComparisonLoading = false,
                        errorMessage = "Comparison demo failed: ${t.localizedMessage ?: "Unknown error"}"
                    )
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
                isWarningDialogVisible = false,
                currentScreen = ScreenDestination.HOME
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
        try {
            com.trustlens.app.TrustlensApplication.instance?.let { app ->
                val prefs = app.getSharedPreferences("trustlens_config", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("backend_base_url", repository.getBaseUrl()).apply()
            }
        } catch (_: Exception) {
            // Ignored in unit testing where application context is not present
        }
        _uiState.update {
            it.copy(
                currentBaseUrl = repository.getBaseUrl(),
                isSettingsVisible = false
            )
        }
        checkBackendHealth()
    }

    private fun extractUrlFromText(text: String): String? {
        val trimmed = text.trim()
        val urlRegex = "(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+)".toRegex()
        val match = urlRegex.find(trimmed)
        return match?.value ?: if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else null
    }

    private fun extractAllUrlsFromText(text: String): List<String> {
        val urlRegex = "(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+)".toRegex()
        return urlRegex.findAll(text).map { it.value }.toList()
    }

    private fun containsMessageLures(text: String): Boolean {
        val lures = listOf("otp", "code", "customs", "delivery", "usps", "wire", "invoice", "urgent", "suspended", "account", "bank")
        val lower = text.lowercase()
        return lures.any { lower.contains(it) }
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
