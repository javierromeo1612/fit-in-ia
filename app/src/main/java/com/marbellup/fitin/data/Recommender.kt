package com.marbellup.fitin.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/*
 * MOTOR DE IA DE FIT-IN (todo en el móvil, sin APIs ni conexión).
 *
 * Modelo: regresión logística online (aprendizaje por refuerzo implícito) sobre 18 características
 * interpretables que cruzan las CARACTERÍSTICAS DEL USUARIO (sus necesidades, deportes, horario,
 * nivel y zona) con las de cada ACTIVIDAD y su INSTALACIÓN.
 *
 *   P(le encaja) = σ(w · x)
 *
 * - Arranca con pesos «expertos» (conocimiento del dominio: cubrir necesidades pesa mucho).
 * - Aprende de lo que hace la persona: apuntarse, marcar «me gusta», abrir una ficha, valorar
 *   una actividad o decir «no me interesa». Cada acción es un ejemplo de entrenamiento y los pesos
 *   se actualizan al momento con descenso de gradiente estocástico.
 * - Regularización hacia los pesos expertos: aprende tus gustos sin olvidar lo esencial
 *   (p. ej. nunca deja de penalizar que a un sitio le falte algo que necesitas).
 * - Muestreo negativo: por cada acción positiva se toma una actividad no elegida como ejemplo
 *   negativo suave (técnica estándar en recomendadores con feedback implícito).
 * - Explicable: cada recomendación lleva la contribución de cada característica (wᵢ·xᵢ), que es
 *   lo que se muestra como «Por qué te lo recomiendo».
 */

/** Franjas horarias que la persona puede elegir. */
enum class Slot(val label: String, val from: Int, val to: Int) {
    Manana("Mañanas", 7, 13),
    Mediodia("Mediodía", 13, 16),
    Tarde("Tardes", 16, 20),
    Noche("Noches", 20, 24),
}

/** Características que pone la persona usuaria. Se pregunta qué necesita, nunca qué tiene. */
data class Profile(
    val needs: Set<Need> = emptySet(),
    val sports: Set<Sport> = emptySet(),
    val slots: Set<Slot> = emptySet(),
    /** iniciacion | intermedio | avanzado | null (da igual) */
    val level: String? = null,
    val zone: String? = null,
)

/** Instalación física (los nombres son genéricos: datos de ejemplo). */
data class Facility(
    val name: String,
    val zone: String,
    val features: Set<Need>,
    val sports: Set<Sport>,
    val seal: Seal,
)

enum class Signal(val label: String, val target: Double, val weight: Double) {
    Join("Te apuntaste", 1.0, 1.0),
    RateUp("Te gustó", 1.0, 1.2),
    RateDown("No te gustó", 0.0, 1.2),
    Like("Me gusta", 1.0, 0.7),
    Unlike("Quitaste el me gusta", 0.0, 0.3),
    View("Abriste la ficha", 1.0, 0.15),
    Dismiss("No me interesa", 0.0, 1.0),
    NotifOpen("Abriste un aviso", 1.0, 0.5),
    NotifDismiss("Descartaste un aviso", 0.0, 0.5),
}

data class Interaction(val activityId: String, val signal: Signal, val at: Long)

/** Las 18 características del modelo, con su peso experto inicial y cómo se explican. */
enum class Feature(val label: String, val prior: Double, val good: String?, val bad: String?) {
    Bias("Sesgo", -2.2, null, null),
    Coverage("Cubre tus necesidades", 2.2, "Cubre lo que necesitas", null),
    Missing("Le falta algo que necesitas", -2.6, null, "Le falta algo que necesitas"),
    Sport("Deporte que te gusta", 1.2, "Es un deporte que te gusta", null),
    Similar("Parecida a lo que te gustó", 1.0, "Se parece a lo que te gustó", null),
    Disliked("Parecida a lo que no te gustó", -0.8, null, "Se parece a algo que no te gustó"),
    SportTaste("Deporte que más eliges", 1.5, "Es el deporte que más eliges", null),
    SlotTaste("Horario que más eliges", 1.0, "Es a la hora que sueles ir", null),
    LevelTaste("Nivel que más eliges", 0.6, "Es del nivel que sueles elegir", null),
    Schedule("En tu horario", 0.8, "Es en tu horario", "No es en tu horario"),
    Soon("Pronto", 0.4, "Es pronto", null),
    Level("Tu nivel", 0.5, "Es de tu nivel", "No es de tu nivel"),
    Near("Cerca de ti", 0.6, "Te pilla cerca", null),
    Verified("Sitio verificado", 0.4, "El sitio está verificado", null),
    Popular("Bien valorada", 0.3, "La gente la valora bien", null),
    Volunteers("Con voluntariado", 0.2, "Hay voluntariado para acompañarte", null),
    OffPeak("Hora tranquila", 0.1, "Es a una hora tranquila", null),
    NeedsYou("Al grupo le faltas tú", 0.2, "Al grupo le falta gente", null),
}

/** Contribución de una característica a una recomendación concreta. */
data class Reason(val feature: Feature, val value: Double, val contribution: Double) {
    val text: String? get() = if (contribution >= 0) feature.good else feature.bad
}

/** Resultado del modelo para una actividad. */
data class Scored(
    val activity: Activity,
    /** Probabilidad de que le encaje, 0..1 */
    val probability: Double,
    val covered: List<Need>,
    val missing: List<Need>,
    val reasons: List<Reason>,
) {
    val match: Int get() = (probability * 100).toInt()

    /** Explicación en una frase, con las 3 razones que más pesan. */
    val explanation: String
        get() {
            val pos = reasons
                .filter { it.contribution > 0.15 && it.text != null }
                .filterNot { it.feature == Feature.Coverage && missing.isNotEmpty() }
                .take(3).map { it.text!! }
            val neg = reasons.filter { it.contribution < -0.3 && it.text != null }.take(1).map { it.text!! }
            val base = if (pos.isEmpty()) "Puede interesarte" else pos.joinToString(", ")
            val miss = if (missing.isNotEmpty()) ". Ojo: falta ${missing.joinToString { it.label.lowercase() }}" else ""
            val extra = if (neg.isNotEmpty() && missing.isEmpty()) ". Pero ${neg[0].lowercase()}" else ""
            return "$base$miss$extra."
        }
}

data class ScoredFacility(val facility: Facility, val probability: Double, val covered: List<Need>, val missing: List<Need>) {
    val match: Int get() = (probability * 100).toInt()
}

/** Pesos del modelo. Se guardan en el móvil (ver Store). */
class LinearModel(val weights: DoubleArray = Feature.entries.map { it.prior }.toDoubleArray()) {
    var updates: Int = 0

    fun score(x: DoubleArray): Double = sigmoid(x.indices.sumOf { weights[it] * x[it] })

    /** Un paso de SGD sobre la pérdida logística, con regularización L2 hacia los pesos expertos. */
    fun update(x: DoubleArray, target: Double, sampleWeight: Double, lr: Double = LEARNING_RATE) {
        val err = score(x) - target
        for (i in weights.indices) {
            val prior = Feature.entries[i].prior
            weights[i] -= lr * (sampleWeight * err * x[i] + L2 * (weights[i] - prior))
        }
        updates++
    }

    fun reset() {
        Feature.entries.forEachIndexed { i, f -> weights[i] = f.prior }
        updates = 0
    }

    companion object {
        const val LEARNING_RATE = 0.25
        const val L2 = 0.02
        fun sigmoid(z: Double) = 1.0 / (1.0 + exp(-z))
    }
}

object Recommender {

    private val zoneOrder = listOf("Zona centro", "Zona oeste", "Zona este", "Zona norte", "Salida en autobús")

    fun hourOf(a: Activity): Int =
        Regex("(\\d{1,2}):\\d{2}").find(a.shortDate)?.groupValues?.get(1)?.toIntOrNull() ?: 12

    fun slotOf(a: Activity): Slot {
        val h = hourOf(a)
        return Slot.entries.firstOrNull { h >= it.from && h < it.to } ?: Slot.Manana
    }

    private fun proximity(user: String?, zone: String): Double = when {
        user == null -> 0.6
        user == zone -> 1.0
        zone == "Salida en autobús" -> 0.5
        user == "Zona centro" || zone == "Zona centro" -> 0.6
        else -> 0.3
    }

    private fun levelFit(want: String?, have: String): Double {
        val order = listOf("iniciacion", "intermedio", "avanzado")
        if (want == null || have == "todos" || want == have) return 1.0
        val d = kotlin.math.abs(order.indexOf(want) - order.indexOf(have))
        return if (d == 1) 0.4 else 0.0
    }

    /** Vector de contenido de una actividad (para medir parecido entre actividades). */
    fun contentVector(a: Activity): DoubleArray {
        val v = DoubleArray(Sport.entries.size + Need.entries.size + Slot.entries.size + zoneOrder.size + 4)
        var o = 0
        v[o + a.sport.ordinal] = 2.0; o += Sport.entries.size
        for (n in a.features) v[o + n.ordinal] = 0.5; o += Need.entries.size
        v[o + slotOf(a).ordinal] = 1.0; o += Slot.entries.size
        val z = zoneOrder.indexOf(a.zone); if (z >= 0) v[o + z] = 0.7; o += zoneOrder.size
        val l = listOf("todos", "iniciacion", "intermedio", "avanzado").indexOf(a.level); if (l >= 0) v[o + l] = 0.7
        return v
    }

    private fun cosine(a: DoubleArray, b: DoubleArray): Double {
        var dot = 0.0; var na = 0.0; var nb = 0.0
        for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
        return if (na == 0.0 || nb == 0.0) 0.0 else dot / (sqrt(na) * sqrt(nb))
    }

    private fun signalValue(s: Signal): Double = when (s) {
        Signal.Join, Signal.RateUp -> 2.0
        Signal.Like, Signal.NotifOpen -> 1.0
        Signal.View -> 0.3
        Signal.Unlike -> -1.0
        Signal.Dismiss, Signal.RateDown -> -2.0
        Signal.NotifDismiss -> -0.5
    }

    /** Actividades que le han gustado (positivas) y que no (negativas), según su historial. */
    fun tastes(history: List<Interaction>): Pair<Set<String>, Set<String>> {
        val score = mutableMapOf<String, Double>()
        for (i in history) score[i.activityId] = (score[i.activityId] ?: 0.0) + signalValue(i.signal)
        return score.filterValues { it > 0.5 }.keys to score.filterValues { it < 0 }.keys
    }

    /** Gustos aprendidos del historial: parecido de contenido y afinidad por deporte, horario y nivel. */
    class Taste(
        val liked: Map<String, DoubleArray>,
        val disliked: Map<String, DoubleArray>,
        val sport: Map<Sport, Double>,
        val slot: Map<Slot, Double>,
        val level: Map<String, Double>,
    ) {
        companion object {
            val EMPTY = Taste(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
        }
    }

    fun taste(history: List<Interaction>, catalog: List<Activity>): Taste {
        val byId = catalog.associateBy { it.id }
        val (likedIds, dislikedIds) = tastes(history)
        val sport = mutableMapOf<Sport, Double>(); val slot = mutableMapOf<Slot, Double>(); val level = mutableMapOf<String, Double>()
        var total = 0.0
        for (i in history) {
            val a = byId[i.activityId] ?: continue
            val v = signalValue(i.signal)
            if (v > 0) total += v
            sport[a.sport] = (sport[a.sport] ?: 0.0) + v
            slot[slotOf(a)] = (slot[slotOf(a)] ?: 0.0) + v
            level[a.level] = (level[a.level] ?: 0.0) + v
        }
        // Afinidad = proporción (suavizada) de lo que elige que es de ese deporte / horario / nivel.
        fun <K> norm(m: Map<K, Double>) = m.mapValues { (_, v) -> (v / (total + 2.0)).coerceIn(-1.0, 1.0) }
        return Taste(
            liked = likedIds.mapNotNull { id -> byId[id]?.let { id to contentVector(it) } }.toMap(),
            disliked = dislikedIds.mapNotNull { id -> byId[id]?.let { id to contentVector(it) } }.toMap(),
            sport = norm(sport), slot = norm(slot), level = norm(level),
        )
    }

    /** Construye el vector x de características para (perfil, actividad, gustos aprendidos). */
    fun features(p: Profile, a: Activity, t: Taste, today: LocalDate = Dates.today): DoubleArray {
        val x = DoubleArray(Feature.entries.size)
        val covered = p.needs.count { it in a.features }
        val missing = p.needs.size - covered
        val cv = contentVector(a)
        val days = a.startDate?.let { ChronoUnit.DAYS.between(today, it).toInt() } ?: 7
        x[Feature.Bias.ordinal] = 1.0
        x[Feature.Coverage.ordinal] = if (p.needs.isEmpty()) 1.0 else covered.toDouble() / p.needs.size
        x[Feature.Missing.ordinal] = min(missing, 3) / 3.0
        x[Feature.Sport.ordinal] = if (p.sports.isEmpty()) 0.5 else if (a.sport in p.sports) 1.0 else 0.0
        // Se excluye la propia actividad para que «se parece a lo que te gustó» no sea trivial.
        x[Feature.Similar.ordinal] = t.liked.filterKeys { it != a.id }.values.maxOfOrNull { cosine(cv, it) } ?: 0.0
        x[Feature.Disliked.ordinal] = t.disliked.filterKeys { it != a.id }.values.maxOfOrNull { cosine(cv, it) } ?: 0.0
        x[Feature.SportTaste.ordinal] = t.sport[a.sport] ?: 0.0
        x[Feature.SlotTaste.ordinal] = t.slot[slotOf(a)] ?: 0.0
        x[Feature.LevelTaste.ordinal] = t.level[a.level] ?: 0.0
        x[Feature.Schedule.ordinal] = if (p.slots.isEmpty()) 0.5 else if (slotOf(a) in p.slots) 1.0 else 0.0
        x[Feature.Soon.ordinal] = 1.0 - min(max(days, 0), 14) / 14.0
        x[Feature.Level.ordinal] = levelFit(p.level, a.level)
        x[Feature.Near.ordinal] = proximity(p.zone, a.zone)
        x[Feature.Verified.ordinal] = if (a.seal == Seal.Ninguno) 0.0 else if (a.seal == Seal.Ayuntamiento) 1.0 else 0.7
        x[Feature.Popular.ordinal] = ln(1.0 + a.confirmations) / ln(21.0)
        x[Feature.Volunteers.ordinal] = if (a.volunteers.isNotEmpty() && Need.Acompanante in p.needs) 1.0
            else if (a.volunteers.isNotEmpty()) 0.4 else 0.0
        x[Feature.OffPeak.ordinal] = if (a.offPeak) 1.0 else 0.0
        x[Feature.NeedsYou.ordinal] = if (a.group.size < a.minParticipants && a.spotsLeft > 0) 1.0 else 0.0
        return x
    }

    /** Ordena las actividades para esta persona. */
    fun rank(
        p: Profile,
        catalog: List<Activity>,
        model: LinearModel,
        history: List<Interaction>,
        today: LocalDate = Dates.today,
        /** Todas las actividades conocidas (para entender el historial aunque ya no estén disponibles). */
        known: List<Activity> = catalog,
    ): List<Scored> {
        val t = taste(history, known)
        return catalog
            .filter { it.spotsLeft > 0 && (it.startDate == null || !it.startDate.isBefore(today)) }
            .map { a ->
                val x = features(p, a, t, today)
                val reasons = Feature.entries
                    .filter { it != Feature.Bias }
                    .map { f -> Reason(f, x[f.ordinal], model.weights[f.ordinal] * x[f.ordinal]) }
                    .sortedByDescending { kotlin.math.abs(it.contribution) }
                Scored(
                    activity = a,
                    probability = model.score(x),
                    covered = p.needs.filter { it in a.features }.sortedBy { it.ordinal },
                    missing = p.needs.filterNot { it in a.features }.sortedBy { it.ordinal },
                    reasons = reasons,
                )
            }
            .sortedByDescending { it.probability }
    }

    /**
     * Entrena el modelo con una acción. Si es positiva, añade un ejemplo negativo suave con
     * una actividad que no eligió (la de peor encaje actual entre las que no ha tocado).
     */
    fun learn(
        model: LinearModel,
        p: Profile,
        catalog: List<Activity>,
        history: List<Interaction>,
        event: Interaction,
        today: LocalDate = Dates.today,
        negativeSeed: Int = history.size,
    ) {
        val a = catalog.firstOrNull { it.id == event.activityId } ?: return
        val t = taste(history, catalog)
        val x = features(p, a, t, today)
        model.update(x, event.signal.target, event.signal.weight)
        if (event.signal.target > 0.5 && event.signal.weight >= 0.5) {
            val touched = history.map { it.activityId }.toSet() + a.id
            val pool = catalog.filter { it.id !in touched }
            if (pool.isNotEmpty()) {
                val neg = pool[(negativeSeed * 7 + 3) % pool.size]
                val xn = features(p, neg, t, today)
                model.update(xn, 0.0, 0.3 * event.signal.weight)
            }
        }
    }

    /** Ordena instalaciones para un deporte según lo que necesita la persona (mismo modelo, características del sitio). */
    fun rankFacilities(p: Profile, sport: Sport?, model: LinearModel, facilities: List<Facility>): List<ScoredFacility> =
        facilities
            .filter { sport == null || sport in it.sports }
            .map { f ->
                val x = DoubleArray(Feature.entries.size)
                val siteNeeds = p.needs.filter { it in FACILITY_NEEDS }
                val covered = siteNeeds.filter { it in f.features }
                x[Feature.Bias.ordinal] = 1.0
                x[Feature.Coverage.ordinal] = if (siteNeeds.isEmpty()) 1.0 else covered.size.toDouble() / siteNeeds.size
                x[Feature.Missing.ordinal] = min(siteNeeds.size - covered.size, 3) / 3.0
                x[Feature.Sport.ordinal] = if (sport != null || f.sports.any { it in p.sports }) 1.0 else 0.0
                x[Feature.Near.ordinal] = proximity(p.zone, f.zone)
                x[Feature.Verified.ordinal] = if (f.seal == Seal.Ninguno) 0.0 else if (f.seal == Seal.Ayuntamiento) 1.0 else 0.7
                x[Feature.Schedule.ordinal] = 0.5
                x[Feature.Level.ordinal] = 1.0
                ScoredFacility(f, model.score(x), covered, siteNeeds.filterNot { it in f.features })
            }
            .sortedByDescending { it.probability }

    /** Necesidades que dependen del sitio físico (las demás dependen de la actividad). */
    val FACILITY_NEEDS = setOf(
        Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Grua, Need.Cubierta, Need.Bucle, Need.Tranquilo,
        Need.Ascensor, Need.PuertasAnchas, Need.BanoAdaptado, Need.DuchaAdaptada, Need.VestuarioIndividual,
        Need.AltoContraste, Need.Iluminacion, Need.AguaTemplada, Need.SombraAgua,
    )
}
