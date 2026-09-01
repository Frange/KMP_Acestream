package com.frange.kmpacestream.presentation.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.frange.kmpacestream.presentation.components.VideoPlayer
import com.frange.kmpacestream.shared.domain.core.toUiText
import com.frange.kmpacestream.shared.domain.model.AceStreamStats
import com.frange.kmpacestream.shared.domain.model.StreamItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Frame
import java.io.File
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds

private val AndroidStudioDarkColorScheme = darkColorScheme(
    primary = Color(0xFF3574F0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFF93C5FD),
    surface = Color(0xFF25272B),
    onSurface = Color(0xFFFFFFFF),        // Texto blanco brillante para máxima visibilidad
    surfaceVariant = Color(0xFF1E1F22),   // Fondo general de la lista
    onSurfaceVariant = Color(0xFFA9ACB6),
    outline = Color(0xFFFFFFFF),          // Borde blanco destacado
    background = Color(0xFF141414),
    onBackground = Color(0xFFFFFFFF),
    error = Color(0xFFCF6679)
)

object StreamFavoritesManager {
    private val appDir: File
        get() {
            val userDir = System.getProperty("user.home")
            return File(userDir, ".kmpacestream").apply { mkdirs() }
        }

    private val favoritesFile: File get() = File(appDir, "favorites.json")
    private val recentsFile: File get() = File(appDir, "recents.json")

    fun loadFavorites(): List<StreamItem> {
        return loadFromFile(favoritesFile).distinctBy { it.id }
    }

    fun saveFavorites(items: List<StreamItem>) {
        saveToFile(favoritesFile, items.distinctBy { it.id })
    }

    fun loadRecents(): List<StreamItem> {
        return loadFromFile(recentsFile).distinctBy { it.id }.take(30)
    }

    fun saveRecents(items: List<StreamItem>) {
        saveToFile(recentsFile, items.distinctBy { it.id }.take(30))
    }

