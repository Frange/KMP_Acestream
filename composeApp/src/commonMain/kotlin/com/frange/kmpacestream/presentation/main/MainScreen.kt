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
import com.frange.kmpacestream.shared.domain.model.UserSettings
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var contentIdInput by remember { mutableStateOf("") }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showStatsOverlay by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var settings by remember { mutableStateOf(UserSettings()) }

    var channelToEdit by remember { mutableStateOf<StreamItem?>(null) }
    var editedNameInput by remember { mutableStateOf("") }

    var jsonStreams by remember {
        mutableStateOf(
            listOf(
                StreamItem("118416583faa3fb4f03800173429b6b844b43a52", "Test funcional nisu", isFavorite = true),
                StreamItem("071b49a1c52d304cae6300e2776022eec7ebe6cd", "Canal de Prueba 2")
            )
        )
    }

    var recentStreams by remember { mutableStateOf(listOf<StreamItem>()) }

    // Estado dinámico de estadísticas
    var liveStats by remember { mutableStateOf(AceStreamStats()) }

    // Polling de estadísticas en tiempo real cuando está reproduciendo
    LaunchedEffect(uiState) {
        val playingState = uiState as? MainUiState.Playing
        if (playingState != null) {
            while (true) {
                // Si el viewModel ofrece función de refresco, se invoca aquí:
                // liveStats = viewModel.fetchStats(playingState.statUrl)
                delay(1000)
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
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
                    onClick = { viewModel.playContent(contentIdInput) },
                    enabled = contentIdInput.isNotBlank(),
                    modifier = Modifier.defaultMinSize(minWidth = 70.dp)
                ) {
                    Text("Ver")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Lista") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Favoritos") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Recientes") })
            }

            val currentList = when (selectedTab) {
                0 -> jsonStreams
                1 -> jsonStreams.filter { it.isFavorite }
                else -> recentStreams
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(currentList, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.name, maxLines = 1) },
                        supportingContent = { Text(item.id.take(16) + "...", fontSize = 11.sp) },
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
                            Row {
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
                                Text(
                                    text = "🗑️",
                                    modifier = Modifier
                                        .clickable {
                                            jsonStreams = jsonStreams.filter { it.id != item.id }
                                            recentStreams = recentStreams.filter { it.id != item.id }
                                        }
                                        .padding(4.dp),
                                    fontSize = 14.sp
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            contentIdInput = item.id
                            if (settings.autoPlayOnSelect) {
                                viewModel.playContent(item.id)
                            }
                            if (!recentStreams.any { it.id == item.id }) {
                                recentStreams = listOf(item) + recentStreams
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showStatsOverlay = !showStatsOverlay },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (showStatsOverlay) "Ocultar Stats" else "Mostrar Stats", fontSize = 12.sp)
                }

                Button(onClick = { showSettingsDialog = true }) {
                    Text("⚙️", fontSize = 14.sp)
                }
            }
        }

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
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (showStatsOverlay && uiState is MainUiState.Playing) {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
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
                        Text("Bajada: ${liveStats.downloadSpeedKbps} KB/s", color = Color.White, fontSize = 12.sp)
                        Text("Subida: ${liveStats.uploadSpeedKbps} KB/s", color = Color.White, fontSize = 12.sp)
                        Text(
                            text = "Buffer: ${liveStats.bufferPercentage}% (~${settings.bufferTimeSeconds}s)",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text("Peers: ${liveStats.peersCount}", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    channelToEdit?.let { item ->
        AlertDialog(
            onDismissRequest = { channelToEdit = null },
            title = { Text("Editar Canal") },
            text = {
                OutlinedTextField(
                    value = editedNameInput,
                    onValueChange = { editedNameInput = it },
                    label = { Text("Nombre del canal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedNameInput.isNotBlank()) {
                            jsonStreams = jsonStreams.map {
                                if (it.id == item.id) it.copy(name = editedNameInput) else it
                            }
                            recentStreams = recentStreams.map {
                                if (it.id == item.id) it.copy(name = editedNameInput) else it
                            }
                        }
                        channelToEdit = null
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { channelToEdit = null }) { Text("Cancelar") }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Ajustes del Reproductor y Engine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            text = "Tiempo de Buffer del Reproductor: ${settings.bufferTimeSeconds} segundos",
                            fontSize = 14.sp
                        )
                        Slider(
                            value = settings.bufferTimeSeconds.toFloat(),
                            onValueChange = { settings = settings.copy(bufferTimeSeconds = it.toInt()) },
                            valueRange = 2f..30f,
                            steps = 27
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reproducir al seleccionar canal")
                        Switch(
                            checked = settings.autoPlayOnSelect,
                            onCheckedChange = { settings = settings.copy(autoPlayOnSelect = it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Aceleración de Hardware (VLC)")
                        Switch(
                            checked = settings.hardwareAcceleration,
                            onCheckedChange = { settings = settings.copy(hardwareAcceleration = it) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) {
                    Text("Aplicar y Cerrar")
                }
            }
        )
    }
}