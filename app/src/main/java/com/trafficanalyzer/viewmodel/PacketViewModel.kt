package com.trafficanalyzer.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trafficanalyzer.TrafficAnalyzerApp
import com.trafficanalyzer.analysis.FilterConfig
import com.trafficanalyzer.analysis.FilterMode
import com.trafficanalyzer.analysis.FilterStats
import com.trafficanalyzer.analysis.FilteredPacket
import com.trafficanalyzer.analysis.PacketFilterEngine
import com.trafficanalyzer.data.model.Packet
import com.trafficanalyzer.data.repository.PacketRepository
import com.trafficanalyzer.vpn.TrafficVpnService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PacketViewModel — Manages VPN lifecycle, raw packet accumulation, and the
 * full filter engine pipeline.
 *
 * Key design: [collectJob] is created fresh each time capture starts and
 * cancelled the moment capture stops. Because [TrafficVpnService.packetFlow]
 * is also replaced with a new instance on every capture start (via
 * resetPacketFlow()), there is NO way for stale packets to arrive after stop.
 */
class PacketViewModel(
    application: Application,
    private val packetRepository: PacketRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG         = "PacketViewModel"
        private const val MAX_PACKETS = 500

        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.AndroidViewModelFactory(application) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    val container = (application as TrafficAnalyzerApp).container
                    return PacketViewModel(application, container.packetRepository) as T
                }
            }
    }

    // ── Raw packet list ───────────────────────────────────────────────────────
    private val _rawPackets = MutableStateFlow<List<Packet>>(emptyList())
    val rawPackets: StateFlow<List<Packet>> = _rawPackets.asStateFlow()

    /** Backwards-compatible alias */
    val packets: StateFlow<List<Packet>> = _rawPackets

    // ── Filtered packet list ──────────────────────────────────────────────────
    private val _filteredPackets = MutableStateFlow<List<FilteredPacket>>(emptyList())
    val filteredPackets: StateFlow<List<FilteredPacket>> = _filteredPackets.asStateFlow()

    // ── Filter config & stats ─────────────────────────────────────────────────
    private val _filterConfig = MutableStateFlow(FilterConfig())
    val filterConfig: StateFlow<FilterConfig> = _filterConfig.asStateFlow()

    private val _filterStats = MutableStateFlow(FilterStats())
    val filterStats: StateFlow<FilterStats> = _filterStats.asStateFlow()

    // ── Capture state ─────────────────────────────────────────────────────────
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _vpnPermissionNeeded = MutableStateFlow(false)
    val vpnPermissionNeeded: StateFlow<Boolean> = _vpnPermissionNeeded.asStateFlow()

    private var vpnPermissionIntent: Intent? = null

    /**
     * The active packet collection coroutine.
     * Cancelled on stopCapture() so NO more packets are processed after stop.
     * A new job is created fresh on each launchVpnService() call.
     */
    private var collectJob: Job? = null

    // ── Filter controls ───────────────────────────────────────────────────────

    fun setFilterMode(mode: FilterMode) {
        val newConfig = FilterConfig.forMode(mode)
        _filterConfig.value = newConfig
        rebuildFilteredList(newConfig)
    }

    fun updateFilterConfig(config: FilterConfig) {
        _filterConfig.value = config
        rebuildFilteredList(config)
    }

    private fun rebuildFilteredList(config: FilterConfig) {
        val raw      = _rawPackets.value
        var stats    = FilterStats()
        val filtered = mutableListOf<FilteredPacket>()
        for (packet in raw) {
            val decision = PacketFilterEngine.apply(packet, config)
            stats = if (decision.pass) stats.incrementPass()
                    else stats.incrementDrop(decision.rule)
            if (decision.pass || config.showDropped) {
                filtered.add(FilteredPacket(packet = packet, decision = decision))
            }
        }
        _filterStats.value     = stats
        _filteredPackets.value = filtered
    }

    // ── VPN lifecycle ─────────────────────────────────────────────────────────

    fun startCapture(context: Context) {
        val permissionIntent = VpnService.prepare(context)
        if (permissionIntent != null) {
            vpnPermissionIntent = permissionIntent
            _vpnPermissionNeeded.value = true
            Log.d(TAG, "VPN permission required")
            return
        }
        launchVpnService(context)
    }

    fun onVpnPermissionGranted(context: Context) {
        _vpnPermissionNeeded.value = false
        vpnPermissionIntent        = null
        launchVpnService(context)
    }

    fun onVpnPermissionDenied() {
        _vpnPermissionNeeded.value = false
        vpnPermissionIntent        = null
    }

    fun getVpnPermissionIntent(): Intent? = vpnPermissionIntent

    fun stopCapture(context: Context) {
        // 1. Cancel the collect coroutine FIRST — no more packets processed
        collectJob?.cancel()
        collectJob = null

        // 2. Send explicit STOP action so the service does not restart
        val intent = Intent(context, TrafficVpnService::class.java).apply {
            action = TrafficVpnService.ACTION_STOP
        }
        context.startService(intent)

        // 3. Also call stopService as a belt-and-suspenders measure
        context.stopService(Intent(context, TrafficVpnService::class.java))

        _isCapturing.value = false
        Log.d(TAG, "Capture stopped — collectJob cancelled")
    }

    fun clearPackets() {
        _rawPackets.value      = emptyList()
        _filteredPackets.value = emptyList()
        _filterStats.value     = FilterStats()
    }

    private fun launchVpnService(context: Context) {
        // Cancel any stale collector from a previous session
        collectJob?.cancel()
        collectJob = null

        clearPackets()

        // Start the VPN service (which calls resetPacketFlow() internally,
        // replacing the SharedFlow with a brand-new instance)
        val intent = Intent(context, TrafficVpnService::class.java)
        context.startForegroundService(intent)
        _isCapturing.value = true

        // Subscribe to the NEW flow instance returned by packetRepository
        // (packetRepository.packetFlow delegates to TrafficVpnService.packetFlow
        //  which is now the fresh instance)
        collectJob = viewModelScope.launch {
            // Small delay to let the service establish the TUN and reset the flow
            kotlinx.coroutines.delay(300)
            packetRepository.packetFlow.collect { packet ->
                // Guard: do not process if already stopped
                if (!_isCapturing.value) return@collect

                val config   = _filterConfig.value
                val decision = PacketFilterEngine.apply(packet, config)

                _rawPackets.update { current ->
                    val updated = listOf(packet) + current
                    if (updated.size > MAX_PACKETS) updated.take(MAX_PACKETS) else updated
                }

                _filterStats.update { stats ->
                    if (decision.pass) stats.incrementPass()
                    else stats.incrementDrop(decision.rule)
                }

                if (decision.pass || config.showDropped) {
                    val wrapped = FilteredPacket(packet = packet, decision = decision)
                    _filteredPackets.update { current ->
                        val updated = listOf(wrapped) + current
                        if (updated.size > MAX_PACKETS) updated.take(MAX_PACKETS) else updated
                    }
                }
            }
        }

        Log.d(TAG, "Capture started — new collectJob launched")
    }
}
