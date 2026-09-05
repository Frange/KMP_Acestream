package com.frange.kmpacestream

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.frange.kmpacestream.di.androidModule
import com.frange.kmpacestream.di.appModule
import com.frange.kmpacestream.presentation.main.MainViewModel
import com.frange.kmpacestream.shared.di.sharedModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (GlobalContext.getOrNull() == null) {
            org.koin.core.context.startKoin {
                androidContext(this@MainActivity)
                modules(sharedModule, appModule, androidModule)
            }
        }

        handleIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null) {
            val urlString = data.toString()
            val cleanId = if (urlString.startsWith("acestream://", ignoreCase = true)) {
                urlString.removePrefix("acestream://").removePrefix("ACESTREAM://").substringBefore("?").substringBefore("/").trim()
            } else {
                urlString.trim()
            }
            if (cleanId.isNotBlank()) {
                viewModel.playContent(cleanId)
            }
        }
    }
}