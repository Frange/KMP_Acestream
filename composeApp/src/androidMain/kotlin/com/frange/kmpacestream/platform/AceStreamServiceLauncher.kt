package com.frange.kmpacestream.platform

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

class AceStreamServiceLauncher(private val context: Context) {

    fun startEngineForContent(contentId: String) {
        try {
            // Intent explícito para despertar el ContentStartActivity o el servicio de Ace Stream
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "acestream://$contentId".toUri()
                setPackage("org.acestream.node") // Cambiar a "org.acestream.engine" si usas la otra variante
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            println("Error al lanzar el motor de Ace Stream: ${e.localizedMessage}")
        }
    }
}