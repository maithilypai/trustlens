package com.trustlens.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Categorized assessment outcome combining local heuristics and threat intelligence.
 */
enum class RiskCategory {
    @SerializedName("confirmed_threat")
    CONFIRMED_THREAT,

    @SerializedName("suspicious_indicators")
    SUSPICIOUS_INDICATORS,

    @SerializedName("no_known_threat")
    NO_KNOWN_THREAT,

    @SerializedName("inconclusive")
    INCONCLUSIVE
}

/**
 * Severity classification for individual heuristic findings.
 */
enum class SeverityLevel {
    @SerializedName("info")
    INFO,

    @SerializedName("low")
    LOW,

    @SerializedName("medium")
    MEDIUM,

    @SerializedName("high")
    HIGH,

    @SerializedName("critical")
    CRITICAL
}

/**
 * Granular operational status of external threat intelligence lookups.
 */
enum class ThreatIntelStatus {
    @SerializedName("confirmed_threat")
    CONFIRMED_THREAT,

    @SerializedName("no_threat_found")
    NO_THREAT_FOUND,

    @SerializedName("service_unavailable")
    SERVICE_UNAVAILABLE,

    @SerializedName("not_configured")
    NOT_CONFIGURED,

    @SerializedName("timeout")
    TIMEOUT,

    @SerializedName("failed")
    FAILED
}

/**
 * Supported demonstration scenarios for UI preview.
 */
enum class DemoScenario(val displayName: String, val description: String) {
    @SerializedName("threat_detected")
    THREAT_DETECTED("Threat Detected (Phishing)", "Simulates an active phishing & malware detection match."),

    @SerializedName("scam_link")
    SCAM_LINK("Deceptive Brand Phishing", "Simulates lookalike brand domain targeting banking credentials."),

    @SerializedName("parcel_sms")
    PARCEL_SMS("Postal Fee Smishing", "Simulates urgent postal customs fee scam with deceptive link."),

    @SerializedName("otp_theft")
    OTP_THEFT("Bank Wire OTP Theft", "Simulates urgent bank wire transfer social engineering lure."),

    @SerializedName("fake_payment")
    FAKE_PAYMENT("Fake Supplier Invoice", "Simulates urgent supplier invoice redirection scam."),

    @SerializedName("qr_scam")
    QR_SCAM("Quishing / QR Trap", "Simulates deceptive payment QR sticker overlay on physical signage."),

    @SerializedName("fraud_call")
    FRAUD_CALL("Fraud Department Alert", "Simulates bank security department impersonation lure."),

    @SerializedName("no_known_threat")
    NO_KNOWN_THREAT("No Known Threat", "Simulates clean heuristics and verified reputation check."),

    @SerializedName("inconclusive")
    INCONCLUSIVE("Inconclusive", "Simulates unverified site when threat intelligence is unreachable."),

    @SerializedName("comparison")
    COMPARISON("Deceptive vs Legitimate", "Side-by-side contrast of phishing lookalike vs benign test domain.")
}

/**
 * Navigation destinations across TRUSTLENS application screens.
 */
enum class ScreenDestination {
    HOME,
    SAFE_BROWSER,
    MESSAGE_ANALYZER,
    QR_INSPECTOR,
    DEMOS,
    PRIVACY_INFO,
    RESULT
}

/**
 * Detailed structural attributes extracted during URL validation.
 */
data class UrlValidationDetails(
    @SerializedName("valid") val valid: Boolean = false,
    @SerializedName("scheme") val scheme: String = "",
    @SerializedName("hostname") val hostname: String = "",
    @SerializedName("normalized_url") val normalizedUrl: String = "",
    @SerializedName("port") val port: Int? = null,
    @SerializedName("path") val path: String = "/",
    @SerializedName("is_ip_address") val isIpAddress: Boolean = false,
    @SerializedName("is_private_ip") val isPrivateIp: Boolean = false,
    @SerializedName("is_loopback") val isLoopback: Boolean = false,
    @SerializedName("is_punycode") val isPunycode: Boolean = false,
    @SerializedName("unicode_hostname") val unicodeHostname: String? = null
)

/**
 * An explainable heuristic indicator detected by local inspection.
 */
