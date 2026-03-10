package com.trafficanalyzer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trafficanalyzer.ui.theme.CyberGreen
import com.trafficanalyzer.ui.theme.SurfaceDark
import com.trafficanalyzer.ui.theme.TextSecondary

/**
 * FilterToggleRow — A labelled Switch row for one filter toggle.
 *
 * The label text describes the filter rule; the sub-label (optional)
 * gives a brief technical explanation for the SOC demo audience.
 */
@Composable
fun FilterToggleRow(
    label:       String,
    subLabel:    String    = "",
    checked:     Boolean,
    onChecked:   (Boolean) -> Unit,
    modifier:    Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (checked) CyberGreen else TextSecondary,
                fontSize = 12.sp
            )
            if (subLabel.isNotBlank()) {
                Text(
                    text  = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor       = CyberGreen,
                checkedTrackColor       = CyberGreen.copy(alpha = 0.3f),
                uncheckedThumbColor     = TextSecondary,
                uncheckedTrackColor     = SurfaceDark
            )
        )
    }
}
