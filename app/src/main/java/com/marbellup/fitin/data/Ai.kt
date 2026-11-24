package com.marbellup.fitin.data

/*
 * Modelos de la capa de IA. Reflejan las respuestas JSON del backend (carpeta backend/)
 * y los usa también el motor local sin conexión (LocalAi.kt).
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

data class Recommendation(
    val activity: Activity,
    /** Encaje 0-100 que muestra la tarjeta. */
    val match: Int,
    /** Justificación breve generada por la IA. */
    val reason: String,
    val covered: List<Need>,
    val missing: List<Need>,
    val distanceKm: Double?,
    val ideal: Boolean,
    val shortOfPeople: Int,
    /** Grupo nuevo propuesto por la IA (no existía ningún evento ideal). */
    val proposed: Boolean = false,
)

/** Persona compatible encontrada por el smart grouping. Solo nombre de pila y lo que tenéis en común. */
data class Candidate(
    val id: String,
    val name: String,
    val compat: Int,
    val distanceKm: Double,
    val shared: List<String>,
    val invited: Boolean,
)

data class Grouping(
    /** completar | nuevo_grupo | sin_instalacion */
    val mode: String,
    val eventId: String?,
    val needed: Int,
    val candidates: List<Candidate>,
    val message: String,
) {
    val invitedCount: Int get() = candidates.count { it.invited }
}

data class AiResult(
    val needs: ParsedNeeds,
    val needLabels: List<String>,
    val message: String,
    val recommendations: List<Recommendation>,
    val grouping: Grouping?,
    /** «Claude», «Motor local del servidor» o «Sin conexión». */
    val engine: String,
)

data class AppNotification(
    val id: String,
    /** invite | group_complete | info */
    val kind: String,
    val title: String,
    val body: String,
    val eventId: String?,
    val fromName: String?,
    /** pending | accepted | declined | info */
    val status: String,
    val compat: Double?,
)

data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val recommendations: List<Recommendation> = emptyList(),
    val grouping: Grouping? = null,
)

data class ChatReply(val text: String, val recommendations: List<Recommendation>, val grouping: Grouping?)

/** Resultado de «Crear actividad»: la IA elige sitio y hora e invita a gente compatible. */
data class CreateResult(val activity: Activity?, val grouping: Grouping, val reason: String)
