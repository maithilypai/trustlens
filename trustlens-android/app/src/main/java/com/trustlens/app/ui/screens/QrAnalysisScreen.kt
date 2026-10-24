package com.trustlens.app.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustlens.app.data.model.QrAnalyzeResponse
import com.trustlens.app.data.model.RiskCategory
import com.trustlens.app.data.model.ScreenDestination
import com.trustlens.app.ui.components.FindingItem
import com.trustlens.app.ui.theme.BlueAccent
import com.trustlens.app.ui.theme.CyanAccent
import com.trustlens.app.ui.theme.NavyBackground
import com.trustlens.app.ui.theme.NavyBorder
import com.trustlens.app.ui.theme.NavyCardElevated
import com.trustlens.app.ui.theme.NavySurface
import com.trustlens.app.ui.theme.NavySurfaceVariant
import com.trustlens.app.ui.theme.StatusCleanEmerald
import com.trustlens.app.ui.theme.StatusThreatRed
import com.trustlens.app.ui.theme.StatusWarningAmber
import com.trustlens.app.ui.theme.TextMuted
import com.trustlens.app.ui.theme.TextPrimary
import com.trustlens.app.ui.theme.TextSecondary
import com.trustlens.app.ui.viewmodel.TrustlensUiState
import com.trustlens.app.ui.viewmodel.TrustlensViewModel

@Composable
fun QrAnalysisScreen(
    viewModel: TrustlensViewModel,
    uiState: TrustlensUiState,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = NavyBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurface)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, NavyBorder))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(ScreenDestination.HOME) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "QR CODE & QUISHING INSPECTOR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Pre-execution analysis for decoded QR payloads",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DECODED QR PAYLOAD STRING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                letterSpacing = 1.sp
                            )
                        )

                        OutlinedButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    viewModel.onQrInputChanged(clip)
                                    Toast.makeText(context, "Pasted QR payload", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uiState.qrPayloadInput,
                        onValueChange = { viewModel.onQrInputChanged(it) },
                        placeholder = {
                            Text(
                                text = "Paste decoded QR string (e.g., https://fake-menu.xyz/pay or upi://pay?pa=attacker@bank)",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        minLines = 3,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = NavySurfaceVariant,
                            unfocusedContainerColor = NavySurfaceVariant,
                            cursorColor = CyanAccent,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = NavyBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.analyzeQrPayload()
                        },
                        enabled = !uiState.isQrLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState.isQrLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NavyBackground,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Analyzing Payload...",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NavyBackground
                                )
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = NavyBackground,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Inspect QR Code Payload",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NavyBackground
                                )
                            )
                        }
                    }

                    if (uiState.qrPayloadInput.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.clearQr() },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Text("Clear", color = TextSecondary)
                        }
                    }
                }
            }

            // Error notice
            if (!uiState.qrErrorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2E1719))
                        .border(1.dp, StatusThreatRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = StatusThreatRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = uiState.qrErrorMessage, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Sample Presets
            if (uiState.qrResult == null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "SAMPLE QR SCENARIOS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                QrPresetCard(
                    title = "Restaurant Table Quishing Sticker",
                    scheme = "URL Scheme",
                    payload = "https://restaurant-menu.xyz/pay-invoice?tip=15",
                    icon = Icons.Default.QrCodeScanner,
                    onClick = { viewModel.loadSampleQr("https://restaurant-menu.xyz/pay-invoice?tip=15") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                QrPresetCard(
                    title = "Direct UPI Payment Transfer",
                    scheme = "Payment Intent",
                    payload = "upi://pay?pa=scammer99@ybl&pn=ParkingLot&am=50.00&cu=INR",
                    icon = Icons.Default.Payment,
                    onClick = { viewModel.loadSampleQr("upi://pay?pa=scammer99@ybl&pn=ParkingLot&am=50.00&cu=INR") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                QrPresetCard(
                    title = "Rogue Wi-Fi Provisioning",
                    scheme = "Wi-Fi Config",
                    payload = "WIFI:S:Free_Airport_WiFi;T:nopass;;",
                    icon = Icons.Default.Wifi,
                    onClick = { viewModel.loadSampleQr("WIFI:S:Free_Airport_WiFi;T:nopass;;") }
                )
            }

            // Analysis Result Presentation
            if (uiState.qrResult != null) {
                Spacer(modifier = Modifier.height(20.dp))
                QrResultCard(result = uiState.qrResult)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QrPresetCard(
    title: String,
    scheme: String,
    payload: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCardElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NavySurfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = scheme, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = payload,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextSecondary, fontSize = 11.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QrResultCard(result: QrAnalyzeResponse) {
    val (statusLabel, statusColor) = when (result.assessment) {
        RiskCategory.CONFIRMED_THREAT -> "CONFIRMED QUISHING THREAT" to StatusThreatRed
        RiskCategory.SUSPICIOUS_INDICATORS -> "SUSPICIOUS QR PAYLOAD" to StatusWarningAmber
        RiskCategory.NO_KNOWN_THREAT -> "NO SUSPICIOUS ANOMALIES" to StatusCleanEmerald
        RiskCategory.INCONCLUSIVE -> "INCONCLUSIVE EVALUATION" to CyanAccent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = statusColor)
                    )
                    Text(
                        text = "Payload classification: ${result.payloadType.uppercase()}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Raw payload box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavySurfaceVariant)
                    .padding(10.dp)
            ) {
                Column {
                    Text(text = "RAW PAYLOAD STRING", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = result.rawPayload, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextPrimary))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Executive Summary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavyCardElevated)
                    .padding(12.dp)
            ) {
                Text(text = result.summary, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, lineHeight = 18.sp))
            }

            // Findings Breakdown
            if (result.findings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "QUISHING HEURISTIC FINDINGS (${result.findings.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.8.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.findings.forEach { finding ->
                        FindingItem(finding = finding)
                    }
                }
            }

            // Recommendations
            if (result.recommendedActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "RECOMMENDED ACTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.8.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                result.recommendedActions.forEach { action ->
                    Text(text = "• $action", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, lineHeight = 18.sp))
                }
            }
        }
    }
}
