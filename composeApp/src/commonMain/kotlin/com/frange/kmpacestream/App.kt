package com.frange.kmpacestream

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.frange.kmpacestream.presentation.main.MainScreen
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        MaterialTheme {
            MainScreen()
        }
    }
}