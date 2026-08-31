package com.frange.kmpacestream.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AceStreamResponseDto(
    @SerialName("response") val response: AceStreamPlaybackDataDto? = null,
    @SerialName("error") val error: String? = null
)

@Serializable
data class AceStreamPlaybackDataDto(
    @SerialName("playback_url") val playbackUrl: String,
    @SerialName("stat_url") val statUrl: String? = null
)