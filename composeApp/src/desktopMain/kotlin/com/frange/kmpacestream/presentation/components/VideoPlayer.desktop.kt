package com.frange.kmpacestream.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }

    val mediaPlayerFactory = remember {
        MediaPlayerFactory(
            "--no-video-title-show",
            "--quiet"
        )
    }

    val mediaPlayer = remember { mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer() }

    DisposableEffect(url) {
        val renderCallback = object : RenderCallback {
            override fun display(
                mediaPlayer: MediaPlayer?,
                nativeBuffers: Array<out ByteBuffer>?,
                bufferFormat: BufferFormat?
            ) {
                if (nativeBuffers == null || bufferFormat == null) return
                val buffer = nativeBuffers[0]

                // En VLCj 4 se accede a sourceWidth y sourceHeight
                val width = bufferFormat.width
                val height = bufferFormat.height

                if (width <= 0 || height <= 0) return

                val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                val pixelData = (bufferedImage.raster.dataBuffer as java.awt.image.DataBufferInt).data

                buffer.rewind()
                buffer.asIntBuffer().get(pixelData)

                currentFrame = bufferedImage.toComposeImageBitmap()
            }
        }

        val bufferFormatCallback = object : BufferFormatCallback {
            override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                return RV32BufferFormat(sourceWidth, sourceHeight)
            }

            override fun allocatedBuffers(buffers: Array<out ByteBuffer>?) {
                // Implementación requerida por el contrato de BufferFormatCallback
            }
        }

        mediaPlayer.videoSurface().set(
            mediaPlayerFactory.videoSurfaces().newVideoSurface(bufferFormatCallback, renderCallback, true)
        )

        if (url.isNotEmpty()) {
            mediaPlayer.media().play(url)
        }

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
    }
}