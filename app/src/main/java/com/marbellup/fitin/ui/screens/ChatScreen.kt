package com.marbellup.fitin.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

/** Dirección «https» interna con la que la app sirve sus propios archivos (assets) al WebView. */
private const val CHAT_URL = "https://appassets.androidplatform.net/assets/chat/index.html"

/**
 * Pestaña de chat: Cuac, el asistente de Fit-In hecho con Botpress, a pantalla completa.
 * El webchat (assets/chat/inject.js + config.js) va dentro de la app; necesita Internet para hablar con el bot.
 */
@Composable
fun ChatScreen(bottomBar: @Composable () -> Unit) {
    Scaffold(containerColor = Color.White, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = bottomBar) { padding ->
        BotpressChat(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .imePadding(),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BotpressChat(modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val loader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true // guarda la conversación entre visitas
                settings.textZoom = 118 // letra algo más grande que la de Botpress por defecto
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                        loader.shouldInterceptRequest(request.url)

                    // Los enlaces que envíe el bot se abren en el navegador, no dentro del chat.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (request.url.host == "appassets.androidplatform.net") return false
                        return try {
                            view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
                loadUrl(CHAT_URL)
            }
        },
    )
}
