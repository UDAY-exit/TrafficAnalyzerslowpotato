package com.trafficanalyzer.analysis

import com.trafficanalyzer.data.model.Packet

/**
 * FilteredPacket — Wraps a [Packet] with its filter result.
 *
 * Emitted by [PacketViewModel.filteredPackets] so the UI can render
 * both the packet data and its filter outcome in a single object.
 *
 * @param packet      The raw parsed packet.
 * @param decision    Result of the filter chain (PASS / DROP + reason).
 * @param abuseScore  Cached AbuseIPDB score (0–100), null if not yet fetched.
 */
data class FilteredPacket(
    val packet:     Packet,
    val decision:   FilterDecision,
    val abuseScore: Int? = null
) {
    val isPassed: Boolean get() = decision.pass
    val isDropped: Boolean get() = !decision.pass
}
