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
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import com.sun.jna.Platform

fun registerWindowsProtocol() {
    val os = System.getProperty("os.name").lowercase()
    if (Platform.isMac()) {
        System.setProperty("jna.library.path", "/Applications/VLC.app/Contents/MacOS/lib")
    }
    if (!os.contains("win")) return

    try {
        val currentJarOrExe = File(
            object {}::class.java.protectionDomain.codeSource.location.toURI()
        ).absolutePath

        println("LOG-REG: Ruta detectada para el registro: $currentJarOrExe")

        val command = if (currentJarOrExe.endsWith(".exe")) {
            "\"$currentJarOrExe\" \"%1\""
        } else {
            // Si se ejecuta como script/jar, aseguramos que reciba el parámetro %1
            "javaw -jar \"$currentJarOrExe\" \"%1\""
        }

        println("LOG-REG: Comando de registro asociado: $command")

        val commands = listOf(
            listOf("reg", "add", "HKCU\\Software\\Classes\\acestream", "/ve", "/d", "URL:AceStream Protocol", "/f"),
            listOf("reg", "add", "HKCU\\Software\\Classes\\acestream", "/v", "URL Protocol", "/f"),
            listOf("reg", "add", "HKCU\\Software\\Classes\\acestream\\shell\\open\\command", "/ve", "/d", command, "/f")
        )

        for (cmd in commands) {
            ProcessBuilder(cmd).start().waitFor()
        }
        println("LOG-REG: Protocolo acestream:// registrado correctamente en el Registro de Windows (HKCU).")
    } catch (e: Exception) {
        println("LOG-REG-ERROR: Error al registrar el protocolo en Windows: ${e.message}")
    }
}

fun startAceStreamEngine() {
    val os = System.getProperty("os.name").lowercase()

    when {
        os.contains("win") -> {
            try {
                val engineDir = File(System.getProperty("java.io.tmpdir"), "kmp-acestream-engine")
                if (!engineDir.exists()) engineDir.mkdirs()

                val winExecutable = File(engineDir, "ace_engine.exe")

                if (!winExecutable.exists()) {
                    object {}::class.java.getResourceAsStream("/engine_win/ace_engine.exe")?.use { input ->
                        Files.copy(input, winExecutable.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }

                if (winExecutable.exists()) {
                    ProcessBuilder(winExecutable.absolutePath, "--g2g-port", "6878")
                        .directory(engineDir)
                        .start()
                    println("LOG-ENGINE: Ace Stream Engine iniciado en Windows desde la ruta temporal.")
                } else {
                    println("LOG-ENGINE: Error: No se pudo extraer el ejecutable de Ace Stream desde los recursos.")
                }
            } catch (e: Exception) {
                println("LOG-ENGINE-ERROR: Error en Windows: ${e.message}")
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
                    println("LOG-ENGINE: Creando nuevo contenedor Docker kmp-acestream...")
                    ProcessBuilder(
                        "/bin/sh", "-c",
                        "docker run -d --name kmp-acestream -p 6878:6878 local-acestream"
                    ).start()
                } else {
                    println("LOG-ENGINE: Contenedor kmp-acestream reanudado correctamente.")
                }

                Thread.sleep(2000)

            } catch (e: Exception) {
                println("LOG-ENGINE-ERROR: Error iniciando Docker en macOS: ${e.message}")
            }
        }
    }
}

fun main(args: Array<String>) {
    System.setProperty("compose.interop.blending", "true")
    System.setProperty("apple.awt.graphics.EnableQ2DX", "true")

    println("=== LOG DE INICIO DE APLICACIÓN ===")
    println("Argumentos de inicio recibidos (args): ${args.contentToString()}")

    registerWindowsProtocol()
    startAceStreamEngine()

    // Variable para almacenar el enlace detectado
    var pendingUri: String? = args.firstOrNull { it.startsWith("acestream://", ignoreCase = true) }
    if (pendingUri != null) {
        println("LOG-URI: ¡Enlace capturado por argumentos de inicio! -> $pendingUri")
    }

    // Registrar manejador de AWT para app abierta en caliente (ej: macOS o segunda llamada en Windows)
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_URI)) {
            Desktop.getDesktop().setOpenURIHandler { event ->
                val uriString = event.uri?.toString()
                if (!uriString.isNullOrBlank()) {
                    println("LOG-URI: ¡URI recibida en caliente por AWT OpenURIHandler! -> $uriString")
                    pendingUri = uriString
                }
            }
            println("LOG-AWT: APP_OPEN_URI registrado correctamente.")
        } else {
            println("LOG-AWT: APP_OPEN_URI no soportado en este entorno.")
        }
    } catch (e: Exception) {
        println("LOG-AWT-ERROR: No se pudo registrar el manejador de URIs de AWT: ${e.message}")
    }

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