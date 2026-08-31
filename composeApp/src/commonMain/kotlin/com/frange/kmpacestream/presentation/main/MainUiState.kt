package com.frange.kmpacestream.presentation.main

import com.frange.kmpacestream.shared.domain.core.NetworkError

sealed interface MainUiState {
    data object Idle : MainUiState
    data object Loading : MainUiState
    data class Playing(val streamUrl: String) : MainUiState
    data class Error(val error: NetworkError) : MainUiState
}