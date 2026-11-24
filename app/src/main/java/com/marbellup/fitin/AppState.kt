package com.marbellup.fitin

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.marbellup.fitin.data.Activity
import com.marbellup.fitin.data.AppNotification
import com.marbellup.fitin.data.Barrier
import com.marbellup.fitin.data.Carita
import com.marbellup.fitin.data.BarrierStatus
import com.marbellup.fitin.data.Dates
import com.marbellup.fitin.data.GroupMember
import com.marbellup.fitin.data.Interaction
import com.marbellup.fitin.data.Need
import com.marbellup.fitin.data.Profile
import com.marbellup.fitin.data.Recommender
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.data.Scored
import com.marbellup.fitin.data.ScoredFacility
import com.marbellup.fitin.data.Signal
import com.marbellup.fitin.data.Slot
import com.marbellup.fitin.data.Sport
import com.marbellup.fitin.data.Store
import com.marbellup.fitin.data.Valoracion
import com.marbellup.fitin.notify.Alerts

enum class Tab(val label: String) {
    Inicio("Inicio"),
    Calendario("Calendario"),
    ParaTi("Chat"),
    Perfil("Perfil"),
}

sealed interface Step {
    data class Detail(val activityId: String) : Step
    data class Company(val activityId: String) : Step
    data class Confirmed(val activityId: String) : Step
    data object Create : Step
    /** Características de la persona + recomendaciones del modelo. */
    data object AiSearch : Step
    data object Notifications : Step
    data object Council : Step
    /** «Cómo aprende tu IA»: pesos del modelo e historial. */
    data object Model : Step
    /** Buscador por palabras clave. */
    data object Search : Step
    data object Achievements : Step
    /** «Cuéntanoslo»: necesidades contadas con texto o con la voz (modelo propio, sin internet). */
    data object Cuentanoslo : Step
    /** «¿Qué tal fue?» después de una actividad. */
    data class QueTalFue(val activityId: String) : Step
}

data class Booking(val activityId: String, val companion: String)

/**
 * Estado de la app. Toda la IA corre en el móvil (ver data/Recommender.kt):
 * no hay servidor ni APIs. Cada acción de la persona entrena el modelo al momento.
 */
