package com.frange.kmpacestream.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AceStreamStats(
    val downloadSpeedKbps: Int = 0,
    val uploadSpeedKbps: Int = 0,
    val bufferPercentage: Int = 0,
    val isLive: Boolean = true,
    val peersCount: Int = 0,
    val liveDelaySeconds: Int = 0
)

@Serializable
data class StreamInfo(
    val playbackUrl: String,
    val statUrl: String? = null
)

@Serializable
data class StreamItem(
    val id: String,
    val name: String,
    val isFavorite: Boolean = false,
    val lastPlayed: Long? = null
)

@Serializable
data class UserSettings(
    val bufferTimeSeconds: Int = 5,
    val maxDownloadSpeedKbps: Int = 0,
    val maxUploadSpeedKbps: Int = 0,
    val autoPlayOnSelect: Boolean = true,
    val hardwareAcceleration: Boolean = true
)