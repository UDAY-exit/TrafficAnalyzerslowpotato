package com.trafficanalyzer.analysis

import com.trafficanalyzer.data.model.Packet

/**
 * PacketFilterEngine — 5-stage filter chain for the Traffic Analyzer demo.
 *
 * Each stage is a pure function that returns a [FilterDecision] (PASS or DROP).
 * Stages are evaluated in order; the first DROP short-circuits the chain.
 *
 * ┌────────────┬─────────────────────────────────────────────────────────────┐
 * │ Stage      │ Rule                                                        │
 * ├────────────┼─────────────────────────────────────────────────────────────┤
 * │ 1          │ Protocol   — keep TCP only (if tcpOnly enabled)             │
 * │ 2          │ Direction  — keep outbound only (if outboundOnly enabled)   │
 * │ 3          │ TCP Flag   — keep SYN only (if synOnly enabled)             │
 * │ 4          │ Port       — keep 80/443 only (if webPortsOnly enabled)     │
 * │ 5          │ Local drop — drop RFC-1918 destinations (if enabled)        │
 * └────────────┴─────────────────────────────────────────────────────────────┘
 *
 * After all stages pass, the reputation check is applied separately because
 * it depends on async API data supplied by the ViewModel.
 */
object PacketFilterEngine {

    // RFC-1918 private address ranges
    private val RFC1918_RANGES = listOf(
        IpRange("10.0.0.0",   8),
        IpRange("172.16.0.0", 12),
        IpRange("192.168.0.0",16),
        IpRange("127.0.0.0",  8),   // loopback
        IpRange("169.254.0.0",16)   // link-local
    )

    private val WEB_PORTS = setOf(80, 443)

    /**
     * Apply the full 5-stage filter chain.
     *
     * @param packet Parsed packet from the TUN interface.
     * @param config Current user-controlled filter configuration.
     * @return       [FilterDecision] — PASS or DROP with reason.
     */
    fun apply(packet: Packet, config: FilterConfig): FilterDecision {

        // ── Stage 1: Protocol filter ─────────────────────────────────────────
        if (config.tcpOnly && packet.protocol != "TCP") {
            return FilterDecision.drop(FilterRule.PROTOCOL_TCP)
        }

        // ── Stage 2: Direction filter ────────────────────────────────────────
        // Outbound = source IP is in RFC-1918 (device) and destination is external
        if (config.outboundOnly && !isLocalIp(packet.sourceIp)) {
            return FilterDecision.drop(FilterRule.DIRECTION_OUT)
        }

        // ── Stage 3: TCP SYN flag filter ─────────────────────────────────────
        if (config.synOnly && !packet.isSyn) {
            return FilterDecision.drop(FilterRule.TCP_SYN)
        }

        // ── Stage 4: Port filter ─────────────────────────────────────────────
        if (config.webPortsOnly &&
            packet.destinationPort !in WEB_PORTS &&
            packet.sourcePort !in WEB_PORTS) {
            return FilterDecision.drop(FilterRule.PORT_WEB)
        }

        // ── Stage 5: Local destination drop ──────────────────────────────────
        if (config.dropLocalDestination && isLocalIp(packet.destinationIp)) {
            return FilterDecision.drop(FilterRule.LOCAL_DROP)
        }

        return FilterDecision.pass()
    }

    /**
     * Reputation check — called separately from the main chain because it
     * relies on async AbuseIPDB data resolved by the ViewModel.
     *
     * @param abuseScore  0–100 score from AbuseIPDB, or null if not fetched.
     * @param config      Current filter config with threshold.
     * @return            FilterDecision (PASS or DROP with REPUTATION reason).
     */
    fun applyReputationCheck(abuseScore: Int?, config: FilterConfig): FilterDecision {
        if (!config.suspiciousOnly) return FilterDecision.pass()
        val score = abuseScore ?: return FilterDecision.drop(FilterRule.REPUTATION)
        return if (score >= config.abuseScoreThreshold) {
            FilterDecision.pass()
        } else {
            FilterDecision.drop(FilterRule.REPUTATION)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    fun isLocalIp(ip: String): Boolean {
        val ipLong = ipToLong(ip) ?: return false
        return RFC1918_RANGES.any { range ->
            val maskBits  = 32 - range.prefixLen
            val network   = ipToLong(range.network) ?: return@any false
            (ipLong ushr maskBits) == (network ushr maskBits)
        }
    }

    private fun ipToLong(ip: String): Long? {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return null
            parts.fold(0L) { acc, part -> (acc shl 8) or part.toLong() }
        } catch (_: NumberFormatException) { null }
    }

    private data class IpRange(val network: String, val prefixLen: Int)

    /** Classify destination port into a human-readable label for the UI */
    fun portLabel(port: Int): String = when (port) {
        80   -> "HTTP"
        443  -> "HTTPS"
        53   -> "DNS"
        22   -> "SSH"
        21   -> "FTP"
        25   -> "SMTP"
        3389 -> "RDP"
        8080 -> "HTTP-ALT"
        else -> if (port > 0) "PORT $port" else "—"
    }
}
