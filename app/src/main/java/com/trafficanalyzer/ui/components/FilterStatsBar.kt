package com.trafficanalyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trafficanalyzer.analysis.FilterStats
import com.trafficanalyzer.ui.theme.CardDark
import com.trafficanalyzer.ui.theme.CyberGreen
import com.trafficanalyzer.ui.theme.ErrorRed
import com.trafficanalyzer.ui.theme.SurfaceDark
import com.trafficanalyzer.ui.theme.TextPrimary
import com.trafficanalyzer.ui.theme.TextSecondary
import com.trafficanalyzer.ui.theme.WarningYellow

/**
 * FilterStatsBar — Compact live counter strip.
 *
 * Shows: Captured | Kept | Dropped | Drop% + a colour-coded progress bar.
 * The bar fills proportionally from green (kept) to red (dropped).
 */
@Composable
fun FilterStatsBar(
    stats: FilterStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardDark)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── Counter row ────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            StatPill(label = "CAPTURED", value = stats.totalCaptured.toString(), color = TextPrimary)
            StatPill(label = "KEPT",     value = stats.totalKept.toString(),     color = CyberGreen)
            StatPill(label = "DROPPED",  value = stats.totalDropped.toString(),  color = ErrorRed)
            StatPill(
                label = "DROP%",
                value = "${stats.dropPercent}%",
                color = when {
                    stats.dropPercent >= 75 -> ErrorRed
                    stats.dropPercent >= 40 -> WarningYellow
                    else                    -> CyberGreen
                }
            )
        }

        // ── Progress bar ───────────────────────────────────────────────────
        if (stats.totalCaptured > 0) {
            val keptFraction = stats.totalKept.toFloat() / stats.totalCaptured
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(keptFraction)
                        .background(CyberGreen)
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color  // kept as FQN to avoid ambiguity with Color import
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall,
            color     = TextSecondary,
            fontSize  = 8.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text      = value,
            style     = MaterialTheme.typography.bodySmall,
            color     = color,
            fontSize  = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