class AppState(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    val userName = SampleData.USER_NAME

    // ---- Modelo de IA
    val model = store.loadModel()
    val history = mutableStateListOf<Interaction>().apply { addAll(store.loadHistory()) }
    /** Cambia cada vez que el modelo aprende, para que las pantallas se recalculen. */
    var modelVersion by mutableIntStateOf(0)
        private set

    // ---- Perfil (características que pone la persona)
    var profile by mutableStateOf(store.loadProfile())
        private set
    val needs: Set<Need> get() = profile.needs

    var bigText by mutableStateOf(false)
    var easyRead by mutableStateOf(false)
    var alertsOn by mutableStateOf(store.getBool("alerts", true))
        private set
    val points: Int get() = 48 + 10 * joined.size + 2 * history.size.coerceAtMost(30) + 5 * stats().medals.count { it.unlocked }

    // ---- Navegación
    var onboarded by mutableStateOf(store.getBool("onboarded", false))
        private set
    var tab by mutableStateOf(Tab.Inicio)
    var sport by mutableStateOf<Sport?>(null)
    val steps = mutableStateListOf<Step>()

    // ---- Reservas y favoritos
    var booking by mutableStateOf<Booking?>(null)
        private set
    val joined = mutableStateListOf<String>().apply { addAll(store.loadSet("joined")) }
    val favorites = mutableStateListOf<String>().apply { addAll(store.loadSet("favorites")) }
    val reported = mutableStateListOf<Barrier>()

    // ---- Buscador
    var query by mutableStateOf("")

    // ---- Avisos
    val notifications = mutableStateListOf<AppNotification>().apply { addAll(store.loadInbox()) }
    var banner by mutableStateOf<AppNotification?>(null)
    val unread: Int get() = notifications.count { it.status == "pending" }

    // ---- «¿Qué tal fue?»
    val reviews = mutableStateMapOf<String, Valoracion>().apply { putAll(store.loadReviews()) }

    init {
        // Modelo propio de «Cuéntanoslo»: se carga de assets la primera vez que se usa.
        com.marbellup.fitin.ai.propio.CuentanosloPropio.iniciar { app.assets.open("modelo_cuentanoslo.bin") }
        // Actividades «publicadas» en demos anteriores vuelven al catálogo.
        val published = store.loadSet("published")
        SampleData.upcoming.filter { it.id in published && SampleData.created.none { c -> c.id == it.id } }
            .forEach { SampleData.created.add(0, it) }
    }

    // ================================================================ IA

    /** Actividades ordenadas por el modelo para esta persona. */
    fun ranked(): List<Scored> {
        modelVersion // lectura para recomponer cuando aprende
        return Recommender.rank(profile, SampleData.all, model, history.toList(), known = SampleData.allKnown)
    }

    fun scoreFor(id: String): Scored? = ranked().firstOrNull { it.activity.id == id }

    /**
     * Buscador por palabras clave: busca en título, deporte, descripción, sitio, zona, nivel y
     * accesibilidad (sin tildes ni mayúsculas). Los resultados salen ordenados por la IA.
     */
    fun search(q: String = query): List<Scored> {
        val words = normalize(q).split(" ").filter { it.length >= 2 }
        if (words.isEmpty()) return emptyList()
        return ranked().filter { s ->
            val a = s.activity
            val text = normalize(
                listOf(a.title, a.sport.label, a.description, a.place, a.zone, a.level, a.shortDate,
                    a.features.joinToString(" ") { it.label }).joinToString(" "),
            )
            words.all { it in text }
        }
    }

    private fun normalize(t: String): String =
        java.text.Normalizer.normalize(t.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "").replace(Regex("[^a-z0-9ñ ]"), " ")

    fun stats() = com.marbellup.fitin.data.Achievements.compute(joined.toList(), history.toList(), favorites.toList())

    fun facilitiesFor(sport: Sport?, extra: Set<Need> = emptySet()): List<ScoredFacility> {
        modelVersion
        return Recommender.rankFacilities(profile.copy(needs = profile.needs + extra), sport, model, SampleData.facilities)
    }

    /** Registra una acción, entrena el modelo con ella y comprueba si hay algo nuevo que avisar. */
    fun record(activityId: String, signal: Signal) {
        val e = Interaction(activityId, signal, System.currentTimeMillis())
        Recommender.learn(model, profile, SampleData.allKnown, history.toList(), e)
        history.add(e)
        store.saveModel(model)
        store.saveHistory(history)
        modelVersion++
        if (signal.target > 0.5 && signal.weight >= 0.5) {
            val a = SampleData.activityOrNull(activityId)
            checkForMatches(if (a != null) "Como te interesó ${a.sport.label.lowercase()}" else null)
        }
    }

    fun updateProfile(p: Profile) {
        profile = p
        store.saveProfile(p)
        modelVersion++
    }

    /** Guarda lo que la persona confirmó en «Cuéntanoslo»: se suma a lo que ya tenía. */
    fun aplicarPerfil(elegidas: Set<Need>, deportes: List<Sport>, franja: com.marbellup.fitin.ai.PerfilIA.Franja) {
        updateProfile(
            profile.copy(
                needs = profile.needs + elegidas,
                sports = profile.sports + deportes,
                slots = franja.slot?.let { profile.slots + it } ?: profile.slots,
            ),
        )
    }

    /** «¿Qué tal fue?»: guarda la valoración y enseña al modelo (Regular = no le gustó). */
    fun valorar(activityId: String, carita: Carita, texto: String) {
        reviews[activityId] = Valoracion(carita, texto.trim())
        store.saveReviews(reviews)
        rate(activityId, carita != Carita.Regular)
    }

    fun toggle(need: Need) = updateProfile(profile.copy(needs = if (need in needs) needs - need else needs + need))
    fun toggleSport(s: Sport) = updateProfile(profile.copy(sports = if (s in profile.sports) profile.sports - s else profile.sports + s))
    fun toggleSlot(s: Slot) = updateProfile(profile.copy(slots = if (s in profile.slots) profile.slots - s else profile.slots + s))
    fun setLevel(level: String?) = updateProfile(profile.copy(level = level))
    fun setZone(zone: String?) = updateProfile(profile.copy(zone = zone))

    /** Vuelve al estado inicial de la demo: la IA olvida lo aprendido, los avisos y las reservas de prueba. */
    fun resetLearning() {
        model.reset()
        history.clear()
        store.clearLearning()
        notifications.clear(); store.saveInbox(emptyList())
        joined.clear(); store.saveSet("joined", emptyList())
        favorites.clear(); store.saveSet("favorites", emptyList())
        store.saveSet("notified", emptyList())
        SampleData.created.removeAll { a -> SampleData.upcoming.any { it.id == a.id } }
        store.saveSet("published", emptyList())
        banner = null
        modelVersion++
    }

    // ================================================================ navegación

    fun startOnboarding() {
        finishOnboarding()
        steps.clear()
        steps.add(Step.Cuentanoslo)
    }

    fun finishOnboarding() {
        onboarded = true
        store.putBool("onboarded", true)
        tab = Tab.Inicio
    }

    fun open(step: Step) {
        steps.add(step)
    }

    fun back(): Boolean {
        if (steps.isEmpty()) return false
        steps.removeAt(steps.lastIndex)
        return true
    }

    fun closeFlow() {
        steps.clear()
        tab = Tab.Calendario
    }

    fun openFromNotification(activityId: String) {
        if (SampleData.activityOrNull(activityId) == null) return
        onboarded = true
        val i = notifications.indexOfFirst { it.eventId == activityId && it.status == "pending" }
        if (i >= 0) notifications[i] = notifications[i].copy(status = "opened")
        store.saveInbox(notifications)
        record(activityId, Signal.NotifOpen)
        steps.clear()
        steps.add(Step.Detail(activityId))
    }

    // ================================================================ acciones que enseñan al modelo

    fun toggleFavorite(id: String) {
        if (id in favorites) {
            favorites.remove(id); record(id, Signal.Unlike)
        } else {
            favorites.add(id); record(id, Signal.Like)
        }
        store.saveSet("favorites", favorites)
    }

    fun viewed(id: String) {
        // Solo cuenta la primera vez que abre cada ficha.
        if (history.none { it.activityId == id && it.signal == Signal.View }) record(id, Signal.View)
    }

    fun dismiss(id: String) {
        record(id, Signal.Dismiss)
        back()
    }

    fun rate(id: String, liked: Boolean) = record(id, if (liked) Signal.RateUp else Signal.RateDown)

    fun ratingOf(id: String): Boolean? =
        history.lastOrNull { it.activityId == id && (it.signal == Signal.RateUp || it.signal == Signal.RateDown) }
            ?.let { it.signal == Signal.RateUp }

    fun book(activityId: String, companion: String) {
        booking = Booking(activityId, companion)
        if (activityId !in joined) joined.add(activityId)
        store.saveSet("joined", joined)
        steps.clear()
        steps.add(Step.Confirmed(activityId))
        record(activityId, Signal.Join)
    }

    fun reportBarrier(activityId: String, what: String) {
        val place = SampleData.activity(activityId).place
        reported.add(0, Barrier(place, what, 1, BarrierStatus.SinResolver))
    }

    // ================================================================ crear actividad

    /** Crea un grupo en la instalación que el modelo considera mejor para lo que necesitas. */
    fun createGroup(sport: Sport, place: ScoredFacility, dayOffset: Int, time: String, level: String?): Activity {
        val id = "grupo_${System.currentTimeMillis()}"
        val a = Activity(
            id = id, sport = sport, shortDate = Dates.short(dayOffset, time), startDate = Dates.date(dayOffset),
            description = "Grupo creado por $userName. La IA eligió esta instalación porque cubre lo que necesitas.",
            title = "${sport.label} en grupo" + (level?.let { " · nivel ${if (it == "iniciacion") "iniciación" else it}" } ?: ""),
            place = place.facility.name, zone = place.facility.zone,
            schedule = Dates.weekdayName(dayOffset) + ", $time", nextSession = Dates.long(dayOffset, time),
            features = place.facility.features + needs.filter { it !in Recommender.FACILITY_NEEDS },
            seal = place.facility.seal, verifiedOn = "", confirmations = 0, reviews = emptyList(),
            group = listOf(GroupMember(userName, "Organiza")), volunteers = emptyList(),
            spotsLeft = if (sport == Sport.Padel || sport == Sport.Tenis) 3 else 7, offPeak = false,
            howToArrive = "Consulta la ficha de la instalación.", meetingNote = "Se queda en la entrada diez minutos antes.",
            level = level ?: "todos", minParticipants = if (sport == Sport.Padel) 4 else 2, status = "propuesta",
        )
        SampleData.created.add(0, a)
        if (id !in joined) joined.add(id)
        modelVersion++
        return a
    }

    // ================================================================ avisos

    /** Busca la mejor actividad aún no avisada; si supera el umbral, avisa (bandeja + sistema). */
    fun checkForMatches(prefix: String? = null): Scored? {
        if (!alertsOn) return null
        val s = Alerts.candidate(store, model, profile, history.toList()) ?: return null
        notifyAbout(s, prefix?.let { "$it, te puede encajar" } ?: "✨ Te puede encajar")
        return s
    }

    private fun notifyAbout(s: Scored, title: String) {
        val n = Alerts.toInbox(s, title)
        notifications.add(0, n)
        store.saveInbox(notifications)
        store.saveSet("notified", store.loadSet("notified") + s.activity.id)
        banner = n
        Alerts.post(getApplication<Application>(), n)
    }

    /**
     * Demo: el Ayuntamiento publica una actividad nueva. La IA la puntúa y SOLO avisa si encaja
     * (≥ 70 %). Si no, lo deja en la bandeja como información, con el porqué.
     */
    fun publishNext(): Scored? {
        val published = store.loadSet("published")
        val next = SampleData.upcoming.firstOrNull { it.id !in published } ?: return null
        store.saveSet("published", published + next.id)
        if (SampleData.created.none { it.id == next.id }) SampleData.created.add(0, next)
        modelVersion++
        val s = scoreFor(next.id) ?: return null
        if (alertsOn && s.probability >= Alerts.THRESHOLD) {
            notifyAbout(s, "✨ Nueva actividad que encaja contigo")
        } else {
            val info = AppNotification(
                id = "info_${next.id}", kind = "info",
                title = "Publicada: ${next.title}",
                body = "La IA le da un ${s.match} % de encaje contigo, así que no te ha avisado. ${s.explanation}",
                eventId = next.id, fromName = null, status = "info", compat = s.probability,
            )
            notifications.add(0, info)
            store.saveInbox(notifications)
            banner = info
        }
        return s
    }

    fun openNotification(n: AppNotification) {
        val i = notifications.indexOfFirst { it.id == n.id }
        if (i >= 0 && n.status == "pending") notifications[i] = n.copy(status = "opened")
        store.saveInbox(notifications)
        if (n.eventId != null) {
            if (n.kind == "match" && n.status == "pending") record(n.eventId, Signal.NotifOpen)
            open(Step.Detail(n.eventId))
        }
        if (banner?.id == n.id) banner = null
    }

    fun dismissNotification(n: AppNotification) {
        val i = notifications.indexOfFirst { it.id == n.id }
        if (i >= 0) notifications[i] = n.copy(status = "dismissed")
        store.saveInbox(notifications)
        if (n.eventId != null && n.kind == "match") record(n.eventId, Signal.NotifDismiss)
        if (banner?.id == n.id) banner = null
    }

    fun setAlerts(on: Boolean) {
        alertsOn = on
        store.putBool("alerts", on)
    }
}
