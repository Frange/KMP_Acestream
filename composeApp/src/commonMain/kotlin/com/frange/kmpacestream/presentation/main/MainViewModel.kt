package com.frange.kmpacestream.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frange.kmpacestream.shared.domain.core.Result
import com.frange.kmpacestream.shared.domain.repository.AceStreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val aceStreamRepository: AceStreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun playContent(contentId: String) {
        if (contentId.isBlank()) return

        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            when (val result = aceStreamRepository.getStreamUrl(contentId.trim())) {
                is Result.Success -> {
                    _uiState.value = MainUiState.Playing(result.data.playbackUrl)
                }
                is Result.Error -> {
                    _uiState.value = MainUiState.Error(result.error)
                }
            }
        }
    }
}