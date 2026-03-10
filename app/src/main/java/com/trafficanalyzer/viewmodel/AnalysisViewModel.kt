package com.trafficanalyzer.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trafficanalyzer.BuildConfig
import com.trafficanalyzer.TrafficAnalyzerApp
import com.trafficanalyzer.data.model.IpInfoResponse
import com.trafficanalyzer.data.model.Packet
import com.trafficanalyzer.data.repository.AiRepository
import com.trafficanalyzer.data.repository.IpInfoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AnalysisViewModel — Fetches IPinfo data for a selected packet,
 * then (on user request) calls AI to generate a plain-English summary.
 *
 * AI is called ONLY when the user taps the button.
 * A 2-second delay is applied before each call to avoid bursting the API.
 * No auto-retry, no countdown, no rate-limit handling.
 */
class AnalysisViewModel(
    private val ipInfoRepository: IpInfoRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val container = (application as TrafficAnalyzerApp).container
                    return AnalysisViewModel(
                        container.ipInfoRepository,
                        container.aiRepository
                    ) as T
                }
            }
    }

    // ── IPinfo state ──────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var currentPacket: Packet? = null

    fun analysePacket(packet: Packet) {
        if (currentPacket?.id == packet.id && _uiState.value is AnalysisUiState.Success) return

        currentPacket = packet
        _uiState.value = AnalysisUiState.Loading
        // Reset any prior summary when a new packet is analysed
        _summaryState.value = AiSummaryState.Idle

        viewModelScope.launch {
            val result = ipInfoRepository.getIpInfo(
                packet.destinationIp,
                BuildConfig.IPINFO_API_KEY
            )
            _uiState.value = result.fold(
                onSuccess = { AnalysisUiState.Success(packet = packet, ipInfo = it) },
                onFailure = {
                    AnalysisUiState.Error(
                        message = it.localizedMessage ?: "Unknown error",
                        packet  = packet
                    )
                }
            )
        }
    }

    // ── AI summary state ──────────────────────────────────────────────────

    private val _summaryState = MutableStateFlow<AiSummaryState>(AiSummaryState.Idle)
    val summaryState: StateFlow<AiSummaryState> = _summaryState.asStateFlow()

    /**
     * Called when the user taps "Generate AI Summary" (or "Regenerate").
     * Requires IPinfo to have succeeded first.
     * Applies a 2-second delay before the API call to avoid bursting.
     */
    fun generateSummary() {
        val successState = _uiState.value as? AnalysisUiState.Success ?: return
        if (_summaryState.value is AiSummaryState.Loading) return

        _summaryState.value = AiSummaryState.Loading

        viewModelScope.launch {
            // 2-second pause — throttles accidental double-taps & avoids burst quota
            delay(2_000)

            val result = aiRepository.summarize(
                ip           = successState.ipInfo.ip ?: successState.packet.destinationIp,
                ipInfo       = successState.ipInfo,
                protocol     = successState.packet.protocol,
                destPort     = successState.packet.destinationPort,
                packetLength = successState.packet.length,
                apiKey       = BuildConfig.AI_API_KEY
            )
            _summaryState.value = result.fold(
                onSuccess = { AiSummaryState.Success(it) },
                onFailure = { AiSummaryState.Error(it.message ?: "AI API error") }
            )
        }
    }

    /** Retry button — resets to Idle then triggers a fresh generateSummary(). */
    fun retrySummary() {
        _summaryState.value = AiSummaryState.Idle
        generateSummary()
    }
}

// ── Sealed states ─────────────────────────────────────────────────────────────

sealed class AnalysisUiState {
    data object Idle    : AnalysisUiState()
    data object Loading : AnalysisUiState()
    data class Success(val packet: Packet, val ipInfo: IpInfoResponse) : AnalysisUiState()
    data class Error(val message: String, val packet: Packet) : AnalysisUiState()
}

sealed class AiSummaryState {
    data object Idle    : AiSummaryState()
    data object Loading : AiSummaryState()
    data class Success(val summary: String) : AiSummaryState()
    data class Error(val message: String)   : AiSummaryState()
}
