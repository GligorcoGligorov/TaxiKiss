package com.example.myapplication

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GeoapifyService {
    @GET("v1/geocode/autocomplete")
    fun getAutocompleteSuggestions(
        @Query("text") text: String,
        @Query("apiKey") apiKey: String
    ): Call<GeoapifyResponse>
}
data class GeoapifyResponse(
    val features: List<GeoapifyFeature>?
)
data class GeoapifyFeature(
    val properties: GeoapifyResult
)

data class GeoapifyResult(
    val lat: Double,
    val lon: Double,
    val formatted: String
) {
    override fun toString(): String {
        return formatted
    }
}