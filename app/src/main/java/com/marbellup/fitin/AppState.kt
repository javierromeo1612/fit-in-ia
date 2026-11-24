package com.marbellup.fitin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.marbellup.fitin.data.AiResult
import com.marbellup.fitin.data.AppNotification
import com.marbellup.fitin.data.Barrier
import com.marbellup.fitin.data.BarrierStatus
import com.marbellup.fitin.data.ChatMessage
import com.marbellup.fitin.data.CreateResult
import com.marbellup.fitin.data.FitInApi
import com.marbellup.fitin.data.LocalAi
import com.marbellup.fitin.data.Need
import com.marbellup.fitin.data.Recommendation
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.data.Sport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

enum class Tab(val label: String) {
    Inicio("Inicio"),
    Calendario("Calendario"),
    Chat("Chat"),
    Perfil("Perfil"),
}

sealed interface Step {
    data class Detail(val activityId: String) : Step
    data class Company(val activityId: String) : Step
    data class Confirmed(val activityId: String) : Step
    data object Create : Step
    /** Necesidades en lenguaje natural + recomendaciones de la IA. */
    data object AiSearch : Step
    data object Notifications : Step
    data object Council : Step
}

data class Booking(val activityId: String, val companion: String)

/**
 * Estado de la app. La IA vive en el backend; si no responde, se usa [LocalAi]
 * para que la demo funcione igual sin conexión.
 */
