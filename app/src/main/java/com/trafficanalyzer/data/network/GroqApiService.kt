package com.trafficanalyzer.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// ─── Request Models ────────────────────────────────────────────────────────────

data class GroqRequest(
    @SerializedName("model")
    val model: String = "llama-3.3-70b-versatile",
    @SerializedName("messages")
    val messages: List<GroqMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int = 500
)

data class GroqMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)

// ─── Response Models ───────────────────────────────────────────────────────────

data class GroqResponse(
    @SerializedName("choices")
    val choices: List<GroqChoice>? = null,
    @SerializedName("error")
    val error: GroqError? = null
)

data class GroqChoice(
    @SerializedName("message")
    val message: GroqMessage? = null
)

data class GroqError(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("code")
    val code: String? = null
)

/** Extract the first text response from a GroqResponse. */
fun GroqResponse.firstText(): String? =
    choices?.firstOrNull()?.message?.content

// ─── OkHttp-based Groq Service ─────────────────────────────────────────────────

private const val TAG = "GroqApiService"

class GroqApiService(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"

    suspend fun generateContent(
        apiKey: String,
        prompt: String
    ): GroqResponse = withContext(Dispatchers.IO) {

        val request = GroqRequest(
            messages = listOf(
                GroqMessage(role = "user", content = prompt)
            )
        )

        val bodyJson = gson.toJson(request)

        Log.d(TAG, "POST → $endpoint")
        Log.d(TAG, "Body length: ${bodyJson.length} chars")

        val httpRequest = Request.Builder()
            .url(endpoint)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")  // Groq uses Bearer token
            .build()

        val response = client.newCall(httpRequest).execute()
        val responseBody = response.body?.string() ?: ""

        Log.d(TAG, "HTTP ${response.code}")

        if (!response.isSuccessful) {
            Log.e(TAG, "Error body: ${responseBody.take(400)}")
            val parsed = runCatching {
                gson.fromJson(responseBody, GroqResponse::class.java)
            }.getOrNull()
            if (parsed?.error != null) return@withContext parsed
            error("HTTP ${response.code} — ${responseBody.take(300)}")
        }

        gson.fromJson(responseBody, GroqResponse::class.java)
    }
}