package com.marbellup.fitin.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Memoria del móvil: perfil, pesos del modelo, historial de acciones y avisos.
 * Todo se guarda en el propio dispositivo (SharedPreferences). Nada sale del móvil.
 */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("fitin_ia", Context.MODE_PRIVATE)

    // ---------------------------------------------------------------- perfil
    fun loadProfile(): Profile {
        val j = prefs.getString("profile", null)?.let { JSONObject(it) }
            ?: return Profile(needs = setOf(Need.SinEscalones, Need.Vestuario), zone = "Zona centro")
        return Profile(
            needs = j.optJSONArray("needs").strings().mapNotNull { n -> Need.entries.firstOrNull { it.name == n } }.toSet(),
            sports = j.optJSONArray("sports").strings().mapNotNull { n -> Sport.entries.firstOrNull { it.name == n } }.toSet(),
            slots = j.optJSONArray("slots").strings().mapNotNull { n -> Slot.entries.firstOrNull { it.name == n } }.toSet(),
            level = if (j.isNull("level")) null else j.optString("level"),
            zone = if (j.isNull("zone")) null else j.optString("zone"),
        )
    }

    fun saveProfile(p: Profile) {
        val j = JSONObject()
            .put("needs", JSONArray(p.needs.map { it.name }))
            .put("sports", JSONArray(p.sports.map { it.name }))
            .put("slots", JSONArray(p.slots.map { it.name }))
            .put("level", p.level ?: JSONObject.NULL)
            .put("zone", p.zone ?: JSONObject.NULL)
        prefs.edit().putString("profile", j.toString()).apply()
    }

    // ---------------------------------------------------------------- modelo
    fun loadModel(): LinearModel {
        val m = LinearModel()
        val arr = prefs.getString("weights", null)?.let { JSONArray(it) } ?: return m
        if (arr.length() == m.weights.size) {
            for (i in 0 until arr.length()) m.weights[i] = arr.getDouble(i)
        }
        m.updates = prefs.getInt("updates", 0)
        return m
    }

    fun saveModel(m: LinearModel) {
        prefs.edit()
            .putString("weights", JSONArray(m.weights.toList()).toString())
            .putInt("updates", m.updates)
            .apply()
    }

    // ------------------------------------------------------------- historial
    fun loadHistory(): List<Interaction> {
        val arr = prefs.getString("history", null)?.let { JSONArray(it) } ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val s = Signal.entries.firstOrNull { it.name == o.optString("s") } ?: return@mapNotNull null
            Interaction(o.getString("a"), s, o.optLong("t"))
        }
    }

    fun saveHistory(h: List<Interaction>) {
        val arr = JSONArray()
        h.takeLast(300).forEach { arr.put(JSONObject().put("a", it.activityId).put("s", it.signal.name).put("t", it.at)) }
        prefs.edit().putString("history", arr.toString()).apply()
    }

    // ------------------------------------------------------ listas sencillas
    fun loadSet(key: String): Set<String> = prefs.getStringSet(key, emptySet())?.toSet() ?: emptySet()
    fun saveSet(key: String, value: Collection<String>) {
        prefs.edit().putStringSet(key, value.toSet()).apply()
    }

    fun getBool(key: String, default: Boolean) = prefs.getBoolean(key, default)
    fun putBool(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    // ------------------------------------------------------------- avisos
    fun loadInbox(): List<AppNotification> {
        val arr = prefs.getString("inbox", null)?.let { JSONArray(it) } ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AppNotification(
                id = o.getString("id"), kind = o.optString("kind"), title = o.optString("title"),
                body = o.optString("body"), eventId = if (o.isNull("event")) null else o.optString("event"),
                fromName = null, status = o.optString("status"),
                compat = if (o.isNull("p")) null else o.optDouble("p"),
            )
        }
    }

    fun saveInbox(list: List<AppNotification>) {
        val arr = JSONArray()
        list.take(50).forEach {
            arr.put(
                JSONObject().put("id", it.id).put("kind", it.kind).put("title", it.title).put("body", it.body)
                    .put("event", it.eventId ?: JSONObject.NULL).put("status", it.status).put("p", it.compat ?: JSONObject.NULL),
            )
        }
        prefs.edit().putString("inbox", arr.toString()).apply()
    }

    // ---------------------------------------------------------- «¿Qué tal fue?»
    /** Valoraciones tras cada actividad: id → (carita, texto). Solo en el móvil. */
    fun loadReviews(): Map<String, Valoracion> {
        val j = prefs.getString("reviews", null)?.let { JSONObject(it) } ?: return emptyMap()
        return j.keys().asSequence().mapNotNull { id ->
            val o = j.optJSONObject(id) ?: return@mapNotNull null
            val c = Carita.entries.firstOrNull { it.name == o.optString("c") } ?: return@mapNotNull null
            id to Valoracion(c, o.optString("t"))
        }.toMap()
    }

    fun saveReviews(m: Map<String, Valoracion>) {
        val j = JSONObject()
        m.forEach { (id, v) -> j.put(id, JSONObject().put("c", v.carita.name).put("t", v.texto)) }
        prefs.edit().putString("reviews", j.toString()).apply()
    }

    fun clearLearning() {
        prefs.edit().remove("weights").remove("updates").remove("history").apply()
    }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { optString(it) }
}
