package com.trafficanalyzer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trafficanalyzer.analysis.FilterConfig
import com.trafficanalyzer.analysis.FilterMode
import com.trafficanalyzer.analysis.FilterStats
import com.trafficanalyzer.ui.theme.CardDark
import com.trafficanalyzer.ui.theme.CyberGreen
import com.trafficanalyzer.ui.theme.SurfaceDark
import com.trafficanalyzer.ui.theme.TextPrimary
import com.trafficanalyzer.ui.theme.TextSecondary
import com.trafficanalyzer.ui.theme.WarningYellow

/**
 * FilterPanel — Expandable card at the top of PacketListScreen.
 *
 * Contains:
 *  1. Mode selector chips (RAW / CONNECTIONS / WEB / SUSPICIOUS / CUSTOM)
 *  2. Individual filter toggle switches
 *  3. Abuse score threshold slider (visible when suspiciousOnly = true)
 *  4. Show-dropped toggle
 *  5. Live stats bar
 *
 * Collapsed by default to save screen space; tapping the header expands it.
 */
@Composable
fun FilterPanel(
    config:          FilterConfig,
    stats:           FilterStats,
    onModeSelected:  (FilterMode) -> Unit,
    onConfigChange:  (FilterConfig) -> Unit,
    modifier:        Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .background(SurfaceDark)
            .border(
                width = 1.dp,
                color = CyberGreen.copy(alpha = 0.2f),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            )
    ) {
        // ── Collapsed header (always visible) ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Default.FilterAlt,
                    contentDescription = "Filter",
                    tint               = CyberGreen,
                    modifier           = Modifier.size(16.dp)
                )
                Text(
                    text      = "  TRAFFIC FILTERS",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = CyberGreen,
                    fontSize  = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text     = "  · ${config.mode.label}",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint               = TextSecondary,
                modifier           = Modifier.size(18.dp)
            )
        }

        // ── Always visible: stats bar ───────────────────────────────────────
        FilterStatsBar(stats = stats)

        // ── Expanded body ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Mode chips
                Text(
                    text     = "MODE",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = TextSecondary,
                    fontSize = 9.sp
                )
                ModeSelectorChips(
                    currentMode    = config.mode,
                    onModeSelected = onModeSelected
                )

                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = CyberGreen.copy(alpha = 0.1f))
                Spacer(Modifier.height(4.dp))

                // Individual toggles
                Text(
                    text     = "FILTER STAGES",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = TextSecondary,
                    fontSize = 9.sp
                )

                FilterToggleRow(
                    label    = "TCP Only",
                    subLabel = "Stage 1 — drop UDP, ICMP, other protocols",
                    checked  = config.tcpOnly,
                    onChecked = { onConfigChange(config.copy(tcpOnly = it, mode = FilterMode.CUSTOM)) }
                )
                FilterToggleRow(
                    label    = "Outbound Only",
                    subLabel = "Stage 2 — drop inbound / reply packets",
                    checked  = config.outboundOnly,
                    onChecked = { onConfigChange(config.copy(outboundOnly = it, mode = FilterMode.CUSTOM)) }
                )
                FilterToggleRow(
                    label    = "SYN Only",
                    subLabel = "Stage 3 — new connection attempts only",
                    checked  = config.synOnly,
                    onChecked = { onConfigChange(config.copy(synOnly = it, mode = FilterMode.CUSTOM)) }
                )
                FilterToggleRow(
                    label    = "Web Ports Only (80/443)",
                    subLabel = "Stage 4 — HTTP and HTTPS traffic only",
                    checked  = config.webPortsOnly,
                    onChecked = { onConfigChange(config.copy(webPortsOnly = it, mode = FilterMode.CUSTOM)) }
                )
                FilterToggleRow(
                    label    = "Drop Local Destinations",
                    subLabel = "Stage 5 — remove RFC-1918 / loopback targets",
                    checked  = config.dropLocalDestination,
                    onChecked = { onConfigChange(config.copy(dropLocalDestination = it, mode = FilterMode.CUSTOM)) }
                )
                FilterToggleRow(
                    label    = "Suspicious Only (AbuseIPDB)",
                    subLabel = "Post-stage — only IPs scoring above threshold",
                    checked  = config.suspiciousOnly,
                    onChecked = { onConfigChange(config.copy(suspiciousOnly = it, mode = FilterMode.CUSTOM)) }
                )

                // Abuse threshold slider (only relevant when suspiciousOnly is on)
                AnimatedVisibility(visible = config.suspiciousOnly) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text     = "Abuse Score Threshold",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = TextPrimary,
                                fontSize = 11.sp
                            )
                            Text(
                                text     = "≥ ${config.abuseScoreThreshold}",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = WarningYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value         = config.abuseScoreThreshold.toFloat(),
                            onValueChange = { v ->
                                onConfigChange(config.copy(abuseScoreThreshold = v.toInt()))
                            },
                            valueRange    = 0f..100f,
                            steps         = 9,
                            colors        = SliderDefaults.colors(
                                thumbColor       = WarningYellow,
                                activeTrackColor = WarningYellow
                            )
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = CyberGreen.copy(alpha = 0.1f))
                Spacer(Modifier.height(4.dp))

                // View options
                Text(
                    text     = "VIEW OPTIONS",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = TextSecondary,
                    fontSize = 9.sp
                )
                FilterToggleRow(
                    label    = "Show Dropped Packets",
                    subLabel = "Greyed-out rows with drop reason badge",
                    checked  = config.showDropped,
                    onChecked = { onConfigChange(config.copy(showDropped = it)) }
                )
            }
        }
    }
}
