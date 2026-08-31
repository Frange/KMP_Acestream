package com.frange.kmpacestream.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StreamItem(
    val id: String,
    val name: String,
    val isFavorite: Boolean = false,
    val lastPlayed: Long? = null
)