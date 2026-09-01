package com.frange.kmpacestream.presentation.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
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
fun WindowsVideoPlayer(
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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    /*
     * IMPORTANTE:
     *
     * De momento NO usamos:
     *
     *     --avcodec-hw=any
     *
     * para descartar problemas de decodificación/renderizado por hardware
     * en Windows.
     */
    val mediaPlayerFactory = remember {
        try {
            MediaPlayerFactory(
                "--verbose=2",
                "--no-video-title-show",
                "--no-osd",
                "--avcodec-hw=none"
            )
        } catch (e: Exception) {
            errorMessage = "Error init VLC: ${e.message}"
            null
        }
    }

    val mediaPlayer = remember(mediaPlayerFactory) {
        mediaPlayerFactory
            ?.mediaPlayers()
            ?.newEmbeddedMediaPlayer()
    }

    fun playMedia() {
        if (url.isEmpty() || mediaPlayer == null) {
            return
        }

        try {
            errorMessage = null

            val options = arrayOf(
                ":network-caching=${bufferSeconds * 1000}"
            )

            mediaPlayer.media().play(
                url,
                *options
            )

            mediaPlayer.audio().isMute = isMuted

            isPlaying = true

        } catch (e: Exception) {
            errorMessage = "Error play: ${e.message}"
            isPlaying = false
        }
    }

    fun stopMedia() {
        try {
            if (mediaPlayer?.status()?.isPlaying == true) {
                mediaPlayer.controls().stop()
            }
        } catch (_: Exception) {
        }

        isPlaying = false
        currentFrame = null
    }

    DisposableEffect(url, mediaPlayer, mediaPlayerFactory) {

        if (mediaPlayer != null && mediaPlayerFactory != null) {

            /*
             * En lugar de utilizar:
             *
             *     Canvas
             *     SwingPanel
             *
             * hacemos exactamente el mismo tipo de rendering que
             * utilizas en Mac.
             *
             * VLC nos entrega los frames directamente en memoria.
             */

            val renderCallback = RenderCallback {
                    _,
                    nativeBuffers,
                    bufferFormat ->

                if (nativeBuffers.isNullOrEmpty()) {
                    return@RenderCallback
                }

                if (bufferFormat == null) {
                    return@RenderCallback
                }

                val buffer = nativeBuffers[0]

                val width = bufferFormat.width
                val height = bufferFormat.height

                if (width <= 0 || height <= 0) {
                    return@RenderCallback
                }

                try {

                    /*
                     * RV32 = 32 bits por píxel.
                     */
                    val bufferedImage = BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_ARGB
                    )

                    val pixelData =
                        (bufferedImage.raster.dataBuffer as DataBufferInt).data

                    buffer.rewind()

                    /*
                     * Copiamos los pixels del buffer de VLC
                     * al BufferedImage.
                     */
                    buffer.asIntBuffer().get(pixelData)

                    /*
                     * Convertimos el BufferedImage a ImageBitmap
                     * para poder mostrarlo directamente en Compose.
                     */
                    val bitmap =
                        bufferedImage.toComposeImageBitmap()

                    /*
                     * La actualización de la UI se hace fuera
                     * del callback de VLC.
                     */
                    SwingUtilities.invokeLater {
                        currentFrame = bitmap
                        isPlaying = true
                    }

                } catch (e: Exception) {

                    SwingUtilities.invokeLater {
                        errorMessage =
                            "Render Error: ${e.message}"
                    }
                }
            }

            val bufferFormatCallback =
                object : BufferFormatCallback {

                    override fun getBufferFormat(
                        sourceWidth: Int,
                        sourceHeight: Int
                    ): BufferFormat {

                        return RV32BufferFormat(
                            sourceWidth,
                            sourceHeight
                        )
                    }

                    override fun allocatedBuffers(
                        buffers: Array<out ByteBuffer>?
                    ) {
                        // No necesitamos hacer nada aquí.
                    }
                }

            /*
             * Configuramos VLC para que nos entregue
             * los frames mediante CallbackVideoSurface.
             */
            mediaPlayer.videoSurface().set(
                CallbackVideoSurface(
                    bufferFormatCallback,
                    renderCallback,
                    true,
                    null
                )
            )

            /*
             * Empezamos la reproducción después de configurar
             * la superficie de vídeo.
             */
            playMedia()
        }

        onDispose {

            stopMedia()

            try {
                mediaPlayer?.release()
            } catch (_: Exception) {
            }

            try {
                mediaPlayerFactory?.release()
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        onToggleFullScreen()
                    }
                )
            }
    ) {

        /*
         * VIDEO
         */
        currentFrame?.let { bitmap ->

            Image(
                bitmap = bitmap,
                contentDescription = "AceStream Video",
                modifier = Modifier.fillMaxSize()
            )

        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Cargando vídeo...",
                    color = Color.Gray,
                    fontSize = 16.sp
                )

                errorMessage?.let { error ->

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(
                            horizontal = 16.dp
                        )
                    )
                }
            }
        }

        /*
         * ERROR
         */
        errorMessage?.let { err ->

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
                    .background(
                        Color.Red.copy(alpha = 0.8f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            ) {

                Text(
                    text = err,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }

        /*
         * FULLSCREEN
         */
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .background(
                    Color.Black.copy(alpha = 0.65f),
                    RoundedCornerShape(8.dp)
                )
                .clickable {
                    onToggleFullScreen()
                }
                .padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                )
        ) {

            Text(
                text = "⛶",
                color = Color.White,
                fontSize = 22.sp
            )
        }

        /*
         * CONTROLES
         */
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .background(
                    Color.Black.copy(alpha = 0.85f),
                    RoundedCornerShape(12.dp)
                )
                .padding(
                    horizontal = 24.dp,
                    vertical = 12.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            /*
             * PLAY / STOP
             */
            Text(
                text = if (!isPlaying) "▶" else "⏹",
                color = if (!isPlaying) Color.Green else Color.Red,
                fontSize = 22.sp,
                modifier = Modifier
                    .clickable {

                        if (isPlaying) {
                            stopMedia()
                        } else {
                            playMedia()
                        }

                    }
                    .padding(4.dp)
            )

            /*
             * VOLUMEN
             */
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.width(140.dp)
            ) {

                Text(
                    text =
                        if (
                            isMuted ||
                            mediaPlayer?.audio()?.volume() == 0
                        ) {
                            "🔇"
                        } else {
                            "🔊"
                        },
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable {

                            isMuted = !isMuted

                            mediaPlayer
                                ?.audio()
                                ?.isMute = isMuted
                        }
                        .padding(4.dp)
                )

                Slider(
                    value =
                        if (isMuted) {
                            0f
                        } else {
                            (
                                    mediaPlayer
                                        ?.audio()
                                        ?.volume()
                                        ?.toFloat()
                                        ?: 100f
                                    )
                        },
                    onValueChange = { newValue ->

                        isMuted = false

                        mediaPlayer
                            ?.audio()
                            ?.isMute = false

                        mediaPlayer
                            ?.audio()
                            ?.setVolume(
                                newValue.toInt()
                            )
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
            }

            /*
             * BUFFER
             */
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text = "➖",
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable {

                            if (bufferSeconds > 1) {

                                bufferSeconds--

                                onBufferChanged(
                                    bufferSeconds
                                )

                                playMedia()
                            }
                        }
                        .padding(4.dp)
                )

                Text(
                    text = "Buf: ${bufferSeconds}s",
                    color = Color.White,
                    fontSize = 15.sp
                )

                Text(
                    text = "➕",
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable {

                            if (bufferSeconds < 30) {

                                bufferSeconds++

                                onBufferChanged(
                                    bufferSeconds
                                )

                                playMedia()
                            }
                        }
                        .padding(4.dp)
                )
            }
        }
    }
}