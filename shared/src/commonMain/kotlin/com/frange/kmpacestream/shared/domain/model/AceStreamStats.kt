package com.frange.kmpacestream.shared.domain.model

data class AceStreamStats(
    val downloadSpeedKbps: Int = 0,
    val uploadSpeedKbps: Int = 0,
    val bufferPercentage: Int = 0,
    val isLive: Boolean = true,
    val peersCount: Int = 0,
    val liveDelaySeconds: Int = 0
)