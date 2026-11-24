package com.marbellup.fitin.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.marbellup.fitin.ui.FloatingMascot
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.theme.LocalExtra

/** Dirección «https» interna con la que la app sirve sus propios archivos (assets) al WebView. */
private const val CHAT_URL = "https://appassets.androidplatform.net/assets/chat/index.html"

/**
 * Pestaña de chat: Cuac, el asistente de Fit-In hecho con Botpress.
 * El webchat (assets/chat/inject.js + config.js) va dentro de la app; necesita Internet para hablar con el bot.
 */
@Composable
fun ChatScreen(bottomBar: @Composable () -> Unit) {
    val x = LocalExtra.current
    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = bottomBar) { padding ->
        GradientBackground(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FloatingMascot(52.dp)
                    Column(Modifier.padding(start = 10.dp)) {
                        Text("Cuac", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                        Text("Tu asistente de Fit-In", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val shape = RoundedCornerShape(24.dp)
                BotpressChat(
                    Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(10.dp, shape, ambientColor = x.hero.copy(alpha = 0.35f), spotColor = x.hero.copy(alpha = 0.45f))
                        .clip(shape)
                        .background(Color.White),
                )
            }
        }
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
