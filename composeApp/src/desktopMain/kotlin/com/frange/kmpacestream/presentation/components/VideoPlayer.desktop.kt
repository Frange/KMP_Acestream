package com.frange.kmpacestream.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import java.awt.Frame
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.ByteBuffer
import javax.swing.SwingUtilities

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var bufferSeconds by remember { mutableIntStateOf(3) } // Buffer por defecto (3s)

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
        }
    }

    fun toggleFullScreen() {
        val window = Frame.getFrames().firstOrNull { it.isVisible }
        if (window != null) {
            val device = window.graphicsConfiguration.device
            if (device.fullScreenWindow == window) {
                device.fullScreenWindow = null
            } else {
                device.fullScreenWindow = window
            }
        }
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
            mediaPlayer.controls().stop()
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

        // Overlay de controles superiores
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause
            Text(
                text = if (mediaPlayer.status().isPlaying) "⏸" else "▶",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable {
                        if (mediaPlayer.status().isPlaying) {
                            mediaPlayer.controls().pause()
                        } else {
                            mediaPlayer.controls().play()
                        }
                    }
                    .padding(2.dp)
            )

            // Stop
            Text(
                text = "⏹",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable { mediaPlayer.controls().stop() }
                    .padding(2.dp)
            )

            // Ir al Directo (reenganchar stream)
            Text(
                text = "🔄 Directo",
                color = Color.Cyan,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { playWithCurrentBuffer() }
                    .padding(2.dp)
            )

            // Ajuste de Buffer (Subir / Bajar en segundos)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "-",
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable {
                            if (bufferSeconds > 1) {
                                bufferSeconds--
                                playWithCurrentBuffer()
                            }
                        }
                        .padding(horizontal = 4.dp)
                )

                Text(
                    text = "Buf: ${bufferSeconds}s",
                    color = Color.White,
                    fontSize = 12.sp
                )

                Text(
                    text = "+",
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable {
                            if (bufferSeconds < 30) {
                                bufferSeconds++
                                playWithCurrentBuffer()
                            }
                        }
                        .padding(horizontal = 4.dp)
                )
            }

            // Pantalla completa
            Text(
                text = "⛶",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable { toggleFullScreen() }
                    .padding(2.dp)
            )
        }
    }
}