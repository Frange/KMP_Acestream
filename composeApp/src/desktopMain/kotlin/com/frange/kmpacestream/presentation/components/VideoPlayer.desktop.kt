package com.frange.kmpacestream.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
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
import kotlin.time.Duration.Companion.milliseconds

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

    val mediaPlayerFactory = remember {
        MediaPlayerFactory(
            "--no-video-title-show",
            "--quiet"
        )
    }

    val mediaPlayer = remember { mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer() }

    fun playWithCurrentBuffer() {
        if (url.isNotEmpty()) {
            val options = arrayOf(":network-caching=${bufferSeconds * 1000}")
            mediaPlayer.media().play(url, *options)
            isPlaying = true
        }
    }

    fun stopPlayback() {
        mediaPlayer.controls().stop()
        isPlaying = false
        currentFrame = null // Limpia el fotograma al parar
    }

    DisposableEffect(url) {
        val renderCallback = object : RenderCallback {
            override fun display(
                mediaPlayer: MediaPlayer?,
                nativeBuffers: Array<out ByteBuffer>?,
                bufferFormat: BufferFormat?
            ) {
                if (nativeBuffers == null || bufferFormat == null || nativeBuffers.isEmpty()) return
                val buffer = nativeBuffers[0]

                val width = bufferFormat.getWidth()
                val height = bufferFormat.getHeight()

                if (width <= 0 || height <= 0) return

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

        val videoSurface = CallbackVideoSurface(bufferFormatCallback, renderCallback, true, null)
        mediaPlayer.videoSurface().set(videoSurface)

        playWithCurrentBuffer()

        onDispose {
            stopPlayback()
            mediaPlayer.release()
            mediaPlayerFactory.release()
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        currentFrame?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "AceStream Video",
                modifier = Modifier.fillMaxSize()
            )
        }

        // Botón FullScreen en la esquina superior derecha
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                .clickable { onToggleFullScreen() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "⛶",
                color = Color.White,
                fontSize = 22.sp
            )
        }

        // Barra de Controles Simplificada y Centrada Abajo
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Play (Verde) / Stop (Rojo)
            if (!isPlaying) {
                Text(
                    text = "▶",
                    color = Color.Green,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .clickable { playWithCurrentBuffer() }
                        .padding(4.dp)
                )
            } else {
                Text(
                    text = "⏹",
                    color = Color.Red,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .clickable { stopPlayback() }
                        .padding(4.dp)
                )
            }

            // Selector de Buffer en segundos (Aplica y relanza al directo automáticamente)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Play (Verde) / Stop (Rojo)
                Text(
                    text = if (!isPlaying) "▶" else "⏹",
                    color = if (!isPlaying) Color.Green else Color.Red,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .clickable {
                            if (isPlaying) {
                                stopPlayback() 
                            } else {
                                playWithCurrentBuffer()
                            }
                        }
                        .padding(4.dp)
                )

                Text(
                    text = "Buf: ${bufferSeconds}s",
                    color = Color.White,
                    fontSize = 16.sp
                )

                Text(
                    text = "➕",
                    color = Color.Yellow,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable {
                            if (bufferSeconds < 30) {
                                bufferSeconds++
                                onBufferChanged(bufferSeconds)
                                playWithCurrentBuffer()
                            }
                        }
                        .padding(4.dp)
                )
            }
        }
    }
}