package com.trustlens.app.ui.viewmodel

import com.trustlens.app.data.model.AnalyzeResponse
import com.trustlens.app.data.model.ComparisonDemoResponse
import com.trustlens.app.data.model.DemoScenario
import com.trustlens.app.data.model.HealthResponse
import com.trustlens.app.data.model.MessageAnalyzeResponse
import com.trustlens.app.data.model.QrAnalyzeResponse
import com.trustlens.app.data.model.RiskCategory
import com.trustlens.app.data.model.ScreenDestination
import com.trustlens.app.data.model.ThreatIntelStatus
import com.trustlens.app.data.model.ThreatIntelligenceResult
import com.trustlens.app.data.model.UrlValidationDetails
import com.trustlens.app.data.remote.NetworkResult
import com.trustlens.app.data.repository.TrustlensRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrustlensViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeRepository : TrustlensRepository {
        var returnError = false
        var throwException = false
        var cannedResponse: AnalyzeResponse? = null
        var lastAnalyzedUrl: String? = null
        var lastDemoScenario: DemoScenario? = null
        var lastMessageText: String? = null
        var lastQrPayload: String? = null
        private var currentBaseUrl: String = "http://10.0.2.2:8000/"

        override suspend fun analyzeUrl(url: String): NetworkResult<AnalyzeResponse> {
            if (throwException) {
                throw RuntimeException("Fatal low-level exception")
            }
            lastAnalyzedUrl = url
            return if (returnError) {
                NetworkResult.Error("Simulated network timeout", code = "TIMEOUT")
            } else {
                NetworkResult.Success(cannedResponse ?: createDefaultResponse())
            }
        }

        override suspend fun runDemo(scenario: DemoScenario, customUrl: String?): NetworkResult<AnalyzeResponse> {
            lastDemoScenario = scenario
            return if (returnError) {
                NetworkResult.Error("Demo failure", code = "DEMO_ERR")
            } else {
                NetworkResult.Success(cannedResponse ?: createDefaultResponse(isSimulated = true))
            }
        }

        override suspend fun checkHealth(): NetworkResult<HealthResponse> {
            return NetworkResult.Success(
                HealthResponse(
                    status = "healthy",
                    app = "TRUSTLENS API",
                    version = "1.0.0",
                    threatIntelConfigured = false,
                    timestamp = "2026-10-22T00:00:00Z"
                )
            )
        }

        override suspend fun analyzeMessage(text: String, sender: String?): NetworkResult<MessageAnalyzeResponse> {
            lastMessageText = text
            if (returnError) {
                return NetworkResult.Error("Message inspection failed", code = "MSG_ERR")
            }
            return NetworkResult.Success(
                MessageAnalyzeResponse(
                    requestId = "fake-msg-123",
                    assessment = RiskCategory.SUSPICIOUS_INDICATORS,
                    isSimulated = false,
                    messageLength = text.length,
                    detectedUrls = listOf("https://fake-link.test"),
                    findings = emptyList(),
                    embeddedUrlAnalyses = emptyList(),
                    summary = "Suspicious parcel message lure detected",
                    recommendedActions = listOf("Do not click links")
                )
            )
        }

        override suspend fun analyzeQr(payload: String): NetworkResult<QrAnalyzeResponse> {
            lastQrPayload = payload
            if (returnError) {
                return NetworkResult.Error("QR inspection failed", code = "QR_ERR")
            }
            return NetworkResult.Success(
                QrAnalyzeResponse(
                    requestId = "fake-qr-123",
                    payloadType = "url",
                    assessment = RiskCategory.CONFIRMED_THREAT,
                    isSimulated = false,
                    rawPayload = payload,
                    findings = emptyList(),
                    urlAnalysis = createDefaultResponse(),
                    summary = "Quishing attack payload detected",
                    recommendedActions = listOf("Do not scan untrusted stickers")
                )
            )
        }

        override suspend fun getComparisonDemo(): NetworkResult<ComparisonDemoResponse> {
            if (returnError) {
                return NetworkResult.Error("Comparison failed", code = "CMP_ERR")
            }
            return NetworkResult.Success(
                ComparisonDemoResponse(
                    isSimulated = true,
                    simulatedThreat = createDefaultResponse(isSimulated = true),
                    harmlessUrl = createDefaultResponse(isSimulated = true),
                    comparisonSummary = "Comparison insights",
                    keyDifferences = listOf("Deceptive domain substitutes characters")
                )
            )
        }

        override fun updateBaseUrl(newBaseUrl: String) {
            currentBaseUrl = newBaseUrl
        }

        override fun getBaseUrl(): String = currentBaseUrl

        private fun createDefaultResponse(isSimulated: Boolean = false): AnalyzeResponse {
            return AnalyzeResponse(
                requestId = "fake-123",
                assessment = RiskCategory.NO_KNOWN_THREAT,
                isSimulated = isSimulated,
                urlValidation = UrlValidationDetails(
                    valid = true,
                    scheme = "https",
                    hostname = "example.com",
                    normalizedUrl = "https://example.com/"
                ),
                findings = emptyList(),
                threatIntelligence = ThreatIntelligenceResult(
                    status = ThreatIntelStatus.NO_THREAT_FOUND,
                    source = "Test Provider"
                ),
                summary = "Clean site",
                recommendedActions = listOf("Verify HTTPS")
            )
        }
    }

    private lateinit var fakeRepository: FakeRepository
    private lateinit var viewModel: TrustlensViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRepository()
        viewModel = TrustlensViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUrlInputManagement() {
        viewModel.onUrlInputChanged("https://bank-verify.test")
        assertEquals("https://bank-verify.test", viewModel.uiState.value.urlInput)

        viewModel.clearInput()
        assertEquals("", viewModel.uiState.value.urlInput)

        viewModel.pasteFromClipboard("  https://secure.site.com  ")
        assertEquals("https://secure.site.com", viewModel.uiState.value.urlInput)
    }

    @Test
    fun testEmptyUrlInputShowsClientError() = runTest {
        viewModel.onUrlInputChanged("   ")
        viewModel.analyzeUrl()

        val state = viewModel.uiState.value
        assertEquals("EMPTY_INPUT", state.errorCode)
        assertNotNull(state.errorMessage)
        assertFalse(state.isLoading)
        assertNull(state.result)
    }

    @Test
    fun testSuccessfulAnalysisEmitsResult() = runTest {
        viewModel.onUrlInputChanged("https://example.com")
        viewModel.analyzeUrl()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.result)
        assertEquals(RiskCategory.NO_KNOWN_THREAT, state.result?.assessment)
        assertFalse(state.isWarningDialogVisible)
    }

    @Test
    fun testConfirmedThreatShowsWarningDialogAutomatically() = runTest {
        fakeRepository.cannedResponse = AnalyzeResponse(
            requestId = "threat-456",
            assessment = RiskCategory.CONFIRMED_THREAT,
            isSimulated = false,
            urlValidation = UrlValidationDetails(
                valid = true,
                scheme = "https",
                hostname = "phishing-bank.test",
                normalizedUrl = "https://phishing-bank.test/login"
            ),
            findings = emptyList(),
            threatIntelligence = ThreatIntelligenceResult(
                status = ThreatIntelStatus.CONFIRMED_THREAT,
                source = "Safe Browsing"
            ),
            summary = "Phishing detected",
            recommendedActions = listOf("Do not enter passwords")
        )

        viewModel.onUrlInputChanged("https://phishing-bank.test/login")
        viewModel.analyzeUrl()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(RiskCategory.CONFIRMED_THREAT, state.result?.assessment)
        assertTrue(state.isWarningDialogVisible)

        // Dismiss warning dialog
        viewModel.dismissWarningDialog()
        assertFalse(viewModel.uiState.value.isWarningDialogVisible)
        assertNotNull(viewModel.uiState.value.result)
    }

    @Test
    fun testNetworkErrorEmitsErrorState() = runTest {
        fakeRepository.returnError = true
        viewModel.onUrlInputChanged("https://example.com")
        viewModel.analyzeUrl()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.result)
        assertEquals("TIMEOUT", state.errorCode)
        assertTrue(state.errorMessage!!.contains("timeout"))
    }

    @Test
    fun testRunDemoScenarioInvokesBackendDemo() = runTest {
        viewModel.runDemoScenario(DemoScenario.THREAT_DETECTED)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(DemoScenario.THREAT_DETECTED, fakeRepository.lastDemoScenario)
        assertNotNull(state.result)
        assertTrue(state.result!!.isSimulated)
    }

    @Test
    fun testUnexpectedExceptionLeavesLoadingFalseAndEmitsError() = runTest {
        fakeRepository.throwException = true
        viewModel.onUrlInputChanged("https://crash-test.com")
        viewModel.analyzeUrl()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.result)
        assertEquals("UNEXPECTED_ERROR", state.errorCode)
        assertTrue(state.errorMessage!!.contains("Fatal low-level exception"))
    }

    @Test
    fun testNavigationModuleTransitions() {
        assertEquals(ScreenDestination.HOME, viewModel.uiState.value.currentScreen)

        viewModel.navigateTo(ScreenDestination.SAFE_BROWSER)
        assertEquals(ScreenDestination.SAFE_BROWSER, viewModel.uiState.value.currentScreen)

        viewModel.navigateTo(ScreenDestination.MESSAGE_ANALYZER)
        assertEquals(ScreenDestination.MESSAGE_ANALYZER, viewModel.uiState.value.currentScreen)

        viewModel.navigateTo(ScreenDestination.QR_INSPECTOR)
        assertEquals(ScreenDestination.QR_INSPECTOR, viewModel.uiState.value.currentScreen)

        viewModel.navigateTo(ScreenDestination.DEMOS)
        assertEquals(ScreenDestination.DEMOS, viewModel.uiState.value.currentScreen)

        viewModel.navigateTo(ScreenDestination.PRIVACY_INFO)
        assertEquals(ScreenDestination.PRIVACY_INFO, viewModel.uiState.value.currentScreen)

        viewModel.clearResult()
        assertEquals(ScreenDestination.HOME, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun testSafeBrowserRejectsUnsafeSchemes() {
        viewModel.navigateSafeBrowser("javascript:alert(1)")
        val state = viewModel.uiState.value
        assertNotNull(state.browserCautionMessage)
        assertTrue(state.browserCautionMessage!!.contains("Blocked unsafe scheme"))
        assertNull(state.browserActiveUrl)
    }

    @Test
    fun testSafeBrowserBlocksConfirmedThreats() = runTest {
        fakeRepository.cannedResponse = AnalyzeResponse(
            requestId = "threat-browser",
            assessment = RiskCategory.CONFIRMED_THREAT,
            isSimulated = false,
            urlValidation = UrlValidationDetails(
                valid = true,
                scheme = "https",
                hostname = "malicious-portal.test",
                normalizedUrl = "https://malicious-portal.test"
            ),
            findings = emptyList(),
            threatIntelligence = ThreatIntelligenceResult(
                status = ThreatIntelStatus.CONFIRMED_THREAT,
                source = "Safe Browsing"
            ),
            summary = "Phishing threat",
            recommendedActions = listOf("Do not visit")
        )

        viewModel.navigateSafeBrowser("https://malicious-portal.test")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isBrowserVerifying)
        assertNotNull(state.browserBlockedThreat)
        assertNull(state.browserActiveUrl)
    }

    @Test
    fun testMessageAnalysisExecutesAndEmitsFindings() = runTest {
        viewModel.onMessageInputChanged("USPS Alert: Your parcel is on hold. Pay fee at https://usps-redelivery.test")
        viewModel.analyzeMessage()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isMessageLoading)
        assertNotNull(state.messageResult)
        assertEquals(RiskCategory.SUSPICIOUS_INDICATORS, state.messageResult?.assessment)
        assertEquals("USPS Alert: Your parcel is on hold. Pay fee at https://usps-redelivery.test", fakeRepository.lastMessageText)
    }

    @Test
    fun testQrAnalysisExecutesAndDetectsQuishing() = runTest {
        viewModel.onQrInputChanged("https://quishing-overlay.test/login")
        viewModel.analyzeQrPayload()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isQrLoading)
        assertNotNull(state.qrResult)
        assertEquals(RiskCategory.CONFIRMED_THREAT, state.qrResult?.assessment)
        assertEquals("https://quishing-overlay.test/login", fakeRepository.lastQrPayload)
    }
}
