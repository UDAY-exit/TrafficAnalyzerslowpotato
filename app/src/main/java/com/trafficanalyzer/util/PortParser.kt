package com.trafficanalyzer.util

import java.nio.ByteBuffer

/**
 * PortParser — Extracts source and destination port numbers from TCP/UDP headers.
 *
 * Both TCP and UDP share the same first 4 bytes layout:
 * ┌──────────┬──────────────────────────┐
 * │ Bytes    │ Field                    │
 * ├──────────┼──────────────────────────┤
 * │ 0-1      │ Source Port (16-bit)     │
 * │ 2-3      │ Destination Port (16-bit)│
 * └──────────┴──────────────────────────┘
 *
 * This means we can extract ports from both TCP and UDP with identical code.
 * The rest of the TCP header (sequence number, flags, etc.) is ignored here
 * because this is a traffic analysis demo, not a full TCP stack.
 */
object PortParser {

    /**
     * Extract source and destination ports from a TCP or UDP header.
     *
     * @param buffer        The raw packet buffer (absolute indices used, position unchanged).
     * @param headerOffset  Byte offset where the transport-layer header starts
     *                      (20 bytes for standard IPv4 with no options).
     * @return Pair(sourcePort, destinationPort) as unsigned Int values (0–65535).
     */
    fun extractPorts(buffer: ByteBuffer, headerOffset: Int): Pair<Int, Int> {
        // getShort() returns a signed Short; masking with 0xFFFF gives unsigned int
        val srcPort = buffer.getShort(headerOffset).toInt() and 0xFFFF
        val dstPort = buffer.getShort(headerOffset + 2).toInt() and 0xFFFF
        return Pair(srcPort, dstPort)
    }
}
