package com.trafficanalyzer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trafficanalyzer.data.model.IpInfoResponse
import com.trafficanalyzer.data.model.Packet
import com.trafficanalyzer.data.model.asn
import com.trafficanalyzer.data.model.orgName
import com.trafficanalyzer.ui.theme.AccentBlue
import com.trafficanalyzer.ui.theme.CardDark
import com.trafficanalyzer.ui.theme.CyberGreen
import com.trafficanalyzer.ui.theme.DarkBackground
import com.trafficanalyzer.ui.theme.ErrorRed
import com.trafficanalyzer.ui.theme.SurfaceDark
import com.trafficanalyzer.ui.theme.TextPrimary
import com.trafficanalyzer.ui.theme.TextSecondary
import com.trafficanalyzer.viewmodel.AnalysisUiState
import com.trafficanalyzer.viewmodel.AnalysisViewModel
import com.trafficanalyzer.viewmodel.AiSummaryState

/**
 * AnalysisScreen — Shows IPinfo geolocation data for a selected packet,
 * and provides a "Generate AI Summary" button that calls AI for a
 * plain-English explanation suitable for cybersecurity students.
 */
@Composable
fun AnalysisScreen(
    packet: Packet,
    viewModel: AnalysisViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val summaryState by viewModel.summaryState.collectAsStateWithLifecycle()

    LaunchedEffect(packet.id) {
        viewModel.analysePacket(packet)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ── Top bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                    tint = CyberGreen)
            }
            Column {
                Text("IP ANALYSIS", style = MaterialTheme.typography.titleMedium,
                    color = CyberGreen, fontSize = 14.sp)
                Text(packet.destinationIp, style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary)
            }
        }

        // ── Content ────────────────────────────────────────────────────────
        when (val state = uiState) {
            is AnalysisUiState.Idle,
            is AnalysisUiState.Loading -> LoadingContent()

            is AnalysisUiState.Success -> SuccessContent(
                packet            = state.packet,
                ipInfo            = state.ipInfo,
                summaryState      = summaryState,
                onGenerateSummary = { viewModel.generateSummary() },
                onRetrySummary    = { viewModel.retrySummary() }
            )

            is AnalysisUiState.Error -> ErrorContent(
                message = state.message,
                packet  = state.packet
            )
        }
    }
}

// ─── Loading ──────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = CyberGreen, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Querying IPinfo API…",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text("(VPN stopped — internet restored)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.6f))
        }
    }
}

// ─── Success ──────────────────────────────────────────────────────────────────