    private fun loadFromFile(file: File): List<StreamItem> {
        return try {
            if (file.exists()) {
                val jsonString = file.readText()
                Json.decodeFromString(ListSerializer(StreamItem.serializer()), jsonString)
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToFile(file: File, items: List<StreamItem>) {
        try {
            val jsonString = Json.encodeToString(ListSerializer(StreamItem.serializer()), items)
            file.writeText(jsonString)
        } catch (_: Exception) { }
    }
}

private fun getInitialStreamList(): List<StreamItem> {
    val saved = StreamFavoritesManager.loadFavorites()
    return if (saved.isNotEmpty()) {
        saved
    } else {
        listOf(
            StreamItem("118416583faa3fb4f03800173429b6b844b43a52", "Test funcional nisu", isFavorite = true),
            StreamItem("071b49a1c52d304cae6300e2776022eec7ebe6cd", "Canal de Prueba 2")
        )
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel(),
    windowState: androidx.compose.ui.window.WindowState = rememberWindowState()
) {
    MaterialTheme(colorScheme = AndroidStudioDarkColorScheme) {
        val uiState by viewModel.uiState.collectAsState()
        var contentIdInput by remember { mutableStateOf("") }
        var selectedTab by remember { mutableIntStateOf(0) }
        var showStatsOverlay by remember { mutableStateOf(true) }
        var searchQuery by remember { mutableStateOf("") }

        // Sincronizamos directamente con el estado de la ventana de Compose
        val isFullscreen = windowState.placement == WindowPlacement.Fullscreen

        var savedBufferSeconds by remember { mutableIntStateOf(3) }

        var jsonStreams by remember { mutableStateOf(getInitialStreamList()) }
        var recentStreams by remember { mutableStateOf(StreamFavoritesManager.loadRecents()) }
        var liveStats by remember { mutableStateOf(AceStreamStats()) }

        var channelToEdit by remember { mutableStateOf<StreamItem?>(null) }
        var editedNameInput by remember { mutableStateOf("") }

        fun cleanContentId(rawId: String): String {
            val trimmed = rawId.trim()
            return if (trimmed.startsWith("acestream://", ignoreCase = true)) {
                trimmed.removePrefix("acestream://").removePrefix("ACESTREAM://")
            } else {
                trimmed
            }
        }

        fun toggleWindowFullScreen() {
            windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) {
                WindowPlacement.Floating
            } else {
                WindowPlacement.Fullscreen
            }
        }

        fun handlePlayContent(id: String, name: String? = null) {
            val cleanId = cleanContentId(id)
            if (cleanId.isBlank()) return

            viewModel.playContent(cleanId)

            val existing = jsonStreams.find { it.id == cleanId } ?: recentStreams.find { it.id == cleanId }
            val streamName = name ?: existing?.name ?: cleanId

            val isFav = existing?.isFavorite ?: false
            val newItem = StreamItem(id = cleanId, name = streamName, isFavorite = isFav)

            recentStreams = listOf(newItem) + recentStreams.filter { it.id != cleanId }
            if (recentStreams.size > 30) {
                recentStreams = recentStreams.take(30)
            }
            StreamFavoritesManager.saveRecents(recentStreams)

            if (isFav && jsonStreams.none { it.id == cleanId }) {
                jsonStreams = (listOf(newItem) + jsonStreams).distinctBy { it.id }
                StreamFavoritesManager.saveFavorites(jsonStreams)
            }
        }

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
                            val jsonStr = connection.getInputStream().bufferedReader().use { it.readText() }

                            fun extractInt(key: String): Int {
                                val regex = "\"$key\":\\s*(-?\\d+)".toRegex()
                                return regex.find(jsonStr)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                            }

                            val dl = extractInt("speed_down")
                            val ul = extractInt("speed_up")
                            val peers = extractInt("peers")
                            val progress = extractInt("progress")
                            val bufferPieces = extractInt("buffer_pieces")
                            val calculatedBuffer = if (progress > 0) progress else if (bufferPieces > 0) (bufferPieces * 100) / 15 else 0
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
                    } catch (_: Exception) { }
                    delay(1000.milliseconds)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!isFullscreen) {
                Surface(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Bloque superior destacado a ancho completo hasta los tabs
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                            border = BorderStroke(0.3.dp, Color.White.copy(alpha = 0.2f)),
                            tonalElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Reproducir Stream",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = contentIdInput,
                                        onValueChange = { contentIdInput = it },
                                        label = { Text("Content ID", color = Color.White, fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.White,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                            focusedLabelColor = Color.White,
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                                    )

                                    Button(
                                        onClick = { handlePlayContent(contentIdInput) },
                                        enabled = contentIdInput.isNotBlank(),
                                        modifier = Modifier.defaultMinSize(minWidth = 70.dp)
                                    ) {
                                        Text("Ver")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Favoritos") })
                                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Recientes") })
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Filtrar canales...", fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.2f)
                                ),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val rawList = when (selectedTab) {
                                0 -> jsonStreams.filter { it.isFavorite }
                                else -> recentStreams
                            }

                            val currentList = if (searchQuery.isBlank()) {
                                rawList
                            } else {
                                rawList.filter {
                                    it.name.contains(searchQuery, ignoreCase = true) ||
                                            it.id.contains(searchQuery, ignoreCase = true)
                                }
                            }

                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(currentList, key = { it.id }) { item ->
                                    val isPlayingThis = (uiState is MainUiState.Playing) && (contentIdInput == item.id)
                                    val cleanIdDisplay = cleanContentId(item.id)

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                contentIdInput = item.id
                                                handlePlayContent(item.id, item.name)
                                            },
                                        color = if (isPlayingThis) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = if (item.isFavorite) "★" else "☆",
                                                modifier = Modifier.clickable {
                                                    val newFavState = !item.isFavorite

                                                    jsonStreams = jsonStreams.map {
                                                        if (it.id == item.id) it.copy(isFavorite = newFavState) else it
                                                    }.distinctBy { s -> s.id }

                                                    if (newFavState && jsonStreams.none { s -> s.id == item.id }) {
                                                        jsonStreams = jsonStreams + item.copy(isFavorite = true)
                                                    }
                                                    StreamFavoritesManager.saveFavorites(jsonStreams)

                                                    recentStreams = recentStreams.map {
                                                        if (it.id == item.id) {
                                                            val updated = it.copy(isFavorite = newFavState)
                                                            if (newFavState && jsonStreams.none { js -> js.id == it.id }) {
                                                                jsonStreams = (listOf(updated) + jsonStreams).distinctBy { s -> s.id }
                                                                StreamFavoritesManager.saveFavorites(jsonStreams)
                                                            }
                                                            updated
                                                        } else {
                                                            it
                                                        }
                                                    }
                                                    StreamFavoritesManager.saveRecents(recentStreams)
                                                },
                                                fontSize = 18.sp,
                                                color = if (item.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = item.name,
                                                    maxLines = 1,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = cleanIdDisplay,
                                                    maxLines = 1,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "✏️",
                                                    modifier = Modifier
                                                        .clickable {
                                                            channelToEdit = item
                                                            editedNameInput = item.name
                                                        }
                                                        .padding(4.dp),
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "🗑️",
                                                    modifier = Modifier
                                                        .clickable {
                                                            if (selectedTab == 0) {
                                                                jsonStreams = jsonStreams.filter { it.id != item.id }
                                                                StreamFavoritesManager.saveFavorites(jsonStreams)
                                                            } else {
                                                                recentStreams = recentStreams.filter { it.id != item.id }
                                                                StreamFavoritesManager.saveRecents(recentStreams)
                                                            }
                                                        }
                                                        .padding(4.dp),
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { showStatsOverlay = !showStatsOverlay },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (showStatsOverlay) "Ocultar Stats" else "Mostrar Stats", fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    when (val state = uiState) {
                        is MainUiState.Idle -> {
                            Text(
                                text = "Introduce un Content ID o selecciona un canal para reproducir.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is MainUiState.Loading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("Cargando Stream AceStream...", color = MaterialTheme.colorScheme.onSurface)
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
                        color = Color(0xFF141414).copy(alpha = 0.95f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (liveStats.isLive) "● EN DIRECTO" else "○ DIFERIDO",
                                color = if (liveStats.isLive) Color(0xFFF87171) else Color.Gray,
                                fontSize = 12.sp
                            )
                            Text("Bajada: ${liveStats.downloadSpeedKbps} KB/s", color = Color(0xFFBCBEC4), fontSize = 12.sp)
                            Text("Subida: ${liveStats.uploadSpeedKbps} KB/s", color = Color(0xFFBCBEC4), fontSize = 12.sp)
                            Text(
                                text = "Buffer: ${liveStats.bufferPercentage}% (~${savedBufferSeconds}s)",
                                color = Color(0xFFBCBEC4),
                                fontSize = 12.sp
                            )
                            Text("Peers: ${liveStats.peersCount}", color = Color(0xFFBCBEC4), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

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
                        StreamFavoritesManager.saveFavorites(jsonStreams)

                        recentStreams = recentStreams.map {
                            if (it.id == targetId) it.copy(name = editedNameInput) else it
                        }
                        StreamFavoritesManager.saveRecents(recentStreams)

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
}
