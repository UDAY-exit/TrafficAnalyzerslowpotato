package com.trafficanalyzer.vpn

import android.util.Log
import com.trafficanalyzer.data.model.Packet
import com.trafficanalyzer.util.IPv4Parser
import java.nio.ByteBuffer

/**
 * PacketParser — Top-level packet parsing orchestrator.
 *
 * Receives a raw [ByteBuffer] containing one IP packet from the TUN fd,
 * validates it is an IPv4 packet, then delegates to [IPv4Parser].
 *
 * Why only IPv4?
 *   The TUN builder only adds an IPv4 address and an IPv4 default route.
 *   IPv6 packets will not be routed through our TUN in this configuration.
 */
object PacketParser {

    private const val TAG = "PacketParser"
    private const val VERSION_MASK   = 0xF0
    private const val IPv4_VERSION   = 0x40   // Upper nibble == 4
    private const val MIN_IPv4_HEADER = 20

    /**
     * Parse a raw packet buffer into a [Packet] data class.
     *
     * @param buffer  ByteBuffer positioned at byte 0 of the IP header.
     * @param id      Monotonic packet ID assigned by [TunInterfaceReader].
     * @return        Parsed [Packet], or null if non-IPv4 or malformed.
     */
    fun parse(buffer: ByteBuffer, id: Long): Packet? {
        if (buffer.remaining() < MIN_IPv4_HEADER) {
            Log.v(TAG, "Packet too short (${buffer.remaining()} bytes) — discarding")
            return null
        }

        val versionAndIhl = buffer.get(0).toInt() and 0xFF
        val version = versionAndIhl and VERSION_MASK

        if (version != IPv4_VERSION) {
            // Could be IPv6 (0x60) — skip silently
            return null
        }

        return try {
            IPv4Parser.parse(buffer, id)
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing packet id=$id: ${e.message}")
            null
        }
    }
}
