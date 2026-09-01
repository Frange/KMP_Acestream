package com.frange.kmpacestream.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun VideoPlayer(
    url: String,
    initialBufferSeconds: Int,
    onBufferChanged: (Int) -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier
) {
    // Placeholder temporal para Android hasta implementar ExoPlayer/Media3
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text("Reproductor Android pendiente: $url")
    }
}