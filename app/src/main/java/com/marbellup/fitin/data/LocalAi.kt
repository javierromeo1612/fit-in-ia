package com.marbellup.fitin.data

import java.text.Normalizer

/**
 * Motor de respaldo SIN CONEXIÓN. Imita al backend con reglas sencillas para que la demo
 * nunca se quede en blanco si falla el wifi. Mismo contrato que FitInApi.
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

    private fun timeFit(p: ParsedNeeds, a: Activity): Double {
        val off = a.startDate?.let { java.time.temporal.ChronoUnit.DAYS.between(Dates.today, it).toInt() } ?: 3
        val d = when (p.day) {
            "hoy" -> if (off == 0) 1.0 else 0.4
            "manana" -> if (off == 1) 1.0 else 0.4
            "fin_de_semana" -> if ((a.startDate?.dayOfWeek?.value ?: 1) >= 6) 1.0 else 0.3
            else -> 1.0
        }
        val hour = Regex("(\\d{1,2}):\\d{2}").find(a.shortDate)?.groupValues?.get(1)?.toIntOrNull() ?: 12
        val s = when (p.slot) {
            "manana" -> if (hour < 13) 1.0 else 0.3
            "tarde" -> if (hour in 16..19) 1.0 else 0.3
            "noche" -> if (hour >= 20) 1.0 else 0.3
            else -> 1.0
        }
        return d * s
    }

    private val people = listOf(
        Candidate("raul", "Raúl", 72, 0.7, listOf("También juega pádel", "Nivel intermedio", "Libre por la tarde"), true),
        Candidate("irene", "Irene", 66, 2.4, listOf("También juega pádel", "Le gustan los grupos mixtos"), true),
        Candidate("luis", "Luis", 54, 4.6, listOf("Busca pareja de raqueta", "Libre por la tarde"), true),
    )

    fun recommend(text: String, profileNeeds: Set<Need>): AiResult {
        val p = extract(text)
        val wanted = (p.needs + profileNeeds).distinct()
        val recs = SampleData.all.filter { it.spotsLeft > 0 }.map { a ->
            val covered = wanted.filter { it in a.features }
            val missing = wanted.filterNot { it in a.features }
            val sportS = if (p.sport == null) 0.6 else if (a.sport == p.sport) 1.0 else 0.0
            val cov = if (wanted.isEmpty()) 1.0 else covered.size.toDouble() / wanted.size
            val time = timeFit(p, a)
            val score = 0.4 * sportS + 0.3 * cov + 0.3 * time
            val short = (a.minParticipants - a.group.size - 1).coerceAtLeast(0)
            val reason = buildString {
                append(if (a.sport == p.sport) "Es ${a.sport.label.lowercase()}" else "Es otra opción")
                append(", ${a.shortDate.lowercase().replace(" · ", " a las ")}")
                if (covered.isNotEmpty()) append(", con ${covered.take(3).joinToString { it.label.lowercase() }}")
                append(".")
                if (missing.isNotEmpty()) append(" Ojo: falta ${missing.joinToString { it.label.lowercase() }}.")
                if (short > 0) append(" ${if (short == 1) "Falta 1 persona" else "Faltan $short personas"} para completar el grupo.")
            }
            Recommendation(a, (score * 100).toInt(), reason, covered, missing, null,
                sportS >= 0.6 && missing.isEmpty() && time >= 0.8, short)
        }.sortedByDescending { it.match }.take(5)

        val best = recs.firstOrNull()
        val grouping = when {
            best != null && best.ideal && best.shortOfPeople > 0 && best.activity.sport == Sport.Padel -> Grouping(
                "completar", best.activity.id, best.shortOfPeople, people,
                (if (best.shortOfPeople == 1) "A este grupo le falta 1 persona" else "A este grupo le faltan ${best.shortOfPeople} personas") +
                    " para empezar. He avisado a ${people.size} personas compatibles cerca (modo sin conexión).",
            )
            p.sport != null && (best == null || !best.ideal) -> Grouping(
                "nuevo_grupo", null, 3, people.filter { p.sport == Sport.Padel || it.id == "luis" },
                "No había un grupo ideal. Cuando vuelva la conexión crearé uno y avisaré a gente compatible.",
            )
            else -> null
        }
        return AiResult(
            needs = p,
            needLabels = wanted.map { it.label },
            message = if (best != null) "He encontrado ${recs.size} opciones. La mejor: ${best.activity.title}." else "No he encontrado nada con plazas.",
            recommendations = recs,
            grouping = grouping,
            engine = "Sin conexión",
        )
    }

    fun chat(text: String, profileNeeds: Set<Need>): ChatReply {
        val r = recommend(text, profileNeeds)
        val best = r.recommendations.firstOrNull()
        val reply = when {
            r.needs.sport == null && r.needs.needs.isEmpty() ->
                "Cuéntame qué deporte te apetece y cuándo, y te busco sitio y compañía."
            best != null -> "Te propongo ${best.activity.title.lowercase()} (${best.activity.shortDate.lowercase()}) en ${best.activity.place.lowercase()}. ${best.reason}"
            else -> "No he encontrado nada con plazas. ¿Probamos otro día?"
        }
        return ChatReply(reply, r.recommendations.take(3), r.grouping)
    }

    fun incomingInvite(n: Int): AppNotification = AppNotification(
        id = "local_$n", kind = "invite", title = "🎾 ¿Te apuntas a pádel?",
        body = "Raúl busca gente para pádel inclusivo hoy a las 18:30 en el club cubierto. Encaja contigo: nivel intermedio y libre por la tarde.",
        eventId = "padel_inclusivo", fromName = "Raúl", status = "pending", compat = 0.72,
    )
}
