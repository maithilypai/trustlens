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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
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
import com.trustlens.app.data.model.MessageAnalyzeResponse
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
fun MessageAnalysisScreen(
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
                        text = "SMS & MESSAGE INSPECTOR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Social engineering, smishing & OTP theft detector",
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
                            text = "SUSPICIOUS MESSAGE BODY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                letterSpacing = 1.sp
                            )
                        )

                        // Quick Paste
                        OutlinedButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    viewModel.onMessageInputChanged(clip)
                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
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

                    // Message text area
                    OutlinedTextField(
                        value = uiState.messageInput,
                        onValueChange = { viewModel.onMessageInputChanged(it) },
                        placeholder = {
                            Text(
                                text = "Paste suspicious SMS, WhatsApp text, or email alert here (e.g., 'USPS: Unpaid customs fee of $2.15...')",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sender Shortcode / Phone (Optional)
                    OutlinedTextField(
                        value = uiState.messageSenderInput,
                        onValueChange = { viewModel.onMessageSenderChanged(it) },
                        placeholder = {
                            Text(
                                text = "Sender Shortcode or Number (optional, e.g. +18005550199)",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
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
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit Analysis Button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.analyzeMessage()
                        },
                        enabled = !uiState.isMessageLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState.isMessageLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NavyBackground,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Inspecting Message...",
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
                                "Inspect for Scam Lures",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NavyBackground
                                )
                            )
                        }
                    }

                    if (uiState.messageInput.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.clearMessage() },
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
            if (!uiState.messageErrorMessage.isNullOrBlank()) {
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
                        Text(text = uiState.messageErrorMessage, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Sample Presets
            if (uiState.messageResult == null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "TRY COMMON SCAM SAMPLES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                MessagePresetCard(
                    title = "Parcel Customs Fee Smishing",
                    preview = "USPS: Your package #US94829 is on hold due to an unpaid customs fee of $2.15. Confirm delivery details at https://usps-redelivery.xyz/pay",
                    onClick = {
                        viewModel.loadSampleMessage(
                            message = "USPS: Your package #US94829 is on hold due to an unpaid customs fee of $2.15. Confirm delivery details at https://usps-redelivery.xyz/pay",
                            sender = "+1 (800) 555-0199"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                MessagePresetCard(
                    title = "Urgent Bank Wire Transfer OTP",
                    preview = "CHASE ALERT: A wire transfer of $1,250.00 was requested. If this was NOT you, immediately verify your account at https://chase-security-cancel.xyz",
                    onClick = {
                        viewModel.loadSampleMessage(
                            message = "CHASE ALERT: A wire transfer of $1,250.00 was requested. If this was NOT you, immediately verify your account at https://chase-security-cancel.xyz",
                            sender = "72972"
                        )
                    }
                )
            }

            // Analysis Result Presentation
            if (uiState.messageResult != null) {
                Spacer(modifier = Modifier.height(20.dp))
                MessageResultCard(result = uiState.messageResult)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MessagePresetCard(
    title: String,
    preview: String,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = preview, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp), maxLines = 2)
        }
    }
}

@Composable
private fun MessageResultCard(result: MessageAnalyzeResponse) {
    val (statusLabel, statusColor) = when (result.assessment) {
        RiskCategory.CONFIRMED_THREAT -> "CONFIRMED THREAT MATCH" to StatusThreatRed
        RiskCategory.SUSPICIOUS_INDICATORS -> "SUSPICIOUS SOCIAL ENGINEERING LURES" to StatusWarningAmber
        RiskCategory.NO_KNOWN_THREAT -> "NO SUSPICIOUS INDICATORS FOUND" to StatusCleanEmerald
        RiskCategory.INCONCLUSIVE -> "INCONCLUSIVE EVALUATION" to CyanAccent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
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
                        text = "Message length: ${result.messageLength} chars • URLs detected: ${result.detectedUrls.size}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Executive Summary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavySurfaceVariant)
                    .padding(12.dp)
            ) {
                Text(
                    text = result.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, lineHeight = 18.sp)
                )
            }

            // Detected URLs
            if (result.detectedUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "EMBEDDED URLS FOUND (${result.detectedUrls.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                result.detectedUrls.forEach { url ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavyCardElevated)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextPrimary),
                            maxLines = 1
                        )
                    }
                }
            }

            // Findings Breakdown
            if (result.findings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "DETECTED SOCIAL ENGINEERING INDICATORS (${result.findings.size})",
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
