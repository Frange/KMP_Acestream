package com.frange.kmpacestream.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sun.jna.Platform

@Composable
actual fun VideoPlayer(
    url: String,
    initialBufferSeconds: Int,
    onBufferChanged: (Int) -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier
) {
    if (Platform.isWindows()) {
        WindowsVideoPlayer(
            url = url,
            initialBufferSeconds = initialBufferSeconds,
            onBufferChanged = onBufferChanged,
            onToggleFullScreen = onToggleFullScreen,
            modifier = modifier
        )
    } else {
        MacVideoPlayer(
            url = url,
            initialBufferSeconds = initialBufferSeconds,
            onBufferChanged = onBufferChanged,
            onToggleFullScreen = onToggleFullScreen,
            modifier = modifier
        )
    }
}