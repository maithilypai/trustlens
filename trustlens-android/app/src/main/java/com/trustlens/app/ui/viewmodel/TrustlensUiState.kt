package com.trustlens.app.ui.viewmodel

import com.trustlens.app.data.model.AnalyzeResponse
import com.trustlens.app.data.model.ComparisonDemoResponse
import com.trustlens.app.data.model.MessageAnalyzeResponse
import com.trustlens.app.data.model.QrAnalyzeResponse
import com.trustlens.app.data.model.RiskCategory
import com.trustlens.app.data.model.ScreenDestination

/**
 * State representing the TRUSTLENS user interface across all security modules.
 */
data class TrustlensUiState(
    // Active navigation screen
    val currentScreen: ScreenDestination = ScreenDestination.HOME,

    // Backend Connectivity Status ("online", "offline", "checking")
    val backendStatus: String = "checking",
    val backendVersion: String? = null,
    val isThreatIntelActive: Boolean = false,

    // URL Quick Scanner
    val urlInput: String = "",
    val isLoading: Boolean = false,
    val result: AnalyzeResponse? = null,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val isWarningDialogVisible: Boolean = false,

    // Safe Browser Module
    val browserUrlInput: String = "https://",
    val browserActiveUrl: String? = null,
    val isBrowserVerifying: Boolean = false,
    val browserBlockedThreat: AnalyzeResponse? = null,
    val browserCautionMessage: String? = null,
    val browserDisclaimerVisible: Boolean = true,

    // SMS & Message Analysis Module
    val messageInput: String = "",
    val messageSenderInput: String = "",
    val isMessageLoading: Boolean = false,
    val messageResult: MessageAnalyzeResponse? = null,
    val messageErrorMessage: String? = null,

    // QR Code / Quishing Inspection Module
    val qrPayloadInput: String = "",
    val isQrLoading: Boolean = false,
    val qrResult: QrAnalyzeResponse? = null,
    val qrErrorMessage: String? = null,

    // Educational Demos & Comparison
    val isDemoPickerVisible: Boolean = false,
    val isComparisonLoading: Boolean = false,
    val comparisonResult: ComparisonDemoResponse? = null,

    // Settings
    val isSettingsVisible: Boolean = false,
    val currentBaseUrl: String = ""
) {
    val isThreatConfirmed: Boolean
        get() = result?.assessment == RiskCategory.CONFIRMED_THREAT

    val hasHighRiskIndicators: Boolean
        get() = result?.assessment == RiskCategory.CONFIRMED_THREAT ||
                result?.assessment == RiskCategory.SUSPICIOUS_INDICATORS
}
