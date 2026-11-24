package com.marbellup.fitin.data

/*
 * Tipos auxiliares de la IA del móvil: lo que el procesamiento de lenguaje local entiende
 * de un texto, los avisos y los mensajes del asistente.
 */

/** Lo que la IA ha entendido del texto libre. */
data class ParsedNeeds(
    val sport: Sport?,
    val freeSport: String?,
    val level: String?,
    /** hoy | manana | esta_semana | fin_de_semana | cualquiera */
    val day: String,
    /** manana | mediodia | tarde | noche | cualquiera */
    val slot: String,
    val needs: List<Need>,
    val company: String,
    val summary: String,
    val clarification: String?,
) {
    val whenLabel: String
        get() {
            val d = when (day) {
                "hoy" -> "Hoy"; "manana" -> "Mañana"; "esta_semana" -> "Esta semana"
                "fin_de_semana" -> "Fin de semana"; else -> ""
            }
            val s = when (slot) {
                "manana" -> "por la mañana"; "mediodia" -> "a mediodía"
                "tarde" -> "por la tarde"; "noche" -> "por la noche"; else -> ""
            }
            return listOf(d, s).filter { it.isNotBlank() }.joinToString(" ").replaceFirstChar { it.uppercase() }
                .ifBlank { "Cuando sea" }
        }

    val levelLabel: String?
        get() = when (level) {
            "iniciacion" -> "Iniciación"; "intermedio" -> "Intermedio"; "avanzado" -> "Avanzado"; else -> null
        }
}

data class AppNotification(
    val id: String,
    /** match (actividad que encaja) | info */
    val kind: String,
    val title: String,
    val body: String,
    val eventId: String?,
    val fromName: String?,
    /** pending | opened | dismissed | info */
    val status: String,
    /** Probabilidad de encaje que calculó el modelo. */
    val compat: Double?,
)

data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val recommendations: List<Scored> = emptyList(),
)
