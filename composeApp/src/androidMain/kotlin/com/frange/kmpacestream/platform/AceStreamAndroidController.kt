package com.frange.kmpacestream.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri

class AceStreamAndroidController(private val context: Context) {

    fun isAceStreamInstalled(): Boolean {
        return try {
            val packageManager = context.packageManager
            packageManager.getPackageInfo("org.acestream.engine", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun launchEngineAndPlay(contentId: String) {
        if (!isAceStreamInstalled()) {
            Toast.makeText(context, "Ace Stream Engine no está instalado", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.acestream.engine"))
                context.startActivity(intent)
            } catch (_: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=org.acestream.engine"))
                context.startActivity(webIntent)
            }
            return
        }

        try {
            // Intent explícito para forzar el inicio del servicio/motor en Android
            val serviceIntent = Intent().apply {
                setClassName("org.acestream.engine", "org.acestream.engine.service.AceStreamEngineService")
            }
            context.startForegroundService(serviceIntent)
        } catch (_: Exception) {
            // Fallback si la versión de la app de Android no permite inicio en segundo plano directo
        }

        // Lanzar reproductor o pasarela mediante Intent de AceStream
        try {
            val playIntent = Intent(Intent.ACTION_VIEW).apply {
                data = "acestream://$contentId".toUri()
                setPackage("org.acestream.engine")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al iniciar stream: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}