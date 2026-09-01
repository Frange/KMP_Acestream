package com.frange.kmpacestream.shared.domain.repository

interface PreferenceSyncListener {
    fun onPreferencesChanged(
        selectedParkId: Int?,
        favoriteParkIds: Set<Int>,
        favoriteRideIds: Set<Int>
    )
}