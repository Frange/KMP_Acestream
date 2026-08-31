package com.frange.kmpacestream.shared.data.remote

import com.frange.kmpacestream.shared.data.remote.dto.AceStreamResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class AceStreamApiClient(private val client: HttpClient) {
    private val baseUrl = "http://127.0.0.1:6878"

    suspend fun getStream(contentId: String): AceStreamResponseDto {
        return client.get("$baseUrl/ace/getstream") {
            parameter("id", contentId)
            parameter("format", "json")
        }.body()
    }
}