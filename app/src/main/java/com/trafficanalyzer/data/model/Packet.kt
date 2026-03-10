package com.trafficanalyzer.data.model

/**
 * Represents a single captured network packet parsed from the TUN interface.
 *
 * This is a pure data class — it holds only the fields extracted from the
 * IPv4 header (and optionally TCP/UDP header). No raw bytes are stored here
 * to keep memory usage low when thousands of packets are captured.
 *
 * Demo limitation: We only parse IPv4 packets. IPv6 packets are discarded
 * because the TUN interface is configured for IPv4-only routing.
 */
data class Packet(
    /** Monotonically increasing ID, used as the Compose list key */
    val id: Long,

    /** Epoch milliseconds when the packet was read from the TUN fd */
    val timestampMs: Long,

    /** Dotted-decimal IPv4 source address, e.g. "192.168.1.5" */
    val sourceIp: String,

    /** Dotted-decimal IPv4 destination address, e.g. "8.8.8.8" */
    val destinationIp: String,

    /**
     * IP protocol number mapped to a human-readable string.
     * Common values: "TCP" (6), "UDP" (17), "ICMP" (1), "OTHER"
     */
    val protocol: String,

    /**
     * Source port extracted from the TCP or UDP header.
     * 0 if the protocol is ICMP or unrecognised.
     */
    val sourcePort: Int,

    /**
     * Destination port extracted from the TCP or UDP header.
     * 0 if the protocol is ICMP or unrecognised.
     */
    val destinationPort: Int,

    /** Total length field from the IPv4 header (bytes) */
    val length: Int,

    /**
     * True if the TCP SYN flag is set (new connection attempt).
     * Always false for non-TCP packets.
     */
    val isSyn: Boolean = false
)