class AppState : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val userId = "lucia"
    val userName = SampleData.USER_NAME

    // ---- Perfil y accesibilidad
    var needs by mutableStateOf(setOf(Need.SinEscalones, Need.Vestuario))
        private set
    var bigText by mutableStateOf(false)
    var easyRead by mutableStateOf(false)
    var points by mutableStateOf(48)
        private set

    // ---- Navegación
    /** La app abre en la bienvenida; «Comenzar» lleva al onboarding con IA. */
    var onboarded by mutableStateOf(false)
        private set
    var tab by mutableStateOf(Tab.Inicio)
    var sport by mutableStateOf<Sport?>(null)
    val steps = mutableStateListOf<Step>()

    // ---- Reservas y favoritos
    var booking by mutableStateOf<Booking?>(null)
        private set
    val joined = mutableStateListOf<String>()
    val favorites = mutableStateListOf<String>()
    val reported = mutableStateListOf<Barrier>()

    // ---- IA: búsqueda
    var prompt by mutableStateOf("")
    var searching by mutableStateOf(false)
        private set
    var result by mutableStateOf<AiResult?>(null)
        private set
    /** Justificación de la IA por actividad, para mostrarla también en la ficha. */
    val reasons = mutableStateMapOf<String, Recommendation>()

    /** «Claude», «Motor local del servidor», «Sin conexión» o null si aún no se sabe. */
    var engine by mutableStateOf<String?>(null)
        private set
    var serverUrl by mutableStateOf(FitInApi.baseUrl)
        private set

    // ---- IA: chat
    val chat = mutableStateListOf(
        ChatMessage(false, "¡Hola, $userName! ¿En qué puedo ayudarte hoy? Cuéntame qué te apetece y lo que necesitas."),
    )
    var chatLoading by mutableStateOf(false)
        private set

    // ---- IA: crear actividad
    var creating by mutableStateOf(false)
        private set
    var createResult by mutableStateOf<CreateResult?>(null)

    // ---- Notificaciones
    val notifications = mutableStateListOf<AppNotification>()
    var banner by mutableStateOf<AppNotification?>(null)
    private var localCounter = 0

    val unread: Int get() = notifications.count { it.status == "pending" }

    // ================================================================ navegación

    fun startOnboarding() {
        onboarded = true
        tab = Tab.Inicio
        steps.clear()
        steps.add(Step.AiSearch)
        checkServer()
    }

    fun finishOnboarding() {
        onboarded = true
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

    fun toggle(need: Need) {
        needs = if (need in needs) needs - need else needs + need
    }

    fun toggleFavorite(id: String) {
        if (id in favorites) favorites.remove(id) else favorites.add(id)
    }

    fun updateServer(url: String) {
        serverUrl = url
        FitInApi.baseUrl = url
        checkServer()
    }

    fun checkServer() {
        scope.launch {
            engine = try {
                FitInApi.health()
            } catch (e: Exception) {
                "Sin conexión"
            }
            if (engine != "Sin conexión") refreshNotifications()
        }
    }

    // ================================================================ IA

    /** Paso 1 + 2 del flujo: lenguaje natural → necesidades → evento e instalación recomendados. */
    fun search(text: String = prompt) {
        if (text.isBlank() || searching) return
        prompt = text
        searching = true
        scope.launch {
            val r = try {
                FitInApi.recommend(userId, text)
            } catch (e: Exception) {
                LocalAi.recommend(text, needs)
            }
            engine = r.engine
            result = r
            needs = needs + r.needs.needs
            r.recommendations.forEach { reasons[it.activity.id] = it }
            searching = false
            if (r.engine != "Sin conexión") refreshNotifications()
        }
    }

    fun sendChat(text: String) {
        if (text.isBlank() || chatLoading) return
        chat.add(ChatMessage(true, text.trim()))
        chatLoading = true
        scope.launch {
            val reply = try {
                FitInApi.chat(userId, chat.toList()).also { engine = engine ?: "Claude" }
            } catch (e: Exception) {
                engine = "Sin conexión"
                LocalAi.chat(chat.filter { it.fromUser }.takeLast(3).joinToString(" ") { it.text }, needs)
            }
            reply.recommendations.forEach { reasons[it.activity.id] = it }
            chat.add(ChatMessage(false, reply.text, reply.recommendations, reply.grouping))
            chatLoading = false
        }
    }

    /** «Crear actividad»: la IA elige instalación y hora, y avisa a personas compatibles. */
    fun createWithAi(sport: Sport, chosen: Set<Need>, whenText: String, level: String?) {
        creating = true
        createResult = null
        scope.launch {
            createResult = try {
                FitInApi.create(userId, sport, chosen + needs, whenText, level)
            } catch (e: Exception) {
                val r = LocalAi.recommend("${sport.label} $whenText", chosen + needs)
                val g = r.grouping ?: com.marbellup.fitin.data.Grouping("nuevo_grupo", null, 3, emptyList(), r.message)
                com.marbellup.fitin.data.CreateResult(r.recommendations.firstOrNull { it.activity.sport == sport }?.activity, g,
                    "Sin conexión: te enseño la opción más parecida que ya existe.")
            }
            creating = false
        }
    }

    // ================================================================ reservas

    fun book(activityId: String, companion: String) {
        booking = com.marbellup.fitin.Booking(activityId, companion)
        if (activityId !in joined) joined.add(activityId)
        points += 10
        steps.clear()
        steps.add(Step.Confirmed(activityId))
        scope.launch { runCatching { FitInApi.join(activityId, userId, companion) } }
    }

    fun reportBarrier(activityId: String, what: String) {
        val place = SampleData.activity(activityId).place
        reported.add(0, Barrier(place, what, 1, BarrierStatus.SinResolver))
    }

    // ================================================================ notificaciones

    fun refreshNotifications() {
        scope.launch {
            val list = try {
                FitInApi.notifications(userId)
            } catch (e: Exception) {
                return@launch
            }
            val known = notifications.map { it.id }.toSet()
            val fresh = list.firstOrNull { it.id !in known && it.kind != "info" }
            val locals = notifications.filter { it.id.startsWith("local_") }
            notifications.clear()
            notifications.addAll(locals + list)
            if (fresh != null && known.isNotEmpty()) banner = fresh
        }
    }

    /** Demo: alguien compatible busca lo mismo y la IA te invita (lado receptor del smart grouping). */
    fun simulateIncoming() {
        scope.launch {
            try {
                FitInApi.simulateIncoming(userId)
                val before = notifications.map { it.id }.toSet()
                val list = FitInApi.notifications(userId)
                notifications.clear()
                notifications.addAll(list)
                banner = list.firstOrNull { it.id !in before } ?: list.firstOrNull()
            } catch (e: Exception) {
                val n = LocalAi.incomingInvite(++localCounter)
                notifications.add(0, n)
                banner = n
            }
        }
    }

    fun respond(n: AppNotification, accept: Boolean) {
        val i = notifications.indexOfFirst { it.id == n.id }
        if (i >= 0) notifications[i] = n.copy(status = if (accept) "accepted" else "declined")
        if (accept && n.eventId != null) {
            if (n.eventId !in joined) joined.add(n.eventId)
            points += 10
        }
        if (banner?.id == n.id) banner = null
        if (!n.id.startsWith("local_")) {
            scope.launch {
                runCatching { FitInApi.respond(n.id, accept) }
                refreshNotifications()
            }
        }
    }

    override fun onCleared() {
        scope.cancel()
    }
}
