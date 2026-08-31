package com.frange.kmpacestream.shared.domain.model

data class StreamInfo(
    val playbackUrl: String,
    val statUrl: String? = null
)