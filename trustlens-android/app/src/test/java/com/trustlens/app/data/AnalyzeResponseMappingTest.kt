package com.trustlens.app.data

import com.google.gson.Gson
import com.trustlens.app.data.model.AnalyzeResponse
import com.trustlens.app.data.model.ApiErrorResponse
import com.trustlens.app.data.model.DemoScenario
import com.trustlens.app.data.model.RiskCategory
import com.trustlens.app.data.model.SeverityLevel
import com.trustlens.app.data.model.ThreatIntelStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnalyzeResponseMappingTest {

    private lateinit var gson: Gson

    @Before
    fun setUp() {
        gson = Gson()
    }

    @Test
    fun testDeserializeAnalyzeResponseSuspiciousIndicators() {
        val json = """
        {
            "request_id": "test-req-1234",
            "assessment": "suspicious_indicators",
            "is_simulated": false,
            "url_validation": {
                "valid": true,
                "scheme": "https",
                "hostname": "paypal.com.verify-account.security-alert.xyz",
                "normalized_url": "https://paypal.com.verify-account.security-alert.xyz/signin",
                "port": null,
                "path": "/signin",
                "is_ip_address": false,
                "is_private_ip": false,
                "is_loopback": false,
                "is_punycode": false,
                "unicode_hostname": "paypal.com.verify-account.security-alert.xyz"
            },
            "findings": [
                {
                    "rule_id": "RULE_BRAND_IMPERSONATION",
                    "title": "Deceptive Brand Impersonation Indicator",
                    "severity": "high",
                    "evidence": "Brand 'paypal' detected in subdomain",
                    "explanation": "Attackers prepend brand names to mislead users",
                    "recommendation": "Do not enter passwords"
                }
            ],
            "threat_intelligence": {
                "status": "not_configured",
                "source": null,
                "threat_types": [],
                "checked_at": null,
                "details": "External threat intelligence is not configured."
            },
            "summary": "Local structural analysis detected high-severity suspicious indicators",
            "recommended_actions": [
                "Do not enter passwords"
            ]
        }
        """.trimIndent()

        val response = gson.fromJson(json, AnalyzeResponse::class.java)

        assertEquals("test-req-1234", response.requestId)
        assertEquals(RiskCategory.SUSPICIOUS_INDICATORS, response.assessment)
        assertFalse(response.isSimulated)

        // URL Validation
        assertTrue(response.urlValidation.valid)
        assertEquals("https", response.urlValidation.scheme)
        assertEquals("paypal.com.verify-account.security-alert.xyz", response.urlValidation.hostname)
        assertNull(response.urlValidation.port)
        assertFalse(response.urlValidation.isIpAddress)

        // Findings
        assertEquals(1, response.findings.size)
        val finding = response.findings[0]
        assertEquals("RULE_BRAND_IMPERSONATION", finding.ruleId)
        assertEquals(SeverityLevel.HIGH, finding.severity)
        assertEquals("Deceptive Brand Impersonation Indicator", finding.title)

        // Threat Intelligence
        assertEquals(ThreatIntelStatus.NOT_CONFIGURED, response.threatIntelligence.status)
        assertNull(response.threatIntelligence.source)

        // Recommendations
        assertEquals(1, response.recommendedActions.size)
        assertEquals("Do not enter passwords", response.recommendedActions[0])
    }

    @Test
    fun testDeserializeSimulatedDemoThreatDetectedResponse() {
        val json = """
        {
            "request_id": "demo-9876",
            "assessment": "confirmed_threat",
            "is_simulated": true,
            "url_validation": {
                "valid": true,
                "scheme": "https",
                "hostname": "secure-login.chase-verification.example.test",
                "normalized_url": "https://secure-login.chase-verification.example.test/auth-portal",
                "port": null,
                "path": "/auth-portal",
                "is_ip_address": false,
                "is_private_ip": false,
                "is_loopback": false,
                "is_punycode": false
            },
            "findings": [
                {
                    "rule_id": "RULE_BRAND_IMPERSONATION",
                    "title": "Deceptive Brand Impersonation Indicator",
                    "severity": "high",
                    "evidence": "Brand 'chase' detected",
                    "explanation": "Phishing simulation",
                    "recommendation": "Do not enter passwords"
                }
            ],
            "threat_intelligence": {
                "status": "confirmed_threat",
                "source": "TRUSTLENS Demonstration Fixture",
                "threat_types": ["SOCIAL_ENGINEERING", "MALWARE"],
                "checked_at": "2026-10-22T17:58:13.634289Z",
                "details": "DEMONSTRATION SIMULATION: Known phishing match."
            },
            "summary": "DEMONSTRATION SIMULATION: External threat intelligence verified target",
            "recommended_actions": [
                "Do not click this link"
            ]
        }
        """.trimIndent()

        val response = gson.fromJson(json, AnalyzeResponse::class.java)

        assertEquals("demo-9876", response.requestId)
        assertEquals(RiskCategory.CONFIRMED_THREAT, response.assessment)
        assertTrue(response.isSimulated)
        assertEquals(ThreatIntelStatus.CONFIRMED_THREAT, response.threatIntelligence.status)
        assertEquals(2, response.threatIntelligence.threatTypes.size)
        assertTrue(response.threatIntelligence.threatTypes.contains("SOCIAL_ENGINEERING"))
    }

    @Test
    fun testDeserializeApiValidationError() {
        val json = """
        {
            "error": "URL_VALIDATION_ERROR",
            "code": "UNSUPPORTED_SCHEME",
            "message": "Unsupported protocol scheme 'ftp'. TRUSTLENS only analyzes 'http://' and 'https://' URLs."
        }
        """.trimIndent()

        val error = gson.fromJson(json, ApiErrorResponse::class.java)

        assertEquals("URL_VALIDATION_ERROR", error.error)
        assertEquals("UNSUPPORTED_SCHEME", error.code)
        assertTrue(error.message!!.contains("ftp"))
    }

    @Test
    fun testDemoScenarioEnumValues() {
        assertEquals("threat_detected", gson.toJson(DemoScenario.THREAT_DETECTED).replace("\"", ""))
        assertEquals("no_known_threat", gson.toJson(DemoScenario.NO_KNOWN_THREAT).replace("\"", ""))
        assertEquals("inconclusive", gson.toJson(DemoScenario.INCONCLUSIVE).replace("\"", ""))
    }
}
