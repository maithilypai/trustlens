package com.trustlens.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustlens.app.ui.theme.StatusWarningAmber
import com.trustlens.app.ui.theme.BlueAccent
import com.trustlens.app.ui.theme.CyanAccent
import com.trustlens.app.ui.theme.NavyBorder
import com.trustlens.app.ui.theme.NavySurface
import com.trustlens.app.ui.theme.StatusCleanEmerald
import com.trustlens.app.ui.theme.StatusThreatRed
import com.trustlens.app.ui.theme.TextMuted
import com.trustlens.app.ui.theme.TextPrimary
import com.trustlens.app.ui.theme.TextSecondary

@Composable
fun TrustlensHeader(
    onSettingsClick: () -> Unit,
    backendStatus: String = "checking",
    isThreatIntelActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Shield Emblem with glowing border
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NavySurface, Color(0xFF1E2A47))
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(CyanAccent, BlueAccent)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "TRUSTLENS Shield",
                        tint = CyanAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TRUST",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "LENS",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = CyanAccent
                            )
                        )
                    }
                    Text(
                        text = "LINK PROTECTION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueAccent
                        )
                    )
                }
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(NavySurface)
                    .border(1.dp, NavyBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Server Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Official Tagline
        Text(
            text = "One click shouldn't cost you your security.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Real-Time Scam Link Detection & Threat Protection",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextMuted
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Backend Connection Status Indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(NavySurface)
                .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            val statusColor = when (backendStatus.lowercase()) {
                "online" -> StatusCleanEmerald
                "offline" -> StatusThreatRed
                else -> StatusWarningAmber
            }
            val statusText = when (backendStatus.lowercase()) {
                "online" -> if (isThreatIntelActive) "Backend Online • Threat Intel Active" else "Backend Online • Heuristics Active"
                "offline" -> "Backend Offline • Tap ⚙ to verify IP"
                else -> "Checking Backend Connectivity..."
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
