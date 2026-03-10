package com.trafficanalyzer.analysis

/**
 * FilterConfig — Runtime-configurable filter toggles.
 *
 * Each boolean toggle maps directly to one filter stage in the engine.
 * This is held in a StateFlow inside PacketViewModel so the UI and engine
 * always see the same state.
 *
 * @param mode                 Active preset mode.
 * @param tcpOnly              Stage 1 — keep only TCP packets.
 * @param outboundOnly         Stage 2 — keep only packets where srcIp is device-local.
 * @param synOnly              Stage 3 — keep only TCP SYN packets (new connections).
 * @param webPortsOnly         Stage 4 — keep only dst ports 80 or 443.
 * @param dropLocalDestination Stage 5 — drop packets with RFC-1918 destination IPs.
 * @param suspiciousOnly       Post-stage — hide packets whose IP has no AbuseIPDB score.
 * @param abuseScoreThreshold  Minimum abuse score (0–100) to classify as suspicious.
 * @param showDropped          UI toggle — grey-out dropped rows instead of hiding them.
 */
data class FilterConfig(
    val mode:                 FilterMode = FilterMode.RAW,
    val tcpOnly:              Boolean    = false,
    val outboundOnly:         Boolean    = false,
    val synOnly:              Boolean    = false,
    val webPortsOnly:         Boolean    = false,
    val dropLocalDestination: Boolean    = false,
    val suspiciousOnly:       Boolean    = false,
    val abuseScoreThreshold:  Int        = 25,
    val showDropped:          Boolean    = true
) {
    companion object {
        /** Preset configs for each FilterMode */
        fun forMode(mode: FilterMode): FilterConfig = when (mode) {
            FilterMode.RAW -> FilterConfig(mode = mode)
            FilterMode.CONNECTIONS -> FilterConfig(
                mode        = mode,
                tcpOnly     = true,
                outboundOnly= true,
                synOnly     = true,
                dropLocalDestination = true
            )
            FilterMode.WEB_ONLY -> FilterConfig(
                mode         = mode,
                tcpOnly      = true,
                outboundOnly = true,
                webPortsOnly = true,
                dropLocalDestination = true
            )
            FilterMode.SUSPICIOUS_ONLY -> FilterConfig(
                mode             = mode,
                tcpOnly          = true,
                outboundOnly     = true,
                dropLocalDestination = true,
                suspiciousOnly   = true,
                abuseScoreThreshold = 25
            )
            FilterMode.CUSTOM -> FilterConfig(mode = mode)
        }
    }
}
