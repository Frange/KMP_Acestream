package com.frange.kmpacestream.shared.domain.model

data class UserSettings(
    val bufferTimeSeconds: Int = 5,
    val maxDownloadSpeedKbps: Int = 0,
    val maxUploadSpeedKbps: Int = 0,
    val autoPlayOnSelect: Boolean = true,
    val hardwareAcceleration: Boolean = true
)