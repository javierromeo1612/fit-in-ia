package com.marbellup.fitin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

/**
 * Cliente HTTP del backend de IA (FastAPI, carpeta backend/).
 *
 * Sin dependencias nuevas: HttpURLConnection + org.json, que vienen con Android.
 * La clave de la API de Claude vive SOLO en el servidor, nunca en el APK.
 *
 * URL por defecto: 10.0.2.2 es el «localhost» del ordenador visto desde el emulador.
 * En un móvil real, cambiadla en Perfil › Servidor de IA por la IP del portátil (misma wifi).
 */
object FitInApi {
    @Volatile
    var baseUrl: String = "http://10.0.2.2:8000"

    private fun request(method: String, path: String, body: JSONObject? = null, readTimeoutMs: Int = 30_000): String {
        val conn = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 3_000
            conn.readTimeout = readTimeoutMs
            conn.setRequestProperty("Accept", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IOException("HTTP $code: $text")
            return text
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun get(path: String) = withContext(Dispatchers.IO) { request("GET", path) }
    private suspend fun post(path: String, body: JSONObject = JSONObject()) =
        withContext(Dispatchers.IO) { request("POST", path, body) }

    // ------------------------------------------------------------------ endpoints

    /** Devuelve el motor activo («Claude» o «Motor local del servidor»), o lanza si no hay conexión. */
    suspend fun health(): String {
        val j = JSONObject(withContext(Dispatchers.IO) { request("GET", "/api/health", readTimeoutMs = 4_000) })
        return engineLabel(j.optString("llm"))
    }

    suspend fun recommend(userId: String, text: String): AiResult {
        val body = JSONObject().put("user_id", userId).put("text", text)
        return parseResult(JSONObject(post("/api/recommendations", body)))
    }

    suspend fun chat(userId: String, history: List<ChatMessage>): ChatReply {
        val msgs = JSONArray()
        for (m in history.takeLast(10)) {
            msgs.put(JSONObject().put("role", if (m.fromUser) "user" else "assistant").put("content", m.text))
        }
        val j = JSONObject(post("/api/chat", JSONObject().put("user_id", userId).put("messages", msgs)))
        return ChatReply(
            text = j.optString("reply"),
            recommendations = parseRecs(j.optJSONArray("recommendations")),
            grouping = j.optJSONObject("grouping")?.let(::parseGrouping),
        )
    }

    suspend fun notifications(userId: String): List<AppNotification> {
        val arr = JSONArray(get("/api/notifications/$userId"))
        return (0 until arr.length()).map { i ->
            val n = arr.getJSONObject(i)
            n.optJSONObject("event")?.let { ev -> parseActivity(ev)?.let { SampleData.remote[it.id] = it } }
            AppNotification(
                id = n.getString("id"),
                kind = n.optString("kind"),
                title = n.optString("title"),
                body = n.optString("body"),
                eventId = n.str("event_id"),
                fromName = n.str("from_name"),
                status = n.optString("status"),
                compat = if (n.isNull("compat")) null else n.optDouble("compat"),
            )
        }
    }

    suspend fun respond(notificationId: String, accept: Boolean) {
        post("/api/notifications/$notificationId/respond", JSONObject().put("accept", accept))
    }

    suspend fun join(eventId: String, userId: String, companion: String) {
        post("/api/events/$eventId/join", JSONObject().put("user_id", userId).put("companion", companion))
    }

    suspend fun create(userId: String, sport: Sport, needs: Set<Need>, whenText: String, level: String?): CreateResult {
        val body = JSONObject()
            .put("user_id", userId)
            .put("sport", sport.name)
            .put("needs", JSONArray(needs.map { it.name }))
            .put("when", whenText)
            .put("level", level ?: JSONObject.NULL)
        val j = JSONObject(post("/api/events", body))
        val activity = j.optJSONObject("event")?.let(::parseActivity)?.also { SampleData.remote[it.id] = it }
        return CreateResult(activity, parseGrouping(j), j.optString("reason"))
    }

    suspend fun savePreferences(userId: String, text: String): ParsedNeeds? {
        val j = JSONObject(withContext(Dispatchers.IO) {
            request("PUT", "/api/users/$userId/preferences", JSONObject().put("text", text))
        })
        return j.optJSONObject("extracted")?.let(::parseNeeds)
    }

    suspend fun simulateIncoming(userId: String) {
        post("/api/demo/incoming?user_id=$userId")
    }

    // ------------------------------------------------------------------- parsers

    private fun JSONObject.str(key: String): String? = if (!has(key) || isNull(key)) null else optString(key)

    private fun engineLabel(code: String) = if (code == "claude") "Claude" else "Motor local del servidor"

    private fun parseNeedCodes(arr: JSONArray?): List<Need> =
        if (arr == null) emptyList()
        else (0 until arr.length()).mapNotNull { i -> Need.entries.firstOrNull { it.name == arr.optString(i) } }

    private fun parseStrings(arr: JSONArray?): List<String> =
        if (arr == null) emptyList() else (0 until arr.length()).map { arr.optString(it) }

    fun parseNeeds(j: JSONObject): ParsedNeeds {
        val cuando = j.optJSONObject("cuando") ?: JSONObject()
        return ParsedNeeds(
            sport = Sport.entries.firstOrNull { it.name == j.str("actividad") },
            freeSport = j.str("actividad_libre"),
            level = j.str("nivel"),
            day = cuando.optString("dia", "cualquiera"),
            slot = cuando.optString("franja", "cualquiera"),
            needs = parseNeedCodes(j.optJSONArray("necesidades")),
            company = j.optString("compania", "indiferente"),
            summary = j.optString("preferencias_texto"),
            clarification = j.str("aclaracion"),
        )
    }

    fun parseActivity(e: JSONObject): Activity? {
        val sport = Sport.entries.firstOrNull { it.name == e.optString("sport") } ?: return null
        val reviews = e.optJSONArray("reviews") ?: JSONArray()
        val group = e.optJSONArray("group") ?: JSONArray()
        val vols = e.optJSONArray("volunteers") ?: JSONArray()
        val start = e.str("start")?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        return Activity(
            id = e.getString("id"),
            sport = sport,
            shortDate = e.optString("short_date"),
            description = e.optString("description"),
            title = e.optString("title"),
            place = e.optString("place"),
            zone = e.optString("zone"),
            schedule = e.optString("schedule"),
            nextSession = e.optString("next_session"),
            features = parseNeedCodes(e.optJSONArray("features")).toSet(),
            seal = Seal.entries.firstOrNull { it.name == e.optString("seal") } ?: Seal.Ninguno,
            verifiedOn = e.optString("verified_on"),
            confirmations = e.optInt("confirmations"),
            reviews = (0 until reviews.length()).map {
                val r = reviews.getJSONObject(it)
                Review(r.optString("author"), r.optString("date"), r.optString("text"))
            },
            group = (0 until group.length()).map {
                val g = group.getJSONObject(it)
                GroupMember(g.optString("name"), g.optString("how"))
            },
            volunteers = (0 until vols.length()).map {
                val v = vols.getJSONObject(it)
                Volunteer("v_${v.optString("name")}", v.optString("name"), v.optString("backed_by"),
                    v.optString("training"), v.optInt("sessions"))
            },
            spotsLeft = e.optInt("spots_left"),
            offPeak = e.optBoolean("off_peak"),
            howToArrive = e.optString("how_to_arrive"),
            meetingNote = e.optString("meeting_note"),
            level = e.optString("level", "todos"),
            minParticipants = e.optInt("min_participants", 2),
            status = e.optString("status", "confirmada"),
            startDate = start?.toLocalDate(),
        )
    }

    private fun parseRecs(arr: JSONArray?): List<Recommendation> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val r = arr.getJSONObject(i)
            val a = parseActivity(r.getJSONObject("event")) ?: return@mapNotNull null
            SampleData.remote[a.id] = a
            Recommendation(
                activity = a,
                match = r.optInt("match"),
                reason = r.optString("reason"),
                covered = parseNeedCodes(r.optJSONArray("covered")),
                missing = parseNeedCodes(r.optJSONArray("missing")),
                distanceKm = if (r.isNull("distance_km")) null else r.optDouble("distance_km"),
                ideal = r.optBoolean("ideal"),
                shortOfPeople = r.optInt("short_of_people"),
                proposed = r.optBoolean("proposed"),
            )
        }
    }

