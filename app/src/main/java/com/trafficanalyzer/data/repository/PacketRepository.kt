package com.trafficanalyzer.data.repository

import com.trafficanalyzer.data.model.Packet
import com.trafficanalyzer.vpn.TrafficVpnService
import kotlinx.coroutines.flow.SharedFlow

/**
 * PacketRepository — Single source of truth for captured packet data.
 * No DI framework needed — instantiated directly in AppContainer.
 */
class PacketRepository {

    /** Live stream of parsed packets from the TUN interface */
    val packetFlow: SharedFlow<Packet>
        get() = TrafficVpnService.packetFlow

    /** Whether the VPN capture is currently active */
    val isCapturing: Boolean
        get() = TrafficVpnService.isRunning
}
