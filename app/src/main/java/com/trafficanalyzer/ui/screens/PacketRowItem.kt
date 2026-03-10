package com.trafficanalyzer.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trafficanalyzer.analysis.FilteredPacket
import com.trafficanalyzer.analysis.PacketFilterEngine
import com.trafficanalyzer.data.model.Packet
import com.trafficanalyzer.ui.components.FilterDecisionBadge
import com.trafficanalyzer.ui.components.PortCategoryBadge
import com.trafficanalyzer.ui.components.SynBadge
import com.trafficanalyzer.ui.theme.AccentOrange
import com.trafficanalyzer.ui.theme.CardDark
import com.trafficanalyzer.ui.theme.CyberGreen
import com.trafficanalyzer.ui.theme.DarkBackground
import com.trafficanalyzer.ui.theme.ErrorRed
import com.trafficanalyzer.ui.theme.ProtocolIcmp
import com.trafficanalyzer.ui.theme.ProtocolOther
import com.trafficanalyzer.ui.theme.ProtocolTcp
import com.trafficanalyzer.ui.theme.ProtocolUdp
import com.trafficanalyzer.ui.theme.TextPrimary
import com.trafficanalyzer.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FilteredPacketRowItem — Packet row with filter engine decorations.
 *
 * Shows:
 *  - Filter decision badge (PASS green / DROP red)
 *  - Protocol badge
 *  - SYN indicator (orange) when TCP SYN flag is set
 *  - Port category label (HTTP / HTTPS / DNS / SSH ...)
 *  - Drop reason text when decision is DROP
 *  - Greyed-out alpha when dropped
 *  - Analyze button disabled on dropped packets
 */
@Composable
fun FilteredPacketRowItem(
    filteredPacket: FilteredPacket,
    onAnalyzeClick: (Packet) -> Unit,
    modifier: Modifier = Modifier
) {
    val packet    = filteredPacket.packet
    val decision  = filteredPacket.decision
    val isDropped = !decision.pass

    val rowAlpha    = if (isDropped) 0.45f else 1f
    val borderColor = if (isDropped) ErrorRed.copy(alpha = 0.25f)
                      else CyberGreen.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .clip(RoundedCornerShape(10.dp))
            .background(CardDark)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            // Top row: id + timestamp + badge cluster
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "#${packet.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text  = formatTime(packet.timestampMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterDecisionBadge(passed = decision.pass, reason = decision.reason)
                    ProtocolBadge(protocol = packet.protocol)
                    if (packet.isSyn) SynBadge()
                    val portLabel = PacketFilterEngine.portLabel(packet.destinationPort)
                    if (portLabel != "—") PortCategoryBadge(label = portLabel)
                }
            }

            // Drop reason
            if (isDropped) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = "\u21b3 ${decision.reason}",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = ErrorRed.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Middle row: src -> dst
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SRC", style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary, fontSize = 9.sp)
                    Text(
                        text  = buildPortString(packet.sourceIp, packet.sourcePort),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary, fontSize = 12.sp
                    )
                }
                Text(
                    text       = "\u2192",
                    color      = if (isDropped) TextSecondary else CyberGreen,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 8.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("DST", style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary, fontSize = 9.sp)
                    Text(
                        text       = buildPortString(packet.destinationIp, packet.destinationPort),
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = if (isDropped) TextSecondary else CyberGreen,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Bottom row: size + Analyze button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "${packet.length} bytes",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Button(
                    onClick  = { if (!isDropped) onAnalyzeClick(packet) },
                    enabled  = !isDropped,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = AccentOrange,
                        contentColor           = DarkBackground,
                        disabledContainerColor = TextSecondary.copy(alpha = 0.2f),
                        disabledContentColor   = TextSecondary.copy(alpha = 0.5f)
                    ),
                    shape    = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Analyze",
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ANALYZE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// Legacy wrapper so any remaining call sites still compile
@Composable
fun PacketRowItem(
    packet: Packet,
    onAnalyzeClick: (Packet) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardDark)
            .border(1.dp, CyberGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("#${packet.id}", style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary, fontSize = 11.sp)
                Text(formatTime(packet.timestampMs), style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary, fontSize = 11.sp)
                ProtocolBadge(protocol = packet.protocol)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SRC", style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary, fontSize = 9.sp)
                    Text(buildPortString(packet.sourceIp, packet.sourcePort),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary, fontSize = 12.sp)
                }
                Text("\u2192", color = CyberGreen, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("DST", style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary, fontSize = 9.sp)
                    Text(buildPortString(packet.destinationIp, packet.destinationPort),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("${packet.length} bytes", style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary)
                Button(onClick = { onAnalyzeClick(packet) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentOrange, contentColor = DarkBackground),
                    shape = RoundedCornerShape(6.dp), modifier = Modifier.height(32.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "Analyze",
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ANALYZE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// Shared private helpers
@Composable
private fun ProtocolBadge(protocol: String) {
    val color = protocolColor(protocol)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(protocol.take(4), color = color, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
    }
}

private fun protocolColor(protocol: String): Color = when {
    protocol.startsWith("TCP")  -> ProtocolTcp
    protocol.startsWith("UDP")  -> ProtocolUdp
    protocol.startsWith("ICMP") -> ProtocolIcmp
    else                        -> ProtocolOther
}

private fun buildPortString(ip: String, port: Int): String =
    if (port > 0) "$ip:$port" else ip

private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
private fun formatTime(epochMs: Long): String = timeFormatter.format(Date(epochMs))
