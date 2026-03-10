package com.trafficanalyzer.data.repository

import android.util.Log
import com.trafficanalyzer.data.model.IpInfoResponse
import com.trafficanalyzer.data.model.orgName
import com.trafficanalyzer.data.network.GroqApiService
import com.trafficanalyzer.data.network.firstText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GroqRepo"

/**
 * GeminiRepository — Builds a short prompt from IPinfo data and calls
 * Groq once via OkHttp. No retries, no fallback models.
 */
class GeminiRepository(
    private val groqApiService: GroqApiService
) {
    private var callCount = 0

    suspend fun summarize(
        ip: String,
        ipInfo: IpInfoResponse,
        protocol: String,
        destPort: Int,
        packetLength: Int,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {

        if (apiKey.isBlank() || apiKey == "YOUR_GROQ_API_KEY_HERE") {
            return@withContext Result.failure(
                IllegalStateException("Groq API key is not set in build.gradle.kts → GROQ_API_KEY.")
            )
        }

        callCount++
        Log.d(TAG, "Call #$callCount — IP: $ip")

        val prompt = buildPrompt(ip, ipInfo, protocol, destPort, packetLength)

        runCatching {
            val response = groqApiService.generateContent(apiKey.trim(), prompt)

            // Surface a structured Groq error if present
            if (response.error != null) {
                val msg = response.error.message ?: "Unknown Groq error"
                Log.e(TAG, "Groq error: $msg")
                error(msg)
            }

            val text = response.firstText()
            Log.d(TAG, "Call #$callCount success — ${text?.length ?: 0} chars returned")
            text ?: error("Groq returned an empty response.")
        }.onFailure { err ->
            Log.e(TAG, "Call #$callCount failed: ${err.message}")
        }
    }

    // ── Prompt — kept under 120 words ────────────────────────────────────────

    private fun buildPrompt(
        ip: String,
        ipInfo: IpInfoResponse,
        protocol: String,
        destPort: Int,
        packetLength: Int
    ): String {
        val location = listOfNotNull(ipInfo.city, ipInfo.region, ipInfo.country)
            .joinToString(", ").ifBlank { "Unknown" }

        val portLabel = when (destPort) {
            80   -> "HTTP"
            443  -> "HTTPS"
            53   -> "DNS"
            22   -> "SSH"
            3389 -> "RDP"
            else -> "port $destPort"
        }

        return """
You are a cybersecurity teacher. Summarize this network packet for undergraduate students in 4-5 plain sentences. No bullet points. Friendly tone.

IP: $ip
Location: $location
Organisation: ${ipInfo.orgName}
Protocol: $protocol
Port: $destPort ($portLabel)
Packet size: $packetLength bytes
Private/bogon: ${if (ipInfo.bogon == true) "Yes" else "No"}

Explain: who owns this IP, what type of traffic this likely is, whether it looks normal or suspicious, and one tip for students.
        """.trimIndent()
    }
}
