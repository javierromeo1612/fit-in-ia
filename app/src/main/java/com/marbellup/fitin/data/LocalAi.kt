package com.marbellup.fitin.data

import java.text.Normalizer

/**
 * Procesamiento de lenguaje natural LOCAL (sin APIs): entiende frases como
 * «pádel nivel intermedio hoy por la tarde, pista cubierta y accesible» y las convierte
 * en características del perfil (deporte, nivel, horario y necesidades).
 */
object LocalAi {

    private fun norm(t: String): String =
        Normalizer.normalize(t.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    private val sportWords = mapOf(
        Sport.Padel to listOf("padel", "pala"),
        Sport.Natacion to listOf("natacion", "nadar", "piscina"),
        Sport.Baloncesto to listOf("baloncesto", "basket", "canasta"),
        Sport.Tenis to listOf("tenis"),
        Sport.Golf to listOf("golf"),
        Sport.Senderismo to listOf("senderismo", "sendero", "ruta", "montana", "caminar"),
        Sport.Ciclismo to listOf("ciclismo", "bici", "handbike"),
    )

    private val needWords = mapOf(
        Need.SinEscalones to listOf("accesible", "silla de ruedas", "sin escalones", "rampa", "en silla"),
        Need.Vestuario to listOf("vestuario", "bano adaptado", "silla de ruedas"),
        Need.Aparcamiento to listOf("aparcamiento", "aparcar", "parking"),
        Need.Grua to listOf("grua"),
        Need.Transporte to listOf("transporte", "no tengo coche"),
        Need.Cubierta to listOf("cubierta", "cubierto", "techada", "interior"),
        Need.Signos to listOf("signos", "interprete"),
        Need.Bucle to listOf("bucle", "audifono"),
        Need.Guia to listOf("guia", "ciego", "ciega", "no veo"),
        Need.Tranquilo to listOf("tranquilo", "tranquila", "poco ruido", "calma"),
        Need.LecturaFacil to listOf("sencillo", "lectura facil"),
        Need.Pictogramas to listOf("pictograma"),
        Need.GrupoReducido to listOf("grupo pequeno", "pocas personas"),
        Need.Material to listOf("material adaptado", "adaptado", "adaptada"),
        Need.Monitor to listOf("monitor", "que me ayude", "apoyo"),
        Need.Acompanante to listOf("acompan", "no ir solo", "no ir sola"),
    )

    fun extract(text: String): ParsedNeeds {
        val t = norm(text)
        val sport = sportWords.entries.firstOrNull { (_, ws) -> ws.any { it in t } }?.key
        val needs = needWords.filter { (n, ws) -> ws.any { it in t } && (n != Need.Grua || sport == Sport.Natacion) }.keys.toList()
        val level = when {
            "intermedio" in t || "nivel medio" in t -> "intermedio"
            "principiante" in t || "empezar" in t || "iniciacion" in t -> "iniciacion"
            "avanzado" in t -> "avanzado"
            else -> null
        }
        val day = when {
            "hoy" in t || "esta tarde" in t -> "hoy"
            Regex("manana (por|a) ").containsMatchIn(t) || t.startsWith("manana") -> "manana"
            "finde" in t || "fin de semana" in t || "sabado" in t || "domingo" in t -> "fin_de_semana"
            "semana" in t -> "esta_semana"
            else -> "cualquiera"
        }
        val slot = when {
            "tarde" in t -> "tarde"
            "noche" in t -> "noche"
            "por la manana" in t -> "manana"
            else -> "cualquiera"
        }
        return ParsedNeeds(
            sport = sport, freeSport = null, level = level, day = day, slot = slot, needs = needs,
            company = if (Need.Acompanante in needs) "con_acompanante" else "indiferente",
            summary = text,
            clarification = if (sport == null) "¿Qué deporte te apetece? Por ejemplo pádel, natación o baloncesto." else null,
        )
    }

    /** Pasa lo entendido a las características del perfil (sin borrar lo que ya tenía). */
    fun applyTo(p: Profile, n: ParsedNeeds): Profile {
        val slot = when (n.slot) {
            "manana" -> Slot.Manana; "mediodia" -> Slot.Mediodia; "tarde" -> Slot.Tarde; "noche" -> Slot.Noche; else -> null
        }
        return p.copy(
            needs = p.needs + n.needs,
            sports = if (n.sport != null) p.sports + n.sport else p.sports,
            slots = if (slot != null) p.slots + slot else p.slots,
            level = n.level ?: p.level,
        )
    }
}
