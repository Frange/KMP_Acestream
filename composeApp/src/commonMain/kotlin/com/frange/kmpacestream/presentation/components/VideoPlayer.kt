package com.frange.kmpacestream.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoPlayer(
    url: String,
    initialBufferSeconds: Int = 3,
    onBufferChanged: (Int) -> Unit = {},
    onToggleFullScreen: () -> Unit = {},
    modifier: Modifier = Modifier
)