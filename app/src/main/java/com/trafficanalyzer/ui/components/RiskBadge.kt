package com.trafficanalyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trafficanalyzer.ui.theme.ErrorRed
import com.trafficanalyzer.ui.theme.SuccessGreen
import com.trafficanalyzer.ui.theme.TextSecondary
import com.trafficanalyzer.ui.theme.WarningYellow

/**
 * RiskBadge — Coloured chip showing an AbuseIPDB reputation score.
 *
 * Colour legend:
 *  GREEN   0–24   — clean / low risk
 *  YELLOW  25–74  — moderate risk
 *  RED     75–100 — high risk / malicious
 *  GREY    null   — score not yet fetched
 */
@Composable
fun RiskBadge(
    score: Int?,
    modifier: Modifier = Modifier
) {
    val (label, color) = when {
        score == null          -> "?" to TextSecondary
        score >= 75            -> "HIGH $score" to ErrorRed
        score >= 25            -> "MED $score"  to WarningYellow
        else                   -> "CLEAN $score" to SuccessGreen
    }

    BadgeChip(label = label, color = color, modifier = modifier)
}

/**
 * FilterDecisionBadge — Shows PASS or the drop reason in a colour chip.
 */
@Composable
fun FilterDecisionBadge(
    passed: Boolean,
    reason: String,
    modifier: Modifier = Modifier
) {
    val (label, color) = if (passed) {
        "PASS" to SuccessGreen
    } else {
        "DROP" to ErrorRed
    }
    BadgeChip(label = label, color = color, modifier = modifier)
}

/**
 * PortCategoryBadge — HTTP / HTTPS / DNS / OTHER label chip.
 */
@Composable
fun PortCategoryBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    val color = when (label) {
        "HTTP"     -> Color(0xFF60A5FA)
        "HTTPS"    -> Color(0xFF34D399)
        "DNS"      -> Color(0xFFA78BFA)
        "SSH"      -> Color(0xFFF97316)
        "RDP"      -> Color(0xFFEF4444)
        else       -> TextSecondary
    }
    BadgeChip(label = label, color = color, modifier = modifier)
}

/**
 * SynBadge — Small red SYN indicator for TCP SYN packets.
 */
@Composable
fun SynBadge(modifier: Modifier = Modifier) {
    BadgeChip(label = "SYN", color = Color(0xFFF97316), modifier = modifier)
}

// ── Private helper ────────────────────────────────────────────────────────────

@Composable
private fun BadgeChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text  = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
