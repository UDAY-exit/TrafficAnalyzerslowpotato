package com.trafficanalyzer.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trafficanalyzer.analysis.FilterMode
import com.trafficanalyzer.ui.theme.AccentOrange
import com.trafficanalyzer.ui.theme.CardDark
import com.trafficanalyzer.ui.theme.CyberGreen
import com.trafficanalyzer.ui.theme.TextSecondary

/**
 * ModeSelectorChips — Horizontal scrollable row of FilterChips for mode selection.
 *
 * The selected chip is highlighted in CyberGreen; others are dimmed.
 * Tapping a chip calls [onModeSelected] which the ViewModel routes to
 * [FilterConfig.forMode] to apply the correct toggle preset.
 */
@Composable
fun ModeSelectorChips(
    currentMode: FilterMode,
    onModeSelected: (FilterMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterMode.entries.forEach { mode ->
            val selected = mode == currentMode
            FilterChip(
                selected = selected,
                onClick  = { onModeSelected(mode) },
                label    = {
                    Text(
                        text     = mode.label,
                        fontSize = 11.sp,
                        style    = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor    = CyberGreen.copy(alpha = 0.2f),
                    selectedLabelColor        = CyberGreen,
                    containerColor            = CardDark,
                    labelColor                = TextSecondary,
                    selectedLeadingIconColor  = AccentOrange
                )
            )
        }
    }
}
