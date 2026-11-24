package com.marbellup.fitin.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

/**
 * SIMULACIÓN QUE DEMUESTRA QUE EL MODELO APRENDE.
 *
 * 300 personas sintéticas con gustos OCULTOS (deportes favoritos, horario, nivel) y necesidades.
 * En la app solo declaran sus necesidades y, como mucho, uno de sus deportes: el resto lo tiene
 * que descubrir el modelo por cómo se comportan.
 *
 * Cada semana hay 25 actividades disponibles (de un catálogo de 120). La app enseña las 3 mejores
 * según el modelo; la persona se apunta a la que más le gusta si alguna le convence, o pulsa
 * «No me interesa» en la peor. El modelo aprende de eso.
 *
 * Métrica: acierto@3 = la actividad que de verdad era la mejor para esa persona esa semana está
 * entre las 3 que le enseñamos. Se compara:
 *   - Modelo que aprende (el de la app)
 *   - Mismo modelo SIN aprender (solo pesos expertos)
 *   - Orden aleatorio
 *
 * Ejecutar en Android Studio: clic derecho en este archivo › Run.
 */
class SimulacionAprendizajeTest {

    @Test
    fun elModeloAprendeYSuperaAlEstatico() {
        val r = Simulacion.run(seed = 42)
        println(r.informe())
        val learnedEnd = r.learning.takeLast(4).average()
        val staticEnd = r.static.takeLast(4).average()
        assertTrue("El modelo que aprende debería superar al estático ($learnedEnd vs $staticEnd)", learnedEnd > staticEnd + 0.05)
        assertTrue("Debe mejorar con las semanas", r.learning.takeLast(4).average() > r.learning.take(2).average())
    }
}

object Simulacion {
    data class Resultado(val learning: List<Double>, val static: List<Double>, val random: List<Double>, val needsOk: Double, val needsOkBase: Double) {
        fun informe(): String = buildString {
            appendLine("Semana | Aprende | Sin aprender | Aleatorio   (acierto@3)")
            for (w in learning.indices) {
                appendLine("%6d | %6.0f%% | %11.0f%% | %8.0f%%".format(w + 1, learning[w] * 100, static[w] * 100, random[w] * 100))
            }
            appendLine("Del top-3 recomendado, cubren TODAS las necesidades: %.0f%% (entre todas las disponibles solo el %.0f%%)".format(needsOk * 100, needsOkBase * 100))
        }
    }

    private val today: LocalDate = LocalDate.of(2026, 11, 23)
    private val zones = listOf("Zona centro", "Zona oeste", "Zona este", "Zona norte")
    private val hours = listOf(9, 10, 11, 12, 17, 18, 19, 21)
    private val extraNeeds = listOf(Need.Monitor, Need.Material, Need.GrupoReducido, Need.Signos, Need.Acompanante, Need.Guia, Need.LecturaFacil)
    private val levels = listOf("todos", "iniciacion", "intermedio", "avanzado")
    private val userNeedPool = listOf(Need.SinEscalones, Need.Vestuario, Need.Grua, Need.Cubierta, Need.Monitor,
        Need.Signos, Need.Acompanante, Need.Tranquilo, Need.Material, Need.GrupoReducido)

    fun catalog(rnd: Random): List<Activity> = List(120) { i ->
        val sport = Sport.entries[rnd.nextInt(Sport.entries.size)]
        val fac = SampleData.facilities.filter { sport in it.sports }.let { it[rnd.nextInt(it.size)] }
        val h = hours[rnd.nextInt(hours.size)]
        val day = rnd.nextInt(0, 14)
        val feats = fac.features + extraNeeds.filter { rnd.nextDouble() < 0.35 }
        Activity(
            id = "a$i", sport = sport, shortDate = "X · $h:00", description = "", title = "${sport.label} $i",
            place = fac.name, zone = fac.zone, schedule = "", nextSession = "", features = feats,
            seal = fac.seal, verifiedOn = "", confirmations = rnd.nextInt(0, 20), reviews = emptyList(),
            group = List(rnd.nextInt(1, 5)) { GroupMember("p$it", "") }, volunteers = emptyList(),
            spotsLeft = rnd.nextInt(1, 6), offPeak = h in 10..12, howToArrive = "", meetingNote = "",
            level = levels[rnd.nextInt(levels.size)], minParticipants = 2, startDate = today.plusDays(day.toLong()),
        )
    }

