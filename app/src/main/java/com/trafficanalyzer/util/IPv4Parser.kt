package com.trafficanalyzer.util

import com.trafficanalyzer.data.model.Packet
import java.nio.ByteBuffer

/**
 * IPv4Parser — Parses an IPv4 header from a raw [ByteBuffer].
 *
 * IPv4 Header Layout (RFC 791):
 * ┌──────┬──────────────────────┬─────────────────────────────────────┐
 * │ Byte │ Field                │ Description                         │
 * ├──────┼──────────────────────┼─────────────────────────────────────┤
 * │  0   │ Version + IHL        │ 4=IPv4; IHL in 32-bit words (min 5) │
 * │  2-3 │ Total Length         │ Header + payload in bytes           │
 * │  9   │ Protocol             │ 1=ICMP, 6=TCP, 17=UDP               │
 * │ 12-15│ Source Address       │ 4-byte IPv4 address                 │
 * │ 16-19│ Destination Address  │ 4-byte IPv4 address                 │
 * │ IHL+ │ Payload              │ TCP/UDP/ICMP data                   │
 * └──────┴──────────────────────┴─────────────────────────────────────┘
 */
object IPv4Parser {

    private const val PROTO_ICMP = 1
    private const val PROTO_TCP  = 6
    private const val PROTO_UDP  = 17

    /**
     * Parse IPv4 fields from [buffer] and return a [Packet].
     * Uses absolute get() calls so the buffer position is NOT modified.
     *
     * @throws IllegalArgumentException if buffer is too small for the declared IHL.
     */
    fun parse(buffer: ByteBuffer, id: Long): Packet {
        // Byte 0: Version (upper nibble) + IHL (lower nibble, in 32-bit words)
        val versionAndIhl = buffer.get(0).toInt() and 0xFF
        val ihl = (versionAndIhl and 0x0F) * 4

        require(buffer.remaining() >= ihl) {
            "Buffer too small for declared IHL: remaining=${buffer.remaining()}, ihl=$ihl"
        }

        // Bytes 2-3: Total Length
        val totalLength = buffer.getShort(2).toInt() and 0xFFFF

        // Byte 9: Protocol
        val protocolByte = buffer.get(9).toInt() and 0xFF
        val protocolName = when (protocolByte) {
            PROTO_ICMP -> "ICMP"
            PROTO_TCP  -> "TCP"
            PROTO_UDP  -> "UDP"
            else       -> "OTHER($protocolByte)"
        }

        // Bytes 12-15: Source IP
        val srcIp = formatIpAddress(
            buffer.get(12), buffer.get(13), buffer.get(14), buffer.get(15)
        )

        // Bytes 16-19: Destination IP
        val dstIp = formatIpAddress(
            buffer.get(16), buffer.get(17), buffer.get(18), buffer.get(19)
        )

        // Bytes IHL+: Payload (TCP or UDP header for port extraction)
        var srcPort = 0
        var dstPort = 0
        var isSyn   = false

        when (protocolByte) {
            PROTO_TCP -> {
                // TCP header: bytes 0-1 = src port, 2-3 = dst port, 13 = flags
                // SYN flag is bit 1 (0x02) of the flags byte at offset ihl+13
                if (buffer.remaining() >= ihl + 14) {
                    val ports = PortParser.extractPorts(buffer, ihl)
                    srcPort = ports.first
                    dstPort = ports.second
                    val tcpFlags = buffer.get(ihl + 13).toInt() and 0xFF
                    isSyn = (tcpFlags and 0x02) != 0
                } else if (buffer.remaining() >= ihl + 4) {
                    val ports = PortParser.extractPorts(buffer, ihl)
                    srcPort = ports.first
                    dstPort = ports.second
                }
            }
            PROTO_UDP -> {
                if (buffer.remaining() >= ihl + 4) {
                    val ports = PortParser.extractPorts(buffer, ihl)
                    srcPort = ports.first
                    dstPort = ports.second
                }
            }
            else -> { /* ICMP has no ports */ }
        }

        return Packet(
            id              = id,
            timestampMs     = System.currentTimeMillis(),
            sourceIp        = srcIp,
            destinationIp   = dstIp,
            protocol        = protocolName,
            sourcePort      = srcPort,
            destinationPort = dstPort,
            length          = totalLength,
            isSyn           = isSyn
        )
    }

    /**
     * Convert 4 raw bytes to dotted-decimal IP string.
     * Masking with 0xFF treats each byte as unsigned.
     */
    private fun formatIpAddress(b1: Byte, b2: Byte, b3: Byte, b4: Byte): String =
        "${b1.toInt() and 0xFF}.${b2.toInt() and 0xFF}" +
        ".${b3.toInt() and 0xFF}.${b4.toInt() and 0xFF}"
}