@Composable
private fun SuccessContent(
    packet: Packet,
    ipInfo: IpInfoResponse,
    summaryState: AiSummaryState,
    onGenerateSummary: () -> Unit,
    onRetrySummary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── IP Information ─────────────────────────────────────────────────
        SectionHeader("IP INFORMATION")
        InfoCard {
            InfoRow(Icons.Default.NetworkCheck, "IP Address", ipInfo.ip ?: packet.destinationIp)
            InfoRow(Icons.Default.Public, "Country", ipInfo.country ?: "N/A")
            InfoRow(Icons.Default.LocationCity, "City / Region", buildString {
                if (!ipInfo.city.isNullOrBlank()) append(ipInfo.city)
                if (!ipInfo.region.isNullOrBlank()) {
                    if (isNotEmpty()) append(", "); append(ipInfo.region)
                }
                if (isEmpty()) append("N/A")
            })
            InfoRow(Icons.Default.Language, "Timezone", ipInfo.timezone ?: "N/A")
        }

        // ── Network ────────────────────────────────────────────────────────
        SectionHeader("NETWORK")
        InfoCard {
            InfoRow(Icons.Default.Numbers, "ASN", ipInfo.asn)
            InfoRow(Icons.Default.NetworkCheck, "Organisation", ipInfo.orgName)
        }

        // ── Packet Details ─────────────────────────────────────────────────
        SectionHeader("PACKET DETAILS")
        InfoCard {
            InfoRow(Icons.Default.NetworkCheck, "Protocol", packet.protocol)
            InfoRow(Icons.Default.Numbers, "Source",
                if (packet.sourcePort > 0) "${packet.sourceIp} : ${packet.sourcePort}"
                else packet.sourceIp)
            InfoRow(Icons.Default.Numbers, "Destination",
                if (packet.destinationPort > 0) "${packet.destinationIp} : ${packet.destinationPort}"
                else packet.destinationIp)
            InfoRow(Icons.Default.NetworkCheck, "Length", "${packet.length} bytes")
        }

        // ── Bogon warning ──────────────────────────────────────────────────
        if (ipInfo.bogon == true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ErrorRed.copy(alpha = 0.15f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "⚠ Bogon IP — This is a private/reserved address. " +
                           "It won't appear in public routing tables.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── AI Summary section ─────────────────────────────────────────────
        AiSummarySection(
            summaryState      = summaryState,
            onGenerateSummary = onGenerateSummary,
            onRetrySummary    = onRetrySummary
        )

        Spacer(Modifier.height(32.dp))
    }
}

// ─── AI Summary section ───────────────────────────────────────────────────────

@Composable
private fun AiSummarySection(
    summaryState: AiSummaryState,
    onGenerateSummary: () -> Unit,
    onRetrySummary: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        SectionHeader("AI SUMMARY")

        when (summaryState) {

            // ── Idle: show the "Generate" button ──────────────────────────
            is AiSummaryState.Idle -> {
                Button(
                    onClick = onGenerateSummary,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberGreen.copy(alpha = 0.15f),
                        contentColor   = CyberGreen
                    )
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Generate AI Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp
                    )
                }
            }

            // ── Loading: spinner ──────────────────────────────────────────
            is AiSummaryState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardDark)
                        .border(1.dp, CyberGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color       = CyberGreen,
                            modifier    = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "thinking…",
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            "Generating plain-English explanation",
                            style  = MaterialTheme.typography.labelSmall,
                            color  = TextSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ── Success: summary card ─────────────────────────────────────
            is AiSummaryState.Success -> {
                AnimatedVisibility(
                    visible = true,
                    enter   = fadeIn() + expandVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardDark)
                            .border(1.dp, CyberGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint     = CyberGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "AI Summary",
                                style      = MaterialTheme.typography.labelMedium,
                                color      = CyberGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 12.sp
                            )
                        }

                        // The summary text from AI
                        Text(
                            text       = summaryState.summary,
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = TextPrimary,
                            lineHeight = 22.sp,
                            fontSize   = 13.sp
                        )

                        // Regenerate button
                        OutlinedButton(
                            onClick  = onGenerateSummary,
                            modifier = Modifier.align(Alignment.End),
                            shape    = RoundedCornerShape(8.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = CyberGreen
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null,
                                modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Regenerate", fontSize = 11.sp)
                        }

                        // Disclaimer
                        Text(
                            text      = "⚠ AI-generated — verify critical information independently.",
                            style     = MaterialTheme.typography.labelSmall,
                            color     = TextSecondary.copy(alpha = 0.5f),
                            fontStyle = FontStyle.Italic,
                            fontSize  = 10.sp
                        )
                    }
                }
            }

            // ── Error: message + retry ────────────────────────────────────
            is AiSummaryState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ErrorRed.copy(alpha = 0.08f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null,
                            tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Summary Failed",
                            style      = MaterialTheme.typography.labelMedium,
                            color      = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text  = summaryState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedButton(
                        onClick = onRetrySummary,
                        shape   = RoundedCornerShape(8.dp),
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retry", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ─── Error state ──────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, packet: Packet) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Error, contentDescription = "Error",
                tint = ErrorRed, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Analysis Failed", style = MaterialTheme.typography.titleMedium, color = ErrorRed)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Destination IP: ${packet.destinationIp}",
                style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(16.dp))
            Text(
                "Hint: Ensure internet is available and the IP is not a private/loopback address.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style      = MaterialTheme.typography.labelSmall,
        color      = CyberGreen,
        fontSize   = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier   = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardDark)
            .border(1.dp, CyberGreen.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) { content() }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = TextSecondary, fontSize = 10.sp)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}
