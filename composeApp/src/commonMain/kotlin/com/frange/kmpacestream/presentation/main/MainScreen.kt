package com.frange.kmpacestream.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frange.kmpacestream.presentation.components.VideoPlayer
import com.frange.kmpacestream.shared.domain.core.toUiText
import com.frange.kmpacestream.shared.domain.model.AceStreamStats
import com.frange.kmpacestream.shared.domain.model.StreamItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Frame
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var contentIdInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Favoritos, 1: Recientes
    var showStatsOverlay by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }

    var savedBufferSeconds by remember { mutableIntStateOf(3) }

    var jsonStreams by remember {
        mutableStateOf(
            listOf(
                StreamItem(
                    "118416583faa3fb4f03800173429b6b844b43a52",
                    "Test funcional nisu",
                    isFavorite = true
                ),
                StreamItem("071b49a1c52d304cae6300e2776022eec7ebe6cd", "Canal de Prueba 2")
            )
        )
    }

    var recentStreams by remember { mutableStateOf(listOf<StreamItem>()) }
    var liveStats by remember { mutableStateOf(AceStreamStats()) }

    var channelToEdit by remember { mutableStateOf<StreamItem?>(null) }
    var editedNameInput by remember { mutableStateOf("") }

    fun toggleWindowFullScreen() {
        isFullscreen = !isFullscreen
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

    fun handlePlayContent(id: String, name: String? = null) {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return

        viewModel.playContent(cleanId)

        val existing =
            jsonStreams.find { it.id == cleanId } ?: recentStreams.find { it.id == cleanId }
        val streamName = name ?: existing?.name ?: cleanId

        val newItem =
            StreamItem(id = cleanId, name = streamName, isFavorite = existing?.isFavorite ?: false)

        if (!recentStreams.any { it.id == cleanId }) {
            recentStreams = listOf(newItem) + recentStreams
        }
    }

    // Polling de estadísticas de AceStream
    LaunchedEffect(uiState) {
        val playingState = uiState as? MainUiState.Playing
        if (playingState != null && playingState.streamUrl.isNotBlank()) {
            val statUrl = playingState.streamUrl.replace("/ace/r/", "/ace/stat/")

            while (true) {
                try {
                    withContext(Dispatchers.IO) {
                        val connection = URL(statUrl).openConnection()
                        connection.connectTimeout = 1000
                        connection.readTimeout = 1000
                        val jsonStr =
                            connection.getInputStream().bufferedReader().use { it.readText() }

                        fun extractInt(key: String): Int {
                            val regex = "\"$key\":\\s*(-?\\d+)".toRegex()
                            return regex.find(jsonStr)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        }

                        val dl = extractInt("speed_down")
                        val ul = extractInt("speed_up")
                        val peers = extractInt("peers")
                        val progress = extractInt("progress")
                        val bufferPieces = extractInt("buffer_pieces")
                        val calculatedBuffer =
                            if (progress > 0) progress else if (bufferPieces > 0) (bufferPieces * 100) / 15 else 0
                        val isLive = extractInt("is_live") != 0

                        withContext(Dispatchers.Main) {
                            liveStats = AceStreamStats(
                                downloadSpeedKbps = dl,
                                uploadSpeedKbps = ul,
                                peersCount = peers,
                                bufferPercentage = calculatedBuffer.coerceIn(0, 100),
                                isLive = isLive
                            )
                        }
                    }
                } catch (_: Exception) {
                }
                delay(1000.milliseconds)
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Panel lateral (Se oculta en Fullscreen)
        if (!isFullscreen) {
            Column(
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = contentIdInput,
                        onValueChange = { contentIdInput = it },
                        label = { Text("Content ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = { handlePlayContent(contentIdInput) },
                        enabled = contentIdInput.isNotBlank(),
                        modifier = Modifier.defaultMinSize(minWidth = 70.dp)
                    ) {
                        Text("Ver")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pestañas simplificadas: Solo Favoritos y Recientes
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Favoritos") })
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Recientes") })
                }

                val currentList = when (selectedTab) {
                    0 -> jsonStreams.filter { it.isFavorite }
                    else -> recentStreams
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(currentList, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.name, maxLines = 1) },
                            supportingContent = {
                                Text(
                                    item.id.take(16) + "...",
                                    fontSize = 11.sp
                                )
                            },
                            leadingContent = {
                                Text(
                                    text = if (item.isFavorite) "★" else "☆",
                                    modifier = Modifier.clickable {
                                        jsonStreams = jsonStreams.map {
                                            if (it.id == item.id) it.copy(isFavorite = !it.isFavorite) else it
                                        }
                                    },
                                    fontSize = 20.sp,
                                    color = if (item.isFavorite) Color(0xFFFFD700) else Color.Gray
                                )
                            },
                            trailingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Botón Lápiz para Editar
                                    Text(
                                        text = "✏️",
                                        modifier = Modifier
                                            .clickable {
                                                channelToEdit = item
                                                editedNameInput = item.name
                                            }
                                            .padding(4.dp),
                                        fontSize = 14.sp
                                    )
                                    // Botón Papelera para Borrar
                                    Text(
                                        text = "🗑️",
                                        modifier = Modifier
                                            .clickable {
                                                jsonStreams =
                                                    jsonStreams.filter { it.id != item.id }
                                                recentStreams =
                                                    recentStreams.filter { it.id != item.id }
                                            }
                                            .padding(4.dp),
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                contentIdInput = item.id
                                handlePlayContent(item.id, item.name)
                            }
                        )
                        HorizontalDivider()
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showStatsOverlay = !showStatsOverlay },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (showStatsOverlay) "Ocultar Stats" else "Mostrar Stats",
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Reproductor y Estadísticas
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (val state = uiState) {
                    is MainUiState.Idle -> {
                        Text("Introduce un Content ID o selecciona un canal para reproducir.")
                    }

                    is MainUiState.Loading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Cargando Stream AceStream...")
                        }
                    }

                    is MainUiState.Error -> {
                        Text(
                            text = state.error.toUiText(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    is MainUiState.Playing -> {
                        VideoPlayer(
                            url = state.streamUrl,
                            initialBufferSeconds = savedBufferSeconds,
                            onBufferChanged = { savedBufferSeconds = it },
                            onToggleFullScreen = { toggleWindowFullScreen() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (showStatsOverlay && uiState is MainUiState.Playing) {
                Surface(
                    color = Color.Black.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (liveStats.isLive) "● EN DIRECTO" else "○ DIFERIDO",
                            color = if (liveStats.isLive) Color.Red else Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            "Bajada: ${liveStats.downloadSpeedKbps} KB/s",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            "Subida: ${liveStats.uploadSpeedKbps} KB/s",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Buffer: ${liveStats.bufferPercentage}% (~${savedBufferSeconds}s)",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            "Peers: ${liveStats.peersCount}",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Diálogo emergente para editar el nombre de un canal con el lápiz
    if (channelToEdit != null) {
        AlertDialog(
            onDismissRequest = { channelToEdit = null },
            title = { Text("Editar nombre del canal") },
            text = {
                OutlinedTextField(
                    value = editedNameInput,
                    onValueChange = { editedNameInput = it },
                    singleLine = true,
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val targetId = channelToEdit!!.id
                    jsonStreams = jsonStreams.map {
                        if (it.id == targetId) it.copy(name = editedNameInput) else it
                    }
                    recentStreams = recentStreams.map {
                        if (it.id == targetId) it.copy(name = editedNameInput) else it
                    }
                    channelToEdit = null
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}