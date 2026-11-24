package com.marbellup.fitin.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Dictado con el reconocimiento de voz del sistema (gratis, sin librerías).
 * Muestra el texto parcial mientras se habla, para que la persona vea que se la entiende.
 * Requiere el permiso RECORD_AUDIO (se pide en la pantalla que lo usa).
 */
class Dictado(private val context: Context) {
    var escuchando by mutableStateOf(false)
        private set
    var parcial by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val disponible: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    private var reconocedor: SpeechRecognizer? = null

    /** Empieza a escuchar. [alTerminar] recibe el texto final (puede estar vacío). */
    fun empezar(alTerminar: (String) -> Unit) {
        if (!disponible || escuchando) return
        error = null
        parcial = ""
        val r = reconocedor ?: SpeechRecognizer.createSpeechRecognizer(context).also { reconocedor = it }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { escuchando = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                primero(partialResults)?.let { parcial = it }
            }

            override fun onResults(results: Bundle?) {
                escuchando = false
                val texto = primero(results) ?: parcial
                parcial = texto
                alTerminar(texto)
            }

            override fun onError(code: Int) {
                escuchando = false
                error = when (code) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No te he oído bien. ¿Lo intentamos otra vez?"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Hace falta permiso para usar el micrófono."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sin conexión para el dictado. Puedes escribirlo."
                    else -> "El dictado no ha funcionado. Puedes escribirlo."
                }
                if (parcial.isNotBlank()) alTerminar(parcial)
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Si el móvil tiene el reconocimiento de español sin conexión, que lo use (la voz no sale del móvil).
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            // Deja hablar con calma: no corta a la primera pausa.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        }
        escuchando = true
        r.startListening(intent)
    }

    fun parar() {
        reconocedor?.stopListening()
    }

    fun liberar() {
        reconocedor?.destroy()
        reconocedor = null
        escuchando = false
    }

    private fun primero(b: Bundle?): String? =
        b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }
}

@Composable
fun rememberDictado(): Dictado {
    val context = LocalContext.current
    val dictado = remember(context) { Dictado(context) }
    DisposableEffect(dictado) { onDispose { dictado.liberar() } }
    return dictado
}
