package com.trafficanalyzer.data.model

import com.google.gson.annotations.SerializedName

/**
 * Maps the JSON response from https://ipinfo.io/{ip}/json
 *
 * All fields are nullable because the free tier may omit some fields
 * for private/reserved IP ranges or if the API key quota is exceeded.
 *
 * Example response:
 * {
 *   "ip": "8.8.8.8",
 *   "city": "Mountain View",
 *   "region": "California",
 *   "country": "US",
 *   "loc": "37.4056,-122.0775",
 *   "org": "AS15169 Google LLC",
 *   "postal": "94043",
 *   "timezone": "America/Los_Angeles"
 * }
 */
data class IpInfoResponse(

    @SerializedName("ip")
    val ip: String? = null,

    @SerializedName("city")
    val city: String? = null,

    @SerializedName("region")
    val region: String? = null,

    /** ISO 3166-1 alpha-2 country code, e.g. "US" */
    @SerializedName("country")
    val country: String? = null,

    /** Latitude,Longitude string, e.g. "37.4056,-122.0775" */
    @SerializedName("loc")
    val loc: String? = null,

    /**
     * Autonomous System Number + organisation name,
     * e.g. "AS15169 Google LLC"
     */
    @SerializedName("org")
    val org: String? = null,

    @SerializedName("postal")
    val postal: String? = null,

    @SerializedName("timezone")
    val timezone: String? = null,

    /** Only present when the IP is a bogon / private address. */
    @SerializedName("bogon")
    val bogon: Boolean? = null
)

/** Convenience extension to parse the ASN number out of the org field. */
val IpInfoResponse.asn: String
    get() = org?.substringBefore(" ") ?: "N/A"

/** Convenience extension to parse the organisation name out of the org field. */
val IpInfoResponse.orgName: String
    get() = org?.substringAfter(" ") ?: "N/A"
