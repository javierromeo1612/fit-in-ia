package com.marbellup.fitin.ai

import com.marbellup.fitin.ai.propio.CuentanosloPropio
import com.marbellup.fitin.data.Need
import com.marbellup.fitin.data.Slot
import com.marbellup.fitin.data.Sport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * «Cuéntanoslo»: la persona describe con sus palabras (escribiendo o con la voz) cómo es,
 * o cómo es la persona a la que cuida, y la IA lo traduce al catálogo de necesidades.
 *
 * Lo hace el MODELO PROPIO del equipo ([CuentanosloPropio], traído de la versión de Talent Land):
 * funciona dentro del móvil, sin internet y sin enviar lo que la persona cuenta (son datos de salud).
 * La IA solo PROPONE: la persona revisa cada sugerencia antes de guardarla.
 */
object PerfilIA {

    enum class Franja(val slot: Slot?) { Manana(Slot.Manana), Tarde(Slot.Tarde), Noche(Slot.Noche), Indiferente(null) }

    data class Sugerencia(val need: Need, val porque: String)

    /** Pregunta de aclaración con respuestas de un toque. Cada opción añade necesidades. */
    data class Pregunta(val texto: String, val opciones: List<Opcion>)
    data class Opcion(val texto: String, val necesidades: List<Need>)

    data class Perfil(
        val nombre: String,
        val gestionaFamiliar: Boolean,
        val sugerencias: List<Sugerencia>,
        val deportes: List<Sport>,
        val franja: Franja,
        val preguntas: List<Pregunta>,
        val resumen: String,
    )

    /** Frase de la demo (historia de Lucía). */
    const val FRASE_DEMO =
        "Es para mi hija Lucía, tiene diez años y va en silla. Le encanta el baloncesto, pero se cansa rápido. " +
            "Con mucho ruido se agobia, y por las tardes está sola."

    /** Modelo propio, en el móvil. Tarda milisegundos y no gasta datos. */
    suspend fun interpretar(texto: String): Perfil = withContext(Dispatchers.Default) {
        CuentanosloPropio.interpretar(texto)
    }
}
