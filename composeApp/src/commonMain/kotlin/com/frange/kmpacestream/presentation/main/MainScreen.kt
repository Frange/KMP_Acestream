package com.frange.kmpacestream.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frange.kmpacestream.presentation.components.VideoPlayer
import com.frange.kmpacestream.shared.domain.core.toUiText
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var contentIdInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Barra superior de entrada para el Content ID
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = contentIdInput,
                onValueChange = { contentIdInput = it },
                label = { Text("AceStream Content ID / Hash") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { viewModel.playContent(contentIdInput) },
                enabled = uiState !is MainUiState.Loading && contentIdInput.isNotBlank()
            ) {
                Text("Reproducir")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Área de estado y vídeo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is MainUiState.Idle -> {
                    Text("Introduce un Content ID para iniciar la reproducción.")
                }
                is MainUiState.Loading -> {
                    CircularProgressIndicator()
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
    }
}