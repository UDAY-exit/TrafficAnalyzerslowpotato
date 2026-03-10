package com.trafficanalyzer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trafficanalyzer.data.model.Packet
import com.trafficanalyzer.ui.components.FilterPanel
import com.trafficanalyzer.ui.theme.CardDark
import com.trafficanalyzer.ui.theme.CyberGreen
import com.trafficanalyzer.ui.theme.DarkBackground
import com.trafficanalyzer.ui.theme.ErrorRed
import com.trafficanalyzer.ui.theme.SurfaceDark
import com.trafficanalyzer.ui.theme.TextSecondary
import com.trafficanalyzer.viewmodel.PacketViewModel

/**
 * PacketListScreen — Shows live captured packets, filter panel, and stats.
 *
 * Now displays [FilteredPacket] wrappers so each row can show its filter
 * decision badge, drop reason, SYN indicator, and port category label.
 *
 * The FilterPanel at the top lets the user switch modes and toggle
 * individual filter stages in real time.
 */
@Composable
fun PacketListScreen(
    viewModel: PacketViewModel,
    onAnalyzePacket: (Packet) -> Unit,
    modifier: Modifier = Modifier
) {
    val context         = LocalContext.current
    val filteredPackets by viewModel.filteredPackets.collectAsStateWithLifecycle()
    val filterConfig    by viewModel.filterConfig.collectAsStateWithLifecycle()
    val filterStats     by viewModel.filterStats.collectAsStateWithLifecycle()
    val isCapturing     by viewModel.isCapturing.collectAsStateWithLifecycle()
    val listState       = rememberLazyListState()

    // Auto-scroll to top when new filtered packet arrives
    LaunchedEffect(filteredPackets.size) {
        if (filteredPackets.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text  = "CAPTURED PACKETS",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberGreen,
                    fontSize = 14.sp
                )
                Text(
                    text  = "${filteredPackets.size} shown · ${filterStats.totalCaptured} captured",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Row {
                IconButton(onClick = { viewModel.clearPackets() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = TextSecondary)
                }
                Spacer(Modifier.width(4.dp))
                AnimatedVisibility(visible = isCapturing) {
                    Button(
                        onClick = { viewModel.stopCapture(context) },
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed,
                            contentColor   = DarkBackground
                        ),
                        shape    = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("STOP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Filter Panel ────────────────────────────────────────────────────
        FilterPanel(
            config         = filterConfig,
            stats          = filterStats,
            onModeSelected = { mode -> viewModel.setFilterMode(mode) },
            onConfigChange = { cfg  -> viewModel.updateFilterConfig(cfg) }
        )

        // ── Live status bar ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isCapturing) CyberGreen else TextSecondary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = if (isCapturing)
                            "LIVE — intercepting all device traffic"
                        else
                            "STOPPED — tap Analyze to inspect a packet",
                style = MaterialTheme.typography.labelSmall,
                color = if (isCapturing) CyberGreen else TextSecondary,
                fontSize = 11.sp
            )
        }

        // ── Packet list / empty state ───────────────────────────────────────
        if (filteredPackets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text     = if (isCapturing) "⏳" else "📭",
                        fontSize = 48.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text  = if (isCapturing)
                                    "Waiting for packets…\nTry opening a browser or any app."
                                else
                                    "No packets captured.\nStart a new capture session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state               = listState,
                contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier            = Modifier.fillMaxSize()
            ) {
                items(items = filteredPackets, key = { it.packet.id }) { fp ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn() + slideInVertically { -it / 2 }
                    ) {
                        FilteredPacketRowItem(
                            filteredPacket = fp,
                            onAnalyzeClick = { selectedPacket ->
                                // Stop VPN first so internet is restored for the API call
                                viewModel.stopCapture(context)
                                onAnalyzePacket(selectedPacket)
                            }
                        )
                    }
                }
            }
        }
    }
}