    private class User(val needs: Set<Need>, val favSports: Set<Sport>, val slots: Set<Slot>, val level: String, val zone: String, val stated: Profile)

    private fun user(rnd: Random): User {
        val needs = userNeedPool.shuffled(rnd).take(rnd.nextInt(0, 4)).toSet()
        val fav = Sport.entries.shuffled(rnd).take(rnd.nextInt(1, 3)).toSet()
        val slots = Slot.entries.shuffled(rnd).take(rnd.nextInt(1, 3)).toSet()
        val level = listOf("iniciacion", "intermedio", "avanzado")[rnd.nextInt(3)]
        val zone = zones[rnd.nextInt(zones.size)]
        // Lo que declara en la app: sus necesidades (siempre) y, a veces, uno de sus deportes.
        val stated = Profile(needs = needs, sports = if (rnd.nextBoolean()) setOf(fav.first()) else emptySet(), zone = zone)
        return User(needs, fav, slots, level, zone, stated)
    }

    /** Lo que de verdad le gusta a la persona (el modelo no lo ve). */
    private fun utility(u: User, a: Activity, rnd: Random): Double {
        val missing = u.needs.count { it !in a.features }
        var s = 0.0
        s += if (missing == 0) 2.0 else -2.5 * missing
        s += if (a.sport in u.favSports) 2.5 else 0.0
        s += if (Recommender.slotOf(a) in u.slots) 1.5 else 0.0
        s += if (a.level == "todos" || a.level == u.level) 0.8 else 0.0
        s += if (a.zone == u.zone) 0.5 else 0.0
        return s + rnd.nextDouble(-0.3, 0.3)
    }

    fun run(seed: Int = 42, users: Int = 300, weeks: Int = 12): Resultado {
        val rnd = Random(seed)
        val cat = catalog(rnd)
        val learn = DoubleArray(weeks); val stat = DoubleArray(weeks); val rand = DoubleArray(weeks)
        var okNeeds = 0; var shown = 0; var okBase = 0; var seen = 0
        repeat(users) {
            val u = user(rnd)
            val model = LinearModel()
            val prior = LinearModel()
            val history = mutableListOf<Interaction>()
            for (w in 0 until weeks) {
                val available = cat.shuffled(rnd).take(25)
                val util = available.associate { it.id to utility(u, it, rnd) }
                val best = util.maxBy { it.value }.key

                val top = Recommender.rank(u.stated, available, model, history, today, known = cat).take(3)
                val topStatic = Recommender.rank(u.stated, available, prior, emptyList(), today).take(3)
                val topRandom = available.shuffled(rnd).take(3)
                if (top.any { it.activity.id == best }) learn[w] += 1.0
                if (topStatic.any { it.activity.id == best }) stat[w] += 1.0
                if (topRandom.any { it.id == best }) rand[w] += 1.0
                top.forEach { if (it.missing.isEmpty()) okNeeds++; shown++ }
                available.forEach { a -> if (u.needs.all { it in a.features }) okBase++; seen++ }

                // La persona reacciona a lo que ve y el modelo aprende.
                val chosen = top.maxBy { util[it.activity.id]!! }
                val event = if (util[chosen.activity.id]!! > 3.0) Interaction(chosen.activity.id, Signal.Join, 0)
                    else Interaction(top.minBy { util[it.activity.id]!! }.activity.id, Signal.Dismiss, 0)
                // Además explora un poco: abre alguna actividad de su deporte favorito fuera del top-3.
                val browsed = available.filter { it.sport in u.favSports && util[it.id]!! > 3.0 }.randomOrNull(rnd)
                for (e in listOfNotNull(event, browsed?.let { Interaction(it.id, Signal.Like, 0) })) {
                    Recommender.learn(model, u.stated, cat, history, e, today)
                    history += e
                }
            }
        }
        return Resultado(learn.map { it / users }, stat.map { it / users }, rand.map { it / users }, okNeeds.toDouble() / shown, okBase.toDouble() / seen)
    }
}
