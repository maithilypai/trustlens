package com.trustlens.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustlens.app.data.model.ComparisonDemoResponse
import com.trustlens.app.data.model.DemoScenario
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
import com.trustlens.app.ui.theme.StatusCleanEmeraldBg
import com.trustlens.app.ui.theme.StatusThreatRed
import com.trustlens.app.ui.theme.StatusThreatRedBg
import com.trustlens.app.ui.theme.StatusWarningAmber
import com.trustlens.app.ui.theme.TextMuted
import com.trustlens.app.ui.theme.TextPrimary
import com.trustlens.app.ui.theme.TextSecondary
import com.trustlens.app.ui.viewmodel.TrustlensUiState
import com.trustlens.app.ui.viewmodel.TrustlensViewModel

@Composable
fun DemoScenariosScreen(
    viewModel: TrustlensViewModel,
    uiState: TrustlensUiState,
    modifier: Modifier = Modifier
) {
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
                        text = "EDUCATIONAL SCAM DEMONSTRATIONS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Controlled simulations of 6 real-world threat modalities",
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
            // Simulation Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF281C3D))
                    .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Science, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SIMULATED HACKATHON EVALUATION SUITE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFC4B5FD))
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "These scenarios run deterministic synthetic fixtures through the TRUSTLENS detection engine. No live victim sites or external payment servers are accessed.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                        )
                    }
                }
            }

            // Side-by-Side Comparison Demo
            if (uiState.comparisonResult != null) {
                Spacer(modifier = Modifier.height(20.dp))
                ComparisonCard(
                    comparison = uiState.comparisonResult,
                    onDismiss = { /* keep open or can dismiss */ }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Comparison Button Action
            OutlinedButton(
                onClick = { viewModel.runComparisonDemo() },
                enabled = !uiState.isComparisonLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = NavySurface)
            ) {
                if (uiState.isComparisonLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyanAccent, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Side-by-Side Comparison Demo", style = MaterialTheme.typography.labelMedium.copy(color = CyanAccent, fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SIX HACKATHON SCAM MODALITIES",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 1.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Phishing Link
            DemoScenarioCard(
                number = "1",
                title = "Suspicious Phishing Link",
                subtitle = "Deceptive lookalike brand portal targeting financial login credentials.",
                icon = Icons.Default.GppBad,
                accentColor = StatusThreatRed,
                onClick = { viewModel.runDemoScenario(DemoScenario.SCAM_LINK) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Parcel Delivery Fee Smishing
            DemoScenarioCard(
                number = "2",
                title = "Parcel Delivery Fee Smishing",
                subtitle = "Postal customs fee notice ($2.15) with urgent package hold threat.",
                icon = Icons.Default.LocalShipping,
                accentColor = StatusThreatRed,
                onClick = { viewModel.runDemoScenario(DemoScenario.PARCEL_SMS) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. OTP Theft Attempt
            DemoScenarioCard(
                number = "3",
                title = "Bank Wire OTP Theft Attempt",
                subtitle = "Wire transfer authorization lure soliciting 6-digit one-time password.",
                icon = Icons.Default.Pin,
                accentColor = StatusThreatRed,
                onClick = { viewModel.runDemoScenario(DemoScenario.OTP_THEFT) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Fake Payment Request
            DemoScenarioCard(
                number = "4",
                title = "Fake Supplier Invoice / Payment",
                subtitle = "Urgent supplier banking redirection requesting corporate payment change.",
                icon = Icons.Default.CreditCard,
                accentColor = StatusThreatRed,
                onClick = { viewModel.runDemoScenario(DemoScenario.FAKE_PAYMENT) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 5. QR-Code Quishing Scam
            DemoScenarioCard(
                number = "5",
                title = "Quishing / Deceptive QR Sticker",
                subtitle = "Physical signage QR sticker overlay redirecting payment to fraudulent account.",
                icon = Icons.Default.QrCode,
                accentColor = StatusThreatRed,
                onClick = { viewModel.runDemoScenario(DemoScenario.QR_SCAM) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Fraudulent Caller Impersonation
            DemoScenarioCard(
                number = "6",
                title = "Fraud Department Vishing Alert",
                subtitle = "Follow-up SMS alert from purported bank fraud prevention team.",
                icon = Icons.Default.PhoneInTalk,
                accentColor = StatusWarningAmber,
                onClick = { viewModel.runDemoScenario(DemoScenario.FRAUD_CALL) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "BENIGN & UNVERIFIED REFERENCES",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Legitimate reference
            DemoScenarioCard(
                number = "7",
                title = "Legitimate Domain Reference",
                subtitle = "Harmless RFC reserved domain (example.com) passing all structural heuristics.",
                icon = Icons.Default.GppGood,
                accentColor = StatusCleanEmerald,
                onClick = { viewModel.runDemoScenario(DemoScenario.NO_KNOWN_THREAT) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Inconclusive reference
            DemoScenarioCard(
                number = "8",
                title = "Inconclusive Threat Intel Lookup",
                subtitle = "Heuristics clean but external threat intelligence service is unreachable.",
                icon = Icons.Default.HelpOutline,
                accentColor = CyanAccent,
                onClick = { viewModel.runDemoScenario(DemoScenario.INCONCLUSIVE) }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun DemoScenarioCard(
    number: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$number. $title",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                )
            }
        }
    }
}

@Composable
private fun ComparisonCard(
    comparison: ComparisonDemoResponse,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SIDE-BY-SIDE EDUCATIONAL CONTRAST",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CyanAccent)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = comparison.comparisonSummary,
                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, lineHeight = 17.sp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Contrast 1: Deceptive Threat
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StatusThreatRedBg)
                    .border(1.dp, StatusThreatRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(text = "SIMULATED DECEPTIVE LINK (CONFIRMED THREAT)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = StatusThreatRed))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = comparison.simulatedThreat.urlValidation.normalizedUrl, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextPrimary))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = comparison.simulatedThreat.summary, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Contrast 2: Harmless Benign URL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StatusCleanEmeraldBg)
                    .border(1.dp, StatusCleanEmerald.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(text = "HARMLESS BENIGN DOMAIN (PASSES CHECKS)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = StatusCleanEmerald))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = comparison.harmlessUrl.urlValidation.normalizedUrl, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextPrimary))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = comparison.harmlessUrl.summary, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
                }
            }

            // Key Differences List
            if (comparison.keyDifferences.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "KEY TECHNICAL DIFFERENTIATORS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.8.sp))
                Spacer(modifier = Modifier.height(6.dp))
                comparison.keyDifferences.forEach { diff ->
                    Text(text = "• $diff", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, lineHeight = 16.sp, fontSize = 12.sp))
                }
            }
        }
    }
}
