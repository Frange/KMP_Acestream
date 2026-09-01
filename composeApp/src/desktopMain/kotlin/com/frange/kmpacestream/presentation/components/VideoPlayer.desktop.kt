package com.frange.kmpacestream.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.ByteBuffer
import javax.swing.SwingUtilities

@Composable
actual fun VideoPlayer(
    url: String,
    initialBufferSeconds: Int,
    onBufferChanged: (Int) -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier
) {
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var bufferSeconds by remember { mutableIntStateOf(initialBufferSeconds) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    val mediaPlayerFactory = remember {
        val os = System.getProperty("os.name").lowercase()
        val extraArgs = if (os.contains("win")) {
            arrayOf(
                "--verbose=2",
                "--no-video-title-show",
                "--vout=direct3d11", // Forzamos DirectX 11 en Windows para que pinte el frame
                "--avcodec-hw=any"   // Permite decodificación por hardware compatible
            )
        } else {
            arrayOf(
                "--verbose=2",
                "--no-video-title-show"
            )
        }
        MediaPlayerFactory(*extraArgs)
    }
    
    val mediaPlayer = remember {
        mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer()
    }

    fun playMedia() {
        if (url.isNotEmpty()) {
            val options = arrayOf(":network-caching=${bufferSeconds * 1000}")
            mediaPlayer.media().play(url, *options)
            mediaPlayer.audio().isMute = isMuted
            isPlaying = true
        }
    }

    fun stopMedia() {
        try {
            if (mediaPlayer.status().isPlaying) {
                mediaPlayer.controls().stop()
            }
        } catch (_: Exception) {}
        isPlaying = false
        currentFrame = null
    }

    // Volvemos a asociarlo a la URL para que el stream se inicialice
    // y recupere el contexto gráfico al cambiar de tamaño o pantalla completa.
    DisposableEffect(url) {
        val renderCallback = RenderCallback { _, nativeBuffers, bufferFormat ->
            if (nativeBuffers.isNullOrEmpty() || bufferFormat == null) return@RenderCallback
            val buffer = nativeBuffers[0]
            val width = bufferFormat.width
            val height = bufferFormat.height

            if (width > 0 && height > 0) {
                val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                val pixelData = (bufferedImage.raster.dataBuffer as DataBufferInt).data

                buffer.rewind()
                buffer.asIntBuffer().get(pixelData)

                val bitmap = bufferedImage.toComposeImageBitmap()

                SwingUtilities.invokeLater {
                    currentFrame = bitmap
                    isPlaying = true
                }
            }
        }

        val bufferFormatCallback = object : BufferFormatCallback {
            override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                return RV32BufferFormat(sourceWidth, sourceHeight)
            }
            override fun allocatedBuffers(buffers: Array<out ByteBuffer>?) {}
        }

        mediaPlayer.videoSurface().set(
            CallbackVideoSurface(bufferFormatCallback, renderCallback, true, null)
        )

        playMedia()

        onDispose {
            stopMedia()
            try {
                mediaPlayer.release()
                mediaPlayerFactory.release()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onToggleFullScreen() }
                )
            }
    ) {
        currentFrame?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "AceStream Video",
                modifier = Modifier.fillMaxSize()
            )
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Cargando vídeo...", color = Color.Gray, fontSize = 16.sp)
        }

        // Botón FullScreen Superior Derecho
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                .clickable { onToggleFullScreen() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(text = "⛶", color = Color.White, fontSize = 22.sp)
        }

        // Barra de Control Inferior
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Stop
            Text(
                text = if (!isPlaying) "▶" else "⏹",
                color = if (!isPlaying) Color.Green else Color.Red,
                fontSize = 22.sp,
                modifier = Modifier.clickable {
                    if (isPlaying) stopMedia() else playMedia()
                }.padding(4.dp)
            )

            // Mute / Unmute
            Text(
                text = if (isMuted) "🔇" else "🔊",
                color = if (isMuted) Color.Red else Color.White,
                fontSize = 20.sp,
                modifier = Modifier.clickable {
                    isMuted = !isMuted
                    mediaPlayer.audio().isMute = isMuted
                }.padding(4.dp)
            )

            // Buffer Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "➖",
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable {
                        if (bufferSeconds > 1) {
                            bufferSeconds--
                            onBufferChanged(bufferSeconds)
                            playMedia()
                        }
                    }.padding(4.dp)
                )

                Text(text = "Buf: ${bufferSeconds}s", color = Color.White, fontSize = 15.sp)

                Text(
                    text = "➕",
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable {
                        if (bufferSeconds < 30) {
                            bufferSeconds++
                            onBufferChanged(bufferSeconds)
                            playMedia()
                        }
                    }.padding(4.dp)
                )
            }
        }
    }
}