package com.trustlens.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.trustlens.app.data.model.AnalyzeResponse
import com.trustlens.app.data.model.ScreenDestination
import com.trustlens.app.ui.components.FindingItem
import com.trustlens.app.ui.theme.CyanAccent
import com.trustlens.app.ui.theme.NavyBackground
import com.trustlens.app.ui.theme.NavyBorder
import com.trustlens.app.ui.theme.NavyCardElevated
import com.trustlens.app.ui.theme.NavySurface
import com.trustlens.app.ui.theme.NavySurfaceVariant
import com.trustlens.app.ui.theme.StatusCleanEmerald
import com.trustlens.app.ui.theme.StatusCleanEmeraldBg
import com.trustlens.app.ui.theme.StatusThreatRed
import com.trustlens.app.ui.theme.StatusThreatRedBg
import com.trustlens.app.ui.theme.StatusThreatRedBorder
import com.trustlens.app.ui.theme.StatusWarningAmber
import com.trustlens.app.ui.theme.StatusWarningAmberBg
import com.trustlens.app.ui.theme.TextMuted
import com.trustlens.app.ui.theme.TextPrimary
import com.trustlens.app.ui.theme.TextSecondary
import com.trustlens.app.ui.viewmodel.TrustlensUiState
import com.trustlens.app.ui.viewmodel.TrustlensViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SafeBrowserScreen(
    viewModel: TrustlensViewModel,
    uiState: TrustlensUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Intercept hardware / gesture back button
    BackHandler {
        if (uiState.browserBlockedThreat != null) {
            viewModel.dismissBrowserThreatBlock()
        } else if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            viewModel.navigateTo(ScreenDestination.HOME)
        }
    }

    Scaffold(
        containerColor = NavyBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurface)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, NavyBorder))
            ) {
                // Browser Controls & Address Bar Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenDestination.HOME) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Return Home",
                            tint = CyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Address Bar
                    OutlinedTextField(
                        value = uiState.browserUrlInput,
                        onValueChange = { viewModel.onBrowserUrlChanged(it) },
                        placeholder = {
                            Text(
                                text = "Enter URL (https://...)",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.browserUrlInput.startsWith("https://")) Icons.Default.Lock else Icons.Default.Search,
                                contentDescription = null,
                                tint = if (uiState.browserUrlInput.startsWith("https://")) StatusCleanEmerald else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.browserUrlInput.isNotEmpty() && uiState.browserUrlInput != "https://") {
                                IconButton(
                                    onClick = { viewModel.onBrowserUrlChanged("https://") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                keyboardController?.hide()
                                viewModel.navigateSafeBrowser()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = NavySurfaceVariant,
                            unfocusedContainerColor = NavySurfaceVariant,
                            cursorColor = CyanAccent,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = NavyBorder
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Go / Verify Button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.navigateSafeBrowser()
                        },
                        enabled = !uiState.isBrowserVerifying,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        if (uiState.isBrowserVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NavyBackground,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Go",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NavyBackground
                                )
                            )
                        }
                    }
                }

                // Verification in Progress Indicator
                if (uiState.isBrowserVerifying) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = CyanAccent,
                        trackColor = NavySurfaceVariant
                    )
                }

                // Sub-Bar: Navigation Controls & Safety Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { webViewRef?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (canGoBack) TextPrimary else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (canGoForward) TextPrimary else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { webViewRef?.reload() },
                            enabled = uiState.browserActiveUrl != null,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = if (uiState.browserActiveUrl != null) TextSecondary else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Security Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (uiState.browserBlockedThreat != null) StatusThreatRedBg
                                else if (uiState.browserCautionMessage != null) StatusWarningAmberBg
                                else if (uiState.browserActiveUrl != null) StatusCleanEmeraldBg
                                else NavySurfaceVariant
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.browserBlockedThreat != null) Icons.Default.Dangerous
                                else if (uiState.browserCautionMessage != null) Icons.Default.GppMaybe
                                else if (uiState.browserActiveUrl != null) Icons.Default.GppGood
                                else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (uiState.browserBlockedThreat != null) StatusThreatRed
                                else if (uiState.browserCautionMessage != null) StatusWarningAmber
                                else if (uiState.browserActiveUrl != null) StatusCleanEmerald
                                else TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.browserBlockedThreat != null) "THREAT BLOCKED"
                                else if (uiState.browserCautionMessage != null) "UNVERIFIED"
                                else if (uiState.browserActiveUrl != null) "CHECKS PASSED"
                                else "SAFE BROWSER ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.browserBlockedThreat != null) StatusThreatRed
                                    else if (uiState.browserCautionMessage != null) StatusWarningAmber
                                    else if (uiState.browserActiveUrl != null) StatusCleanEmerald
                                    else TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Case 1: High Risk Threat Block Screen
            if (uiState.browserBlockedThreat != null) {
                BlockedThreatScreen(
                    threat = uiState.browserBlockedThreat,
                    onReturnToSafety = { viewModel.dismissBrowserThreatBlock() },
                    onCopyUrl = {
                        clipboardManager.setText(AnnotatedString(uiState.browserBlockedThreat.urlValidation.normalizedUrl))
                        Toast.makeText(context, "URL copied for reporting", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            // Case 2: Active Browser View
            else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Caution Banner for Inconclusive / Unverified destinations
                    if (uiState.browserCautionMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2E2211))
                                .border(1.dp, StatusWarningAmber.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = "Caution",
                                        tint = StatusWarningAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uiState.browserCautionMessage,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = StatusWarningAmber,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.dismissBrowserCaution() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = StatusWarningAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Persistent Disclaimer Pill
                    if (uiState.browserActiveUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NavySurfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "No known threat indicators found. Automated checks do not guarantee 100% safety.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // WebView Component
                    if (uiState.browserActiveUrl != null) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.setSupportZoom(true)
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            canGoBack = view?.canGoBack() == true
                                            canGoForward = view?.canGoForward() == true
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            canGoBack = view?.canGoBack() == true
                                            canGoForward = view?.canGoForward() == true
                                        }

                                        // Strict security interceptor: Inspect destination before navigation!
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val destUrl = request?.url?.toString() ?: return false
                                            val lowerDest = destUrl.lowercase()

                                            // Reject unsupported schemes
                                            if (!lowerDest.startsWith("http://") && !lowerDest.startsWith("https://")) {
                                                Toast.makeText(ctx, "Blocked unsupported scheme: $destUrl", Toast.LENGTH_SHORT).show()
                                                return true
                                            }

                                            // Trigger pre-navigation backend verification
                                            viewModel.navigateSafeBrowser(destUrl)
                                            return true // Prevent WebView from loading automatically until verified!
                                        }
                                    }
                                    webViewRef = this
                                }
                            },
                            update = { webView ->
                                webViewRef = webView
                                if (uiState.browserActiveUrl != webView.url) {
                                    webView.loadUrl(uiState.browserActiveUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Empty browser landing state
                        BrowserLandingPlaceholder(
                            onSelectPreset = { preset ->
                                viewModel.onBrowserUrlChanged(preset)
                                viewModel.navigateSafeBrowser(preset)
                            }
                        )
                    }
                }
            }

            // Verification Overlay
            if (uiState.isBrowserVerifying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NavyBackground.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = CyanAccent,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ANALYZING DESTINATION",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = CyanAccent
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Evaluating structural heuristics and querying threat intelligence before allowing navigation.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedThreatScreen(
    threat: AnalyzeResponse,
    onReturnToSafety: () -> Unit,
    onCopyUrl: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .padding(20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Large Warning Shield Emblem
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(StatusThreatRedBg)
                .border(2.dp, StatusThreatRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Dangerous,
                contentDescription = "Threat Detected",
                tint = StatusThreatRed,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "DANGER: HIGH-RISK DESTINATION BLOCKED",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = StatusThreatRed,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "TRUSTLENS blocked this page from loading in your browser. Navigating to this website puts your personal information, passwords, and device at risk.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Target URL Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, StatusThreatRedBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "BLOCKED DESTINATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = StatusThreatRed,
                        fontSize = 10.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = threat.urlValidation.normalizedUrl,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary of Threat
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "EXECUTIVE SUMMARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = threat.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        // Findings Breakdown
        if (threat.findings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                threat.findings.forEach { finding ->
                    FindingItem(finding = finding)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons: Safe return & Copy for reporting (NO bypass!)
        Button(
            onClick = onReturnToSafety,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StatusThreatRed),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Return to Safety",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onCopyUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Copy URL for Phishing Report",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun BrowserLandingPlaceholder(
    onSelectPreset: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(NavySurfaceVariant)
                .border(1.5.dp, CyanAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "TRUSTLENS Safe Browser",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Pre-navigation threat inspection safeguards your phone from zero-click scams, deceptive lookalike domains, and phishing links before pages load.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TRY SAMPLE PRESETS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        BrowserPresetButton(
            label = "Phishing Lookalike Link (Blocked)",
            url = "https://paypal.com.account-update.xyz/login",
            color = StatusThreatRed,
            onClick = { onSelectPreset("https://paypal.com.account-update.xyz/login") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        BrowserPresetButton(
            label = "Legitimate Domain (Passes Checks)",
            url = "https://example.com",
            color = StatusCleanEmerald,
            onClick = { onSelectPreset("https://example.com") }
        )
    }
}

@Composable
private fun BrowserPresetButton(
    label: String,
    url: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCardElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