    private fun parseGrouping(g: JSONObject): Grouping {
        val invitedIds = mutableSetOf<String>()
        g.optJSONArray("invited")?.let { arr -> for (i in 0 until arr.length()) invitedIds += arr.getJSONObject(i).optString("id") }
        val cands = g.optJSONArray("candidates") ?: JSONArray()
        return Grouping(
            mode = g.optString("mode"),
            eventId = g.str("event_id"),
            needed = g.optInt("needed"),
            candidates = (0 until cands.length()).map {
                val c = cands.getJSONObject(it)
                val id = c.optString("id")
                Candidate(
                    id = id,
                    name = c.optString("name"),
                    compat = (c.optDouble("compat") * 100).toInt(),
                    distanceKm = c.optDouble("distance_km"),
                    shared = parseStrings(c.optJSONArray("shared")),
                    invited = id in invitedIds,
                )
            },
            message = g.optString("message"),
        )
    }

    private fun parseResult(j: JSONObject): AiResult {
        val engine = j.optJSONObject("engine")
        val usedClaude = engine != null && engine.keys().asSequence().any { engine.optString(it) == "claude" }
        return AiResult(
            needs = parseNeeds(j.getJSONObject("needs")),
            needLabels = parseStrings(j.optJSONArray("needs_labels")),
            message = j.optString("message"),
            recommendations = parseRecs(j.optJSONArray("recommendations")),
            grouping = j.optJSONObject("grouping")?.let(::parseGrouping),
            engine = if (usedClaude) "Claude" else "Motor local del servidor",
        )
    }
}
