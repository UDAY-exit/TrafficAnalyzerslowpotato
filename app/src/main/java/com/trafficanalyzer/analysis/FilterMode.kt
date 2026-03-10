package com.trafficanalyzer.analysis

/**
 * FilterMode — Top-level preset that drives which filter stages are active.
 *
 * Selecting a mode automatically sets the filter toggles so that the user
 * gets a meaningful preset, but individual toggles can still be adjusted
 * after a mode is selected (mode becomes CUSTOM automatically).
 */
enum class FilterMode(val label: String, val description: String) {
    RAW(
        label       = "RAW",
        description = "Show all captured packets — no filtering"
    ),
    CONNECTIONS(
        label       = "CONNECTIONS",
        description = "TCP SYN only — new connection attempts"
    ),
    WEB_ONLY(
        label       = "WEB",
        description = "Ports 80/443 only — HTTP and HTTPS traffic"
    ),
    SUSPICIOUS_ONLY(
        label       = "SUSPICIOUS",
        description = "Only IPs with AbuseIPDB score above threshold"
    ),
    CUSTOM(
        label       = "CUSTOM",
        description = "Manual toggle configuration"
    )
}
