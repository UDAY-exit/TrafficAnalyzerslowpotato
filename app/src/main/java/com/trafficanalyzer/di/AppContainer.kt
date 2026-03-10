package com.trafficanalyzer.di

import com.trafficanalyzer.BuildConfig
import com.trafficanalyzer.data.network.GroqApiService
import com.trafficanalyzer.data.network.IpInfoApiService
import com.trafficanalyzer.data.repository.AiRepository
import com.trafficanalyzer.data.repository.IpInfoRepository
import com.trafficanalyzer.data.repository.PacketRepository
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * AppContainer — Manual dependency injection container.
 * Holds singleton instances of all shared dependencies.
 * Created once in TrafficAnalyzerApp.onCreate().
 */
class AppContainer {

    // ── Shared OkHttpClient ───────────────────────────────────────────────────
    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }
        builder.build()
    }

    private val gson by lazy { GsonBuilder().setLenient().create() }

    // ── IPinfo Retrofit ───────────────────────────────────────────────────────
    private val ipInfoRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.IPINFO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val ipInfoApiService: IpInfoApiService by lazy {
        ipInfoRetrofit.create(IpInfoApiService::class.java)
    }

    // ── Groq — plain OkHttp (same approach as before) ────────────────────────
    private val groqApiService: GroqApiService by lazy {
        GroqApiService(okHttpClient, gson)
    }

    // ── Repositories ──────────────────────────────────────────────────────────
    val packetRepository: PacketRepository by lazy { PacketRepository() }
    val ipInfoRepository: IpInfoRepository by lazy { IpInfoRepository(ipInfoApiService) }
    val aiRepository: AiRepository by lazy { AiRepository(groqApiService) }
}
