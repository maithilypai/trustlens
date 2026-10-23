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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustlens.app.data.model.AnalyzeResponse
import com.trustlens.app.data.model.RiskCategory
import com.trustlens.app.ui.theme.NavyBorder
import com.trustlens.app.ui.theme.NavySurface
import com.trustlens.app.ui.theme.NavySurfaceVariant
import com.trustlens.app.ui.theme.StatusCleanEmerald
import com.trustlens.app.ui.theme.StatusCleanEmeraldBg
import com.trustlens.app.ui.theme.StatusCleanEmeraldBorder
import com.trustlens.app.ui.theme.StatusInconclusiveCyan
import com.trustlens.app.ui.theme.StatusInconclusiveCyanBg
import com.trustlens.app.ui.theme.StatusInconclusiveCyanBorder
import com.trustlens.app.ui.theme.StatusThreatRed
import com.trustlens.app.ui.theme.StatusThreatRedBg
import com.trustlens.app.ui.theme.StatusThreatRedBorder
import com.trustlens.app.ui.theme.StatusWarningAmber
import com.trustlens.app.ui.theme.StatusWarningAmberBg
import com.trustlens.app.ui.theme.StatusWarningAmberBorder
import com.trustlens.app.ui.theme.TextPrimary
import com.trustlens.app.ui.theme.TextSecondary

private data class AssessmentDisplayConfig(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val backgroundColor: Color,
    val borderColor: Color
)

@Composable
fun RiskCard(
    response: AnalyzeResponse,
    modifier: Modifier = Modifier
) {
    val config = when (response.assessment) {
        RiskCategory.CONFIRMED_THREAT -> AssessmentDisplayConfig(
            title = "CONFIRMED THREAT MATCH",
            subtitle = "Active scam or malware danger verified",
            icon = Icons.Default.GppBad,
            primaryColor = StatusThreatRed,
            backgroundColor = StatusThreatRedBg,
            borderColor = StatusThreatRedBorder
        )
        RiskCategory.SUSPICIOUS_INDICATORS -> AssessmentDisplayConfig(
            title = "SUSPICIOUS INDICATORS DETECTED",
            subtitle = "Structural characteristics strongly resemble scam campaigns",
            icon = Icons.Default.WarningAmber,
            primaryColor = StatusWarningAmber,
            backgroundColor = StatusWarningAmberBg,
            borderColor = StatusWarningAmberBorder
        )
        RiskCategory.NO_KNOWN_THREAT -> AssessmentDisplayConfig(
            title = "NO KNOWN THREAT DETECTED",
            subtitle = "Local heuristics and reputation checks observed no indexed threats",
            icon = Icons.Default.GppGood,
            primaryColor = StatusCleanEmerald,
            backgroundColor = StatusCleanEmeraldBg,
            borderColor = StatusCleanEmeraldBorder
        )
        RiskCategory.INCONCLUSIVE -> AssessmentDisplayConfig(
            title = "INCONCLUSIVE EVALUATION",
            subtitle = "No structural warning signs, but external threat intel was unavailable",
            icon = Icons.Default.HelpOutline,
            primaryColor = StatusInconclusiveCyan,
            backgroundColor = StatusInconclusiveCyanBg,
            borderColor = StatusInconclusiveCyanBorder
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, config.borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Simulated demonstration banner
            if (response.isSimulated) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF281C3D))
                        .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Simulated Demo",
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SIMULATED DEMONSTRATION RESULT — NOT A LIVE THREAT DETECTION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC4B5FD),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Main Assessment Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(config.backgroundColor)
                        .border(1.5.dp, config.primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = config.title,
                        tint = config.primaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = config.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = config.primaryColor,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = config.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Executive Summary Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavySurfaceVariant)
                    .border(1.dp, NavyBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = response.summary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                )
            }

            // Recommended Actions
            if (response.recommendedActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "RECOMMENDED ACTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    response.recommendedActions.forEach { action ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = config.primaryColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = action,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