data class Finding(
    @SerializedName("rule_id") val ruleId: String,
    @SerializedName("title") val title: String,
    @SerializedName("severity") val severity: SeverityLevel,
    @SerializedName("evidence") val evidence: String,
    @SerializedName("explanation") val explanation: String,
    @SerializedName("recommendation") val recommendation: String
)

/**
 * Operational outcome of external threat intelligence lookup.
 */
data class ThreatIntelligenceResult(
    @SerializedName("status") val status: ThreatIntelStatus,
    @SerializedName("source") val source: String? = null,
    @SerializedName("threat_types") val threatTypes: List<String> = emptyList(),
    @SerializedName("checked_at") val checkedAt: String? = null,
    @SerializedName("details") val details: String? = null
)

/**
 * Request payload for URL security analysis.
 */
data class UrlAnalyzeRequest(
    @SerializedName("url") val url: String
)

/**
 * Request payload for a controlled demonstration scenario.
 */
data class DemoAnalyzeRequest(
    @SerializedName("scenario") val scenario: DemoScenario,
    @SerializedName("url") val url: String? = null
)

/**
 * Comprehensive analysis response returned by TRUSTLENS API.
 */
data class AnalyzeResponse(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("assessment") val assessment: RiskCategory,
    @SerializedName("is_simulated") val isSimulated: Boolean = false,
    @SerializedName("url_validation") val urlValidation: UrlValidationDetails,
    @SerializedName("findings") val findings: List<Finding> = emptyList(),
    @SerializedName("threat_intelligence") val threatIntelligence: ThreatIntelligenceResult,
    @SerializedName("summary") val summary: String,
    @SerializedName("recommended_actions") val recommendedActions: List<String> = emptyList()
)

/**
 * Backend health check response.
 */
data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("app") val app: String,
    @SerializedName("version") val version: String,
    @SerializedName("threat_intel_configured") val threatIntelConfigured: Boolean,
    @SerializedName("timestamp") val timestamp: String
)

/**
 * Structured API error response for validation or network errors.
 */
data class ApiErrorResponse(
    @SerializedName("error") val error: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("details") val details: Any? = null
)

/**
 * Request payload for SMS or suspicious message text inspection.
 */
data class MessageAnalyzeRequest(
    @SerializedName("message") val message: String,
    @SerializedName("sender") val sender: String? = null
)

/**
 * Comprehensive analysis report for an SMS or text message.
 */
data class MessageAnalyzeResponse(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("assessment") val assessment: RiskCategory,
    @SerializedName("is_simulated") val isSimulated: Boolean = false,
    @SerializedName("message_length") val messageLength: Int = 0,
    @SerializedName("detected_urls") val detectedUrls: List<String> = emptyList(),
    @SerializedName("findings") val findings: List<Finding> = emptyList(),
    @SerializedName("embedded_url_analyses") val embeddedUrlAnalyses: List<Any> = emptyList(),
    @SerializedName("summary") val summary: String,
    @SerializedName("recommended_actions") val recommendedActions: List<String> = emptyList()
)

/**
 * Request payload for decoded QR code payload string inspection.
 */
data class QrAnalyzeRequest(
    @SerializedName("payload") val payload: String
)

/**
 * Comprehensive analysis report for decoded QR code payload.
 */
data class QrAnalyzeResponse(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("payload_type") val payloadType: String,
    @SerializedName("assessment") val assessment: RiskCategory,
    @SerializedName("is_simulated") val isSimulated: Boolean = false,
    @SerializedName("raw_payload") val rawPayload: String,
    @SerializedName("findings") val findings: List<Finding> = emptyList(),
    @SerializedName("url_analysis") val urlAnalysis: Any? = null,
    @SerializedName("summary") val summary: String,
    @SerializedName("recommended_actions") val recommendedActions: List<String> = emptyList()
)

/**
 * Side-by-side comparison response contrasting deceptive phishing vs benign test domain.
 */
data class ComparisonDemoResponse(
    @SerializedName("is_simulated") val isSimulated: Boolean = true,
    @SerializedName("simulated_threat") val simulatedThreat: AnalyzeResponse,
    @SerializedName("harmless_url") val harmlessUrl: AnalyzeResponse,
    @SerializedName("comparison_summary") val comparisonSummary: String,
    @SerializedName("key_differences") val keyDifferences: List<String> = emptyList()
)
