package com.trustlens.app.ui.screens

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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustlens.app.data.model.ScreenDestination
import com.trustlens.app.ui.theme.CyanAccent
import com.trustlens.app.ui.theme.NavyBackground
import com.trustlens.app.ui.theme.NavyBorder
import com.trustlens.app.ui.theme.NavyCardElevated
import com.trustlens.app.ui.theme.NavySurface
import com.trustlens.app.ui.theme.NavySurfaceVariant
import com.trustlens.app.ui.theme.StatusCleanEmerald
import com.trustlens.app.ui.theme.StatusWarningAmber
import com.trustlens.app.ui.theme.TextMuted
import com.trustlens.app.ui.theme.TextPrimary
import com.trustlens.app.ui.theme.TextSecondary
import com.trustlens.app.ui.viewmodel.TrustlensViewModel

@Composable
fun PrivacyEducationScreen(
    viewModel: TrustlensViewModel,
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
                        text = "PRIVACY & SECURITY ARCHITECTURE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Transparent data practices, limitations, and user defense",
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
            // Privacy Commitment Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.15f))
                            .border(1.dp, CyanAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "ZERO PERSISTENCE ARCHITECTURE",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CyanAccent)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "TRUSTLENS is engineered on privacy-by-design principles. User-submitted links, SMS text, and QR payloads are inspected strictly in volatile memory and are NEVER logged, sold, or stored in databases.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 18.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CORE DATA & PRIVACY PILLARS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 1.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            PrivacyPillarCard(
                title = "No Browsing History Collection",
                description = "Unlike traditional tracking browsers or commercial DNS proxies, the TRUSTLENS Safe Browser does not record browsing sessions, user cookies, or site history.",
                icon = Icons.Default.VisibilityOff
            )

            Spacer(modifier = Modifier.height(10.dp))

            PrivacyPillarCard(
                title = "No Credential or OTP Storage",
                description = "TRUSTLENS never prompts for passwords, one-time verification codes, bank PINs, or debit/credit card credentials. Our heuristic engine only inspects social engineering lure patterns.",
                icon = Icons.Default.Security
            )

            Spacer(modifier = Modifier.height(10.dp))

            PrivacyPillarCard(
                title = "Threat Intelligence Privacy",
                description = "External queries to Google Safe Browsing only submit the normalized URL hash prefix or URL string strictly when configured. Upstream queries are performed securely over HTTPS.",
                icon = Icons.Default.CloudOff
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "DETECTOR LIMITATIONS & REALITIES",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = StatusWarningAmber, letterSpacing = 1.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Limitations Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCardElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarningAmber.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = StatusWarningAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Important Security Notice: Automated Limits",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = StatusWarningAmber)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. False Negatives (Novel Zero-Days): Newly registered scam domains created minutes ago may not yet be indexed by threat intelligence providers.\n\n" +
                                        "2. False Positives: Authentic brand affiliates, corporate CDNs, or complex corporate subdomains can occasionally trigger brand-in-subdomain heuristic flags.\n\n" +
                                        "3. HTTPS Does Not Equal Legitimate: Today over 85% of phishing sites use free Let's Encrypt or Cloudflare TLS certificates. A green padlock only proves encryption, NOT safety.\n\n" +
                                        "4. Verification Rule: Always verify sensitive payment and identity requests by navigating directly to official apps or verified phone numbers.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 18.sp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PrivacyPillarCard(
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NavySurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = StatusCleanEmerald, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = description, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp))
            }
        }
    }
}
