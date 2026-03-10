package com.trafficanalyzer.data.repository

import com.trafficanalyzer.data.model.IpInfoResponse
import com.trafficanalyzer.data.network.IpInfoApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * IpInfoRepository — Wraps the Retrofit API call with error handling.
 * No DI annotations — instantiated directly in AppContainer.
 */
class IpInfoRepository(
    private val apiService: IpInfoApiService
) {
    suspend fun getIpInfo(ip: String, token: String = ""): Result<IpInfoResponse> = withContext(Dispatchers.IO) {
        runCatching { apiService.getIpInfo(ip, token) }
    }
}
