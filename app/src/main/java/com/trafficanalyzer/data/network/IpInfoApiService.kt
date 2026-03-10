package com.trafficanalyzer.data.network

import com.trafficanalyzer.data.model.IpInfoResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * IpInfoApiService — Retrofit interface for the IPinfo REST API.
 *
 * Base URL: https://ipinfo.io/
 * Endpoint: GET /{ip}/json?token=YOUR_TOKEN
 *
 * Authentication:
 *   Pass token as a query parameter.
 */
interface IpInfoApiService {

    @GET("{ip}/json")
    suspend fun getIpInfo(
        @Path("ip") ip: String,
        @Query("token") token: String = ""
    ): IpInfoResponse
}
