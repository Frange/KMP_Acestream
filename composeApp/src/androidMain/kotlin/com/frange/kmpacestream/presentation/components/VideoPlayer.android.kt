package com.frange.kmpacestream.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration

@Composable
actual fun VideoPlayer(
    url: String,
    initialBufferSeconds: Int,
    onBufferChanged: (Int) -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        TabletVideoPlayer(
            url = url,
            initialBufferSeconds = initialBufferSeconds,
            onBufferChanged = onBufferChanged,
            onToggleFullScreen = onToggleFullScreen,
            modifier = modifier
        )
    } else {
        PhoneVideoPlayer(
            url = url,
            initialBufferSeconds = initialBufferSeconds,
            onBufferChanged = onBufferChanged,
            onToggleFullScreen = onToggleFullScreen,
            modifier = modifier
        )
    }
}