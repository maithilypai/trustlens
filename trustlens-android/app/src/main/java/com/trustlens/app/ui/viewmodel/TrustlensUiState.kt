package com.trustlens.app.ui.viewmodel

import com.trustlens.app.data.model.AnalyzeResponse
import com.trustlens.app.data.model.RiskCategory

/**
 * State representing the TRUSTLENS user interface.
 */
data class TrustlensUiState(
    val urlInput: String = "",
    val isLoading: Boolean = false,
    val result: AnalyzeResponse? = null,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val isWarningDialogVisible: Boolean = false,
    val isDemoPickerVisible: Boolean = false,
    val isSettingsVisible: Boolean = false,
    val currentBaseUrl: String = ""
) {
    val isThreatConfirmed: Boolean
        get() = result?.assessment == RiskCategory.CONFIRMED_THREAT

    val hasHighRiskIndicators: Boolean
        get() = result?.assessment == RiskCategory.CONFIRMED_THREAT ||
                result?.assessment == RiskCategory.SUSPICIOUS_INDICATORS
}
