package com.frange.kmpacestream

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.frange.kmpacestream.di.appModule
import com.frange.kmpacestream.presentation.main.MainScreen
import com.frange.kmpacestream.shared.di.sharedModule
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.awt.Dimension
import java.io.File

fun startAceStreamEngine() {
    val os = System.getProperty("os.name").lowercase()

    when {
        os.contains("win") -> {
            val engineDir = File("resources/engine_win")
            val winExecutable = File(engineDir, "ace_engine.exe")

            if (winExecutable.exists()) {
                try {
                    ProcessBuilder(winExecutable.absolutePath, "--g2g-port", "6878")
                        .directory(engineDir)
                        .start()
                    println("Ace Stream Engine iniciado en Windows.")
                } catch (e: Exception) {
                    println("Error en Windows: ${e.message}")
                }
            }
        }

        os.contains("mac") || os.contains("darwin") -> {
            try {
                val dockerPath = listOf(
                    "/usr/local/bin/docker",
                    "/opt/homebrew/bin/docker",
                    "/usr/bin/docker",
                    "docker"
                ).firstOrNull { File(it).exists() } ?: "docker"

                val startProcess = ProcessBuilder(dockerPath, "start", "kmp-acestream").start()
                val exitCode = startProcess.waitFor()

                if (exitCode != 0) {
                    println("Creando nuevo contenedor Docker kmp-acestream...")
                    ProcessBuilder(
                        "/bin/sh", "-c",
                        "docker run -d --name kmp-acestream -p 6878:6878 local-acestream"
                    ).start()
                } else {
                    println("Contenedor kmp-acestream reanudado correctamente.")
                }

                Thread.sleep(2000)

            } catch (e: Exception) {
                println("Error iniciando Docker en macOS: ${e.message}")
            }
        }
    }
}

fun main() {
    System.setProperty("compose.interop.blending", "true")
    System.setProperty("apple.awt.graphics.EnableQ2DX", "true")

    startAceStreamEngine()

    application {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(sharedModule, appModule)
            }
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "KMP Acestream",
            state = rememberWindowState(
                width = 1100.dp,
                height = 700.dp
            )
        ) {
            window.minimumSize = Dimension(950, 600)
            MainScreen()
        }
    }
}