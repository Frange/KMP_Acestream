package com.frange.kmpacestream.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class AceStreamAndroidController(private val context: Context) {

    companion object {
        private const val TAG = "AceStreamController"
        private const val DEFAULT_PORT = 6878 // Puerto HTTP estándar del motor de Ace Stream
        private const val SID = "kmp-client"
        private const val PRODUCT_KEY = "kmp-client"
    }

    /**
     * Despierta el motor de Ace Stream en segundo plano mediante un Foreground Service
     * y solicita la sesión de reproducción por HTTP local.
     */
    suspend fun startSession(contentId: String): Result<String> {
        val cleanId = extractContentId(contentId)
        if (cleanId.isBlank()) {
            return Result.failure(Exception("Content ID vacío"))
        }

        return withContext(Dispatchers.IO) {
            try {
                // 1. Aseguramos que el motor esté arrancado en segundo plano
                val intent = Intent().apply {
                    setClassName("org.acestream.node", "org.acestream.engine.service.AceStreamEngineService")
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("acestream://$cleanId")
                }

                try {
                    ContextCompat.startForegroundService(context, intent)
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo iniciar el servicio directamente, intentando lanzamiento alternativo", e)
                }

                // Damos un pequeño respiro para que el motor levante el servidor HTTP si estaba cerrado
                kotlinx.coroutines.delay(1500)

                // 2. Consultamos la API HTTP local del motor
                val encodedContentId = URLEncoder.encode(cleanId, "UTF-8")
                val requestUrl = "http://127.0.0.1:$DEFAULT_PORT/ace/getstream" +
                        "?sid=$SID" +
                        "&product_key=$PRODUCT_KEY" +
                        "&format=json" +
                        "&id=$encodedContentId"

                Log.d(TAG, "Start session: $requestUrl")

                val connection = URL(requestUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 30_000
                connection.readTimeout = 60_000
                connection.instanceFollowRedirects = true

                val responseCode = connection.responseCode
                val inputStream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val responseBody = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                connection.disconnect()

                Log.d(TAG, "AceStream response: $responseBody")

                if (responseCode !in 200..299) {
                    return@withContext Result.failure(
                        Exception("AceStream HTTP $responseCode: $responseBody")
                    )
                }

                val json = JSONObject(responseBody)
                val error = if (json.has("error") && !json.isNull("error")) {
                    json.optString("error")
                } else {
                    null
                }

                if (!error.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("AceStream error: $error"))
                }

                val response = json.optJSONObject("response") ?: return@withContext Result.failure(
                    Exception("AceStream no devolvió response")
                )

                val playbackUrl = response.optString("playback_url", "")
                if (playbackUrl.isBlank()) {
                    return@withContext Result.failure(Exception("AceStream no devolvió playback_url"))
                }

                Log.d(TAG, "Playback URL: $playbackUrl")
                Result.success(playbackUrl)

            } catch (e: Exception) {
                Log.e(TAG, "Error startSession", e)
                Result.failure(e)
            }
        }
    }

    fun extractContentId(value: String): String {
        val input = value.trim()
        if (input.startsWith("acestream://")) {
            return input
                .removePrefix("acestream://")
                .substringBefore("?")
                .substringBefore("/")
                .trim()
        }

        if (input.startsWith("acestream:") && input.contains("content_id=")) {
            return input
                .substringAfter("content_id=")
                .substringBefore("&")
                .trim()
        }

        return input
    }

}