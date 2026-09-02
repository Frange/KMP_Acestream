package com.frange.kmpacestream.presentation.components

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun TabletVideoPlayer(
    url: String,
    initialBufferSeconds: Int,
    onBufferChanged: (Int) -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    var bufferSeconds by remember { mutableIntStateOf(initialBufferSeconds) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var volumeValue by remember { mutableFloatStateOf(100f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    fun playMedia() {
        if (url.isNotEmpty()) {
            try {
                errorMessage = null
                val mediaItem = MediaItem.fromUri(url)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
                isPlaying = true
            } catch (e: Exception) {
                errorMessage = "Error play: ${e.message}"
            }
        }
    }

    fun stopMedia() {
        try {
            exoPlayer.stop()
        } catch (_: Exception) {}
        isPlaying = false
    }

    DisposableEffect(url) {
        playMedia()
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onToggleFullScreen() })
            }
    ) {
        // Reproductor de vídeo nativo ocupando toda la pantalla de la tablet
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Controles personalizados idénticos a las demás plataformas
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Botón de pantalla completa (⛶) adaptado al espacio de tablet
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                .clickable { onToggleFullScreen() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(text = "⛶", color = Color.White, fontSize = 24.sp)
        }

        // Barra inferior unificada y holgada para pantallas grandes
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Play / Stop
            Text(
                text = if (!isPlaying) "▶" else "⏹",
                color = if (!isPlaying) Color.Green else Color.Red,
                fontSize = 24.sp,
                modifier = Modifier.clickable {
                    if (isPlaying) {
                        stopMedia()
                    } else {
                        playMedia()
                    }
                }.padding(4.dp)
            )

            // Control de Volumen con Slider (más ancho en tablets)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.width(180.dp)
            ) {
                Text(
                    text = if (isMuted || volumeValue == 0f) "🔇" else "🔊",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable {
                        isMuted = !isMuted
                        exoPlayer.volume = if (isMuted) 0f else (volumeValue / 100f)
                    }.padding(4.dp)
                )

                Slider(
                    value = if (isMuted) 0f else volumeValue,
                    onValueChange = { newValue ->
                        isMuted = false
                        volumeValue = newValue
                        exoPlayer.volume = newValue / 100f
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Control de Buffer
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("➖", color = Color.Yellow, fontSize = 18.sp, modifier = Modifier.clickable {
                    if (bufferSeconds > 1) { bufferSeconds--; onBufferChanged(bufferSeconds); playMedia() }
                }.padding(4.dp))
                Text("Buf: ${bufferSeconds}s", color = Color.White, fontSize = 16.sp)
                Text("➕", color = Color.Yellow, fontSize = 18.sp, modifier = Modifier.clickable {
                    if (bufferSeconds < 30) { bufferSeconds++; onBufferChanged(bufferSeconds); playMedia() }
                }.padding(4.dp))
            }
        }
    }
}