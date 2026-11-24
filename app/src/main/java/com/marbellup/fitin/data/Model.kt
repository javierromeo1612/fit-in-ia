package com.marbellup.fitin.data

import java.time.DayOfWeek
import java.time.LocalDate

/*
 * TODOS LOS DATOS DE ESTE ARCHIVO SON DE EJEMPLO.
 * Personas, instalaciones, fechas y cifras son ficticias y sirven solo para la demo.
 * Antes de un piloto hay que sustituirlas por instalaciones reales comprobadas.
 */

enum class NeedGroup(val label: String) {
    Movilidad("Para moverte"),
    Sentidos("Para ver y oír"),
    Comprension("Para entender"),
    Apoyo("Apoyo durante la actividad"),
    Sitio("Sobre el sitio"),
}

/** Lo que una persona necesita. Nunca se pregunta por un diagnóstico. */
enum class Need(val group: NeedGroup, val label: String) {
    SinEscalones(NeedGroup.Movilidad, "Acceso sin escalones"),
    Aparcamiento(NeedGroup.Movilidad, "Aparcamiento reservado"),
    Vestuario(NeedGroup.Movilidad, "Vestuario y baño adaptados"),
    Grua(NeedGroup.Movilidad, "Grúa de piscina"),
    Transporte(NeedGroup.Movilidad, "Transporte hasta allí"),
    Signos(NeedGroup.Sentidos, "Lengua de signos"),
    Bucle(NeedGroup.Sentidos, "Bucle magnético"),
    Guia(NeedGroup.Sentidos, "Guía para personas ciegas"),
    Tranquilo(NeedGroup.Sentidos, "Entorno tranquilo"),
    LecturaFacil(NeedGroup.Comprension, "Explicaciones sencillas"),
    Pictogramas(NeedGroup.Comprension, "Pictogramas"),
    GrupoReducido(NeedGroup.Comprension, "Grupo pequeño"),
    Material(NeedGroup.Apoyo, "Material adaptado"),
    Monitor(NeedGroup.Apoyo, "Monitor de apoyo"),
    Acompanante(NeedGroup.Apoyo, "Alguien que me acompañe"),
    Cubierta(NeedGroup.Sitio, "Instalación cubierta"),
}

enum class Seal(val label: String) {
    Ayuntamiento("Sello del Ayuntamiento"),
    Asociacion("Sello de una asociación"),
    Ninguno("Sin sello oficial todavía"),
}

data class Review(val author: String, val date: String, val text: String)

data class GroupMember(val name: String, val how: String)

data class Volunteer(
    val id: String,
    val name: String,
    val backedBy: String,
    val training: String,
    val sessions: Int,
)

enum class Sport(val label: String) {
    Baloncesto("Baloncesto"),
    Natacion("Natación"),
    Tenis("Tenis"),
    Padel("Pádel"),
    Golf("Golf"),
    Senderismo("Senderismo"),
    Ciclismo("Ciclismo"),
    Yoga("Yoga"),
    Futbol("Fútbol"),
    Atletismo("Atletismo"),
    Vela("Vela"),
    Boccia("Boccia"),
    Danza("Danza"),
    Fitness("Fitness"),
}

data class Activity(
    val id: String,
    val sport: Sport,
    val shortDate: String,
    val description: String,
    val title: String,
    val place: String,
    val zone: String,
    val schedule: String,
    val nextSession: String,
    val features: Set<Need>,
    val seal: Seal,
    val verifiedOn: String,
    val confirmations: Int,
    val reviews: List<Review>,
    val group: List<GroupMember>,
    val volunteers: List<Volunteer>,
    val spotsLeft: Int,
    val offPeak: Boolean,
    val howToArrive: String,
    val meetingNote: String,
    /** Nivel: «todos», «iniciacion», «intermedio» o «avanzado». */
    val level: String = "todos",
    /** Personas necesarias para que la actividad se haga (pádel: 4). */
    val minParticipants: Int = 2,
    /** «confirmada» o «propuesta» (grupo nuevo creado por la IA, pendiente de gente). */
    val status: String = "confirmada",
    /** Día de la actividad (para el calendario). */
    val startDate: LocalDate? = null,
    /** Portada: 0 = foto del deporte; 1, 2… = ilustraciones distintas. Así dos actividades del mismo deporte no se repiten. */
    val cover: Int = 0,
    /** Foto concreta (p. ej. «playa») en lugar de la del deporte. */
    val photoKey: String? = null,
)

data class Match(val activity: Activity, val covered: List<Need>, val missing: List<Need>)

fun matchActivities(needs: Set<Need>): List<Match> =
    SampleData.all
        .map { a ->
            val ordered = needs.sortedBy { it.ordinal }
            Match(a, ordered.filter { it in a.features }, ordered.filterNot { it in a.features })
        }
        .sortedWith(compareBy<Match> { it.missing.size }.thenByDescending { it.activity.confirmations })

data class Barrier(val place: String, val what: String, val reports: Int, val status: BarrierStatus)

enum class BarrierStatus(val label: String) {
    SinResolver("Sin resolver"),
    EnReparacion("En reparación"),
    Resuelta("Resuelta"),
}

data class UnmetDemand(val what: String, val requests: Int)

/** Fechas relativas a hoy, para que «hoy por la tarde» siempre tenga algo en la demo. */
object Dates {
    val today: LocalDate = LocalDate.now()
    private val wdShort = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    private val wdLong = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
    private val months = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
        "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    private fun until(day: DayOfWeek): Int = (day.value - today.dayOfWeek.value + 7) % 7
    val sat: Int get() = until(DayOfWeek.SATURDAY)
    val sun: Int get() = until(DayOfWeek.SUNDAY)

    fun date(offset: Int): LocalDate = today.plusDays(offset.toLong())

    fun short(offset: Int, time: String): String {
        val d = date(offset)
        return when (offset) {
            0 -> "Hoy · $time"
            1 -> "Mañana · $time"
            else -> "${wdShort[d.dayOfWeek.value - 1]} ${d.dayOfMonth}/${d.monthValue} · $time"
        }
    }

    fun long(offset: Int, time: String): String {
        val d = date(offset)
        val prefix = when (offset) { 0 -> "Hoy, "; 1 -> "Mañana, "; else -> "" }
        return "$prefix${wdLong[d.dayOfWeek.value - 1]} ${d.dayOfMonth} de ${months[d.monthValue - 1]}, $time"
    }

    fun weekdayName(offset: Int): String =
        wdLong[date(offset).dayOfWeek.value - 1].replaceFirstChar { it.uppercase() }
}

object SampleData {

    const val USER_NAME = "Lucía"

    private val rocio = Volunteer("v1", "Rocío", "Asociación de ejemplo", "Formación en apoyo en el agua", 14)
    private val andres = Volunteer("v2", "Andrés", "Club de natación de ejemplo", "Formación en apoyo en el agua", 6)
    private val marta = Volunteer("v3", "Marta", "Asociación de ejemplo", "Formación en guía de montaña", 9)
    private val samir = Volunteer("v4", "Samir", "Club deportivo de ejemplo", "Formación en deporte inclusivo", 21)

    val activities = listOf(
        Activity(
            id = "padel_inclusivo",
            sport = Sport.Padel,
            shortDate = Dates.short(0, "18:30"),
            startDate = Dates.date(0),
            description = "Partido de pádel mixto en pista cubierta y ancha. Se juega de pie o en silla; la red y las bolas se adaptan si hace falta.",
            title = "Pádel inclusivo, nivel intermedio",
            place = "Club de pádel cubierto",
            zone = "Zona este",
            schedule = "Hoy, 18:30",
            nextSession = Dates.long(0, "18:30"),
            features = setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Cubierta, Need.Material, Need.Monitor),
            seal = Seal.Asociacion,
            verifiedOn = "12 de noviembre de 2026",
            confirmations = 7,
            reviews = listOf(Review("Marcos", "15 de noviembre", "Pista ancha y puerta sin escalón. El vestuario adaptado está junto a la pista.")),
            group = listOf(GroupMember("Marcos", "Juega en silla, busca pareja"), GroupMember("Ana", "Nivel intermedio, viene sola")),
            volunteers = listOf(samir),
            spotsLeft = 2,
            offPeak = false,
            howToArrive = "Entrada a nivel de calle. Dos plazas reservadas en la puerta.",
            meetingNote = "Se queda en la pista 3 diez minutos antes.",
            level = "intermedio",
            minParticipants = 4,
        ),
        Activity(
            id = "padel_parque",
            sport = Sport.Padel,
            shortDate = Dates.short(1, "10:00"),
            startDate = Dates.date(1),
            description = "Primeros pasos en pádel al aire libre, con monitor y sin prisas.",
            title = "Pádel para empezar",
            place = "Pistas de pádel del parque",
            zone = "Zona norte",
            schedule = "Mañana, 10:00",
            nextSession = Dates.long(1, "10:00"),
            features = setOf(Need.SinEscalones, Need.Monitor, Need.GrupoReducido),
            seal = Seal.Ninguno,
            verifiedOn = "",
            confirmations = 2,
            reviews = emptyList(),
            group = listOf(GroupMember("Bea", "Empieza este mes"), GroupMember("Jorge y Lola", "Hermanos")),
            volunteers = emptyList(),
            spotsLeft = 1,
            offPeak = true,
            howToArrive = "Acceso a nivel desde el parque.",
            meetingNote = "Se queda junto a la pista 1.",
            level = "iniciacion",
            minParticipants = 2,
        ),
        Activity(
            id = "baloncesto",
            sport = Sport.Baloncesto,
            shortDate = Dates.short(Dates.sat, "11:00"),
            startDate = Dates.date(Dates.sat),
            description = "Partido de baloncesto en silla abierto a todo el mundo. Prestan sillas deportivas: juega quien quiera, la use a diario o no.",
            title = "Baloncesto en silla, equipo mixto",
            place = "Pabellón polideportivo",
            zone = "Zona oeste",
            schedule = "Sábados, 11:00",
            nextSession = Dates.long(Dates.sat, "11:00"),
            features = setOf(
                Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Material, Need.Signos, Need.Monitor, Need.Cubierta,
            ),
            seal = Seal.Asociacion,
            verifiedOn = "2 de noviembre de 2026",
            confirmations = 14,
            reviews = listOf(
                Review("Dani", "8 de noviembre", "Prestan sillas deportivas. Juega quien quiera, use silla a diario o no."),
            ),
            group = listOf(
                GroupMember("Dani", "Capitán del equipo"),
                GroupMember("Lola y Kike", "Hermanos, juegan juntos"),
                GroupMember("Sergio", "Intérprete de lengua de signos"),
            ),
            volunteers = listOf(samir),
            spotsLeft = 2,
            offPeak = false,
            howToArrive = "Rampa en la entrada principal. Ascensor hasta la pista.",
            meetingNote = "El equipo queda en la pista quince minutos antes.",
        ),
        Activity(
            id = "natacion",
            sport = Sport.Natacion,
            shortDate = Dates.short(1, "11:00"),
            startDate = Dates.date(1),
            description = "Natación tranquila en calle reservada. Cada uno va a su ritmo y el monitor ayuda a entrar y salir del agua.",
            title = "Natación en grupo",
            place = "Piscina cubierta municipal",
            zone = "Zona centro",
            schedule = Dates.weekdayName(1) + ", 11:00",
            nextSession = Dates.long(1, "11:00"),
            features = setOf(
                Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Grua, Need.Cubierta,
                Need.Monitor, Need.Acompanante, Need.GrupoReducido, Need.Material,
            ),
            seal = Seal.Ayuntamiento,
            verifiedOn = "28 de octubre de 2026",
            confirmations = 12,
            reviews = listOf(
                Review("Paco", "5 de noviembre", "La grúa funciona y el socorrista sabe usarla. Entré al agua en dos minutos."),
                Review("Inés", "22 de octubre", "El vestuario adaptado tiene camilla. La puerta pesa un poco."),
            ),
            group = listOf(
                GroupMember("Paco", "Viene todas las semanas"),
                GroupMember("Inés y su hija Carla", "Vienen juntas"),
                GroupMember("Tomás", "Primera vez, como tú"),
                GroupMember("Elena", "Vecina del barrio"),
            ),
            volunteers = listOf(rocio, andres),
            spotsLeft = 3,
            offPeak = true,
            howToArrive = "Entrada por la puerta lateral, a nivel de calle. Dos plazas reservadas junto a la entrada.",
            meetingNote = "El grupo queda en la entrada diez minutos antes.",
        ),
        Activity(
            id = "ciclismo",
            sport = Sport.Ciclismo,
            shortDate = Dates.short(Dates.sat, "12:00"),
            startDate = Dates.date(Dates.sat),
            description = "Paseo en bicicleta adaptada por el paseo marítimo: handbike, triciclo o tándem con guía. Ritmo tranquilo y en buena compañía.",
            title = "Ciclismo adaptado en el paseo",
            place = "Carril bici del paseo marítimo",
            zone = "Zona este",
            schedule = "Sábados, 12:00",
            nextSession = Dates.long(Dates.sat, "12:00"),
            features = setOf(
                Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Material,
                Need.Monitor, Need.Acompanante, Need.Guia,
            ),
            seal = Seal.Ayuntamiento,
            verifiedOn = "15 de octubre de 2026",
            confirmations = 9,
            reviews = listOf(
                Review("Nuria", "1 de noviembre", "Carril ancho y sin escalones. Nos prestaron una handbike y alguien nos acompañó todo el paseo."),
            ),
            group = listOf(
                GroupMember("Nuria", "Viene con su pareja"),
                GroupMember("Álvaro", "Viene con dos amigos"),
            ),
            volunteers = listOf(samir),
            spotsLeft = 4,
            offPeak = false,
            howToArrive = "Salida junto al paseo marítimo, a nivel de calle. Hay bicicletas adaptadas para prestar.",
            meetingNote = "El grupo queda en el punto de salida diez minutos antes.",
        ),
        Activity(
            id = "tenis",
            sport = Sport.Tenis,
            shortDate = Dates.short(Dates.sat, "10:00"),
            startDate = Dates.date(Dates.sat),
            description = "Tenis adaptado con monitor, en silla de ruedas o de pie. Pista ancha y pelotas de bote bajo para quien las necesite.",
            title = "Tenis para todos",
            place = "Pistas de tenis municipales",
            zone = "Zona centro",
            schedule = "Sábados, 10:00",
            nextSession = Dates.long(Dates.sat, "10:00"),
            features = setOf(Need.SinEscalones, Need.Aparcamiento, Need.Material, Need.Monitor),
            seal = Seal.Asociacion,
            verifiedOn = "20 de octubre de 2026",
            confirmations = 4,
            reviews = listOf(
                Review("Carmen", "25 de octubre", "Pista ancha y monitor atento. El baño adaptado está en otro edificio."),
            ),
            group = listOf(GroupMember("Carmen", "Juega en pareja"), GroupMember("Luis", "Busca pareja")),
            volunteers = listOf(samir),
            spotsLeft = 2,
            offPeak = true,
            howToArrive = "Acceso a nivel desde el aparcamiento.",
            meetingNote = "Se queda en la pista 2.",
        ),
        Activity(
            id = "golf",
            sport = Sport.Golf,
            shortDate = Dates.short(2, "18:00"),
            startDate = Dates.date(2),
            description = "Golf tranquilo en grupo pequeño, con buggy adaptado y sin prisas. El monitor explica cada golpe con calma.",
            title = "Golf adaptado",
            place = "Campo de golf de par 3",
            zone = "Zona norte",
            schedule = Dates.weekdayName(2) + ", 18:00",
            nextSession = Dates.long(2, "18:00"),
            features = setOf(
                Need.SinEscalones, Need.Tranquilo, Need.GrupoReducido, Need.LecturaFacil,
                Need.Material, Need.Monitor,
            ),
            seal = Seal.Ninguno,
            verifiedOn = "",
            confirmations = 3,
            reviews = listOf(
                Review("Sofía", "29 de octubre", "Poca gente y mucha calma. Prestan buggy adaptado y el monitor explica cada golpe."),
            ),
            group = listOf(GroupMember("Sofía", "Viene con su madre"), GroupMember("Hugo", "Viene solo")),
            volunteers = emptyList(),
            spotsLeft = 6,
            offPeak = true,
            howToArrive = "Acceso a nivel desde el aparcamiento. El buggy adaptado espera en la entrada.",
            meetingNote = "El monitor espera en la salida del primer hoyo diez minutos antes.",
        ),
        Activity(
            id = "senderismo",
            sport = Sport.Senderismo,
            shortDate = Dates.short(Dates.sun, "9:00"),
            startDate = Dates.date(Dates.sun),
            description = "Ruta por la sierra con silla de montaña. El grupo se turna para llevarla y todos llegan juntos al mirador.",
            title = "Senderismo con silla de montaña",
            place = "Sendero de la sierra",
            zone = "Salida en autobús",
            schedule = "Un domingo al mes, 9:00",
            nextSession = Dates.long(Dates.sun, "9:00"),
            features = setOf(Need.Material, Need.Acompanante, Need.Transporte, Need.Guia),
            seal = Seal.Asociacion,
            verifiedOn = "10 de octubre de 2026",
            confirmations = 6,
            reviews = listOf(
                Review("Julián", "12 de octubre", "Cuatro personas llevan la silla por turnos. Llegamos al mirador todos."),
            ),
            group = listOf(GroupMember("Julián", "Viene con su hijo"), GroupMember("Pilar", "Camina con guía")),
            volunteers = listOf(marta),
            spotsLeft = 2,
            offPeak = false,
            howToArrive = "Autobús adaptado desde el centro. El sendero no tiene baño.",
            meetingNote = "El autobús sale a las 9:00 en punto.",
        ),
    )

    /** Instalaciones de ejemplo (nombres genéricos). Lo que cubre cada una es lo comprobado del SITIO. */
    val facilities = listOf(
        Facility("Piscina cubierta municipal", "Zona centro",
            setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Grua, Need.Cubierta), setOf(Sport.Natacion), Seal.Ayuntamiento),
        Facility("Pabellón polideportivo", "Zona oeste",
            setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Cubierta, Need.Bucle), setOf(Sport.Baloncesto, Sport.Padel), Seal.Asociacion),
        Facility("Club de pádel cubierto", "Zona este",
            setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Cubierta), setOf(Sport.Padel), Seal.Asociacion),
        Facility("Pistas de pádel del parque", "Zona norte", setOf(Need.SinEscalones), setOf(Sport.Padel), Seal.Ninguno),
        Facility("Pistas de tenis municipales", "Zona centro",
            setOf(Need.SinEscalones, Need.Aparcamiento), setOf(Sport.Tenis), Seal.Asociacion),
        Facility("Campo de golf de par 3", "Zona norte", setOf(Need.SinEscalones, Need.Tranquilo), setOf(Sport.Golf), Seal.Ninguno),
        Facility("Carril bici del paseo marítimo", "Zona este",
            setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario), setOf(Sport.Ciclismo), Seal.Ayuntamiento),
        Facility("Sendero de la sierra", "Salida en autobús", emptySet(), setOf(Sport.Senderismo), Seal.Asociacion),
        Facility("Polideportivo municipal norte", "Zona norte",
            setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Cubierta, Need.Bucle),
            setOf(Sport.Yoga, Sport.Fitness, Sport.Danza, Sport.Boccia, Sport.Baloncesto), Seal.Ayuntamiento),
        Facility("Campo de fútbol municipal", "Zona oeste",
            setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario), setOf(Sport.Futbol, Sport.Atletismo), Seal.Asociacion),
        Facility("Pista de atletismo", "Zona centro",
            setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario), setOf(Sport.Atletismo), Seal.Ayuntamiento),
        Facility("Playa accesible", "Zona este",
            setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario), setOf(Sport.Vela, Sport.Natacion), Seal.Ayuntamiento),
    )

    /**
     * Actividades que «publica el Ayuntamiento» durante la demo (botón en Perfil). Sirven para
     * enseñar que la IA avisa solo de las que encajan contigo.
     */
    val upcoming = listOf(
        Activity(
            id = "nueva_natacion_tarde", sport = Sport.Natacion,
            shortDate = Dates.short(3, "18:00"), startDate = Dates.date(3),
            description = "Nuevo grupo de natación por la tarde, con monitor y calle reservada.",
            title = "Natación de tarde, grupo nuevo", place = "Piscina cubierta municipal", zone = "Zona centro",
            schedule = Dates.weekdayName(3) + ", 18:00", nextSession = Dates.long(3, "18:00"),
            features = setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Grua, Need.Cubierta, Need.Monitor, Need.GrupoReducido),
            seal = Seal.Ayuntamiento, verifiedOn = "20 de noviembre de 2026", confirmations = 5,
            reviews = emptyList(), group = listOf(GroupMember("Elena", "Vecina del barrio")), volunteers = listOf(rocio),
            spotsLeft = 6, offPeak = false, howToArrive = "Puerta lateral, a nivel de calle.",
            meetingNote = "El grupo queda en la entrada diez minutos antes.", minParticipants = 3,
        ),
        Activity(
            id = "nueva_padel_viernes", sport = Sport.Padel,
            shortDate = Dates.short(4, "19:00"), startDate = Dates.date(4),
            description = "Pádel mixto en pista cubierta para jugar de pie o en silla.",
            title = "Pádel cubierto de los viernes", place = "Pabellón polideportivo", zone = "Zona oeste",
            schedule = Dates.weekdayName(4) + ", 19:00", nextSession = Dates.long(4, "19:00"),
            features = setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Cubierta, Need.Material, Need.Monitor),
            seal = Seal.Asociacion, verifiedOn = "18 de noviembre de 2026", confirmations = 4,
            reviews = emptyList(), group = listOf(GroupMember("Raúl", "Nivel intermedio")), volunteers = listOf(samir),
            spotsLeft = 3, offPeak = false, howToArrive = "Rampa en la entrada principal.",
            meetingNote = "Se queda en la pista 1.", level = "intermedio", minParticipants = 4,
        ),
        Activity(
            id = "nueva_senderismo_costa", sport = Sport.Senderismo,
            shortDate = Dates.short(5, "9:30"), startDate = Dates.date(5),
            description = "Ruta por la costa con tramos de tierra y escaleras. Sin adaptar todavía.",
            title = "Ruta costera a pie", place = "Sendero de la sierra", zone = "Salida en autobús",
            schedule = Dates.weekdayName(5) + ", 9:30", nextSession = Dates.long(5, "9:30"),
            features = setOf(Need.Guia),
            seal = Seal.Ninguno, verifiedOn = "", confirmations = 1,
            reviews = emptyList(), group = listOf(GroupMember("Pilar", "Camina con guía")), volunteers = emptyList(),
            spotsLeft = 10, offPeak = false, howToArrive = "Autobús desde el centro.",
            meetingNote = "Salida a las 9:30.", level = "intermedio",
        ),
    )

    /** Atajo para declarar más actividades de ejemplo sin repetir tanto código. */
    private fun ex(
        id: String, sport: Sport, offset: Int, time: String, title: String, description: String,
        place: String, zone: String, features: Set<Need>, seal: Seal, cover: Int,
        level: String = "todos", spots: Int = 5, group: List<GroupMember> = listOf(GroupMember("Elena", "Viene cada semana")),
        min: Int = 2, confirmations: Int = 4, offPeak: Boolean = false, photoKey: String? = null,
        volunteers: List<Volunteer> = emptyList(),
    ) = Activity(
        id = id, sport = sport, shortDate = Dates.short(offset, time), startDate = Dates.date(offset),
        description = description, title = title, place = place, zone = zone,
        schedule = Dates.weekdayName(offset) + ", " + time, nextSession = Dates.long(offset, time),
        features = features, seal = seal, verifiedOn = if (seal == Seal.Ninguno) "" else "noviembre de 2026",
        confirmations = confirmations, reviews = emptyList(), group = group, volunteers = volunteers,
        spotsLeft = spots, offPeak = offPeak, howToArrive = "Acceso a nivel desde la entrada principal.",
        meetingNote = "El grupo queda en la entrada diez minutos antes.", level = level, minParticipants = min,
        cover = cover, photoKey = photoKey,
    )

    private val poli = setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Cubierta, Need.Bucle)
    private val campo = setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario)
    private val piscina = setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Grua, Need.Cubierta)

    /** Más actividades de ejemplo (varios deportes y varias por deporte, repartidas en 3 semanas). */
    val more = listOf(
        ex("yoga_adaptado", Sport.Yoga, 2, "10:00", "Yoga adaptado",
            "Yoga suave sentado o de pie, con explicaciones sencillas y mucha calma.",
            "Polideportivo municipal norte", "Zona norte", poli + setOf(Need.Monitor, Need.GrupoReducido, Need.Tranquilo, Need.LecturaFacil),
            Seal.Ayuntamiento, 0, spots = 8, offPeak = true, confirmations = 11,
            group = listOf(GroupMember("Sofía", "Viene con su madre"), GroupMember("Hugo", "Primera vez"))),
        ex("yoga_silla", Sport.Yoga, 9, "18:00", "Yoga en silla",
            "Posturas adaptadas desde la silla para ganar movilidad y relajarse.",
            "Polideportivo municipal norte", "Zona norte", poli + setOf(Need.Monitor, Need.Tranquilo, Need.Material),
            Seal.Ayuntamiento, 1, spots = 6),
        ex("futbol_inclusivo", Sport.Futbol, 6, "11:00", "Fútbol 7 inclusivo",
            "Partido mixto con reglas adaptadas: aquí juega todo el mundo.",
            "Campo de fútbol municipal", "Zona oeste", campo + setOf(Need.Material, Need.Monitor),
            Seal.Asociacion, 0, spots = 6, min = 10, confirmations = 9,
            group = List(6) { GroupMember("Jugador ${it + 1}", "Equipo mixto") }),
        ex("futbol_ciegos", Sport.Futbol, 13, "17:00", "Fútbol para personas ciegas",
            "Balón sonoro y guía en cada portería. Abierto también a quien quiera probar con antifaz.",
            "Campo de fútbol municipal", "Zona oeste", campo + setOf(Need.Guia, Need.Material, Need.Monitor),
            Seal.Asociacion, 1, spots = 4, min = 6),
        ex("atletismo_todos", Sport.Atletismo, 3, "18:30", "Atletismo para todos",
            "Carrera, lanzamientos y saltos adaptados. Hay guías para correr en pareja.",
            "Pista de atletismo", "Zona centro", campo + setOf(Need.Guia, Need.Monitor, Need.Material),
            Seal.Ayuntamiento, 0, spots = 10, confirmations = 7),
        ex("vela_adaptada", Sport.Vela, 5, "11:00", "Vela adaptada",
            "Salida en barco adaptado con monitor. Silla anfibia y pasarela hasta la orilla.",
            "Playa accesible", "Zona este", campo + setOf(Need.Material, Need.Monitor, Need.Acompanante),
            Seal.Ayuntamiento, 0, spots = 4, confirmations = 6, volunteers = listOf(samir)),
        ex("bano_asistido", Sport.Natacion, 7, "10:30", "Baño asistido en la playa",
            "Baño en el mar con silla anfibia y personal de apoyo. Sin prisas.",
            "Playa accesible", "Zona este", campo + setOf(Need.Acompanante, Need.Monitor, Need.Material),
            Seal.Ayuntamiento, 0, spots = 8, offPeak = true, photoKey = "playa", confirmations = 15),
        ex("boccia_grupo", Sport.Boccia, 4, "17:00", "Boccia en grupo",
            "Juego de precisión con bolas, ideal para empezar. Se juega sentado.",
            "Polideportivo municipal norte", "Zona norte", poli + setOf(Need.Material, Need.Monitor, Need.Tranquilo),
            Seal.Ayuntamiento, 0, spots = 6, confirmations = 5),
        ex("danza_inclusiva", Sport.Danza, 8, "19:00", "Danza inclusiva",
            "Baile en grupo para todos los cuerpos, con intérprete de lengua de signos.",
            "Polideportivo municipal norte", "Zona norte", poli + setOf(Need.Signos, Need.Monitor, Need.GrupoReducido),
            Seal.Ayuntamiento, 0, spots = 7, confirmations = 8),
        ex("gimnasio_adaptado", Sport.Fitness, 1, "9:30", "Gimnasio adaptado",
            "Circuito de fuerza con máquinas adaptadas y monitor de apoyo.",
            "Polideportivo municipal norte", "Zona norte", poli + setOf(Need.Monitor, Need.Material),
            Seal.Ayuntamiento, 0, spots = 9, offPeak = true),
        ex("natacion_tarde", Sport.Natacion, 10, "18:00", "Natación de tarde tranquila",
            "Calle reservada y grúa. Grupo pequeño para nadar a tu ritmo.",
            "Piscina cubierta municipal", "Zona centro", piscina + setOf(Need.Monitor, Need.GrupoReducido),
            Seal.Ayuntamiento, 1, spots = 5, volunteers = listOf(rocio)),
        ex("baloncesto_3x3", Sport.Baloncesto, 12, "19:00", "Baloncesto 3x3 en silla",
            "Partidos cortos y rápidos de tres contra tres. Prestan sillas deportivas.",
            "Polideportivo municipal norte", "Zona norte", poli + setOf(Need.Material, Need.Monitor),
            Seal.Ayuntamiento, 1, spots = 4, min = 6, level = "intermedio"),
        ex("padel_silla_inicio", Sport.Padel, 15, "18:00", "Pádel en silla: iniciación",
            "Clase para empezar en pádel en silla, con pelotas de bote doble.",
            "Club de pádel cubierto", "Zona este", setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Cubierta, Need.Monitor, Need.Material),
            Seal.Asociacion, 1, spots = 3, min = 4, level = "iniciacion"),
        ex("tenis_silla", Sport.Tenis, 11, "10:00", "Tenis en silla: entrenamiento",
            "Entrenamiento técnico en pista ancha con monitor.",
            "Pistas de tenis municipales", "Zona centro", setOf(Need.SinEscalones, Need.Aparcamiento, Need.Monitor, Need.Material),
            Seal.Asociacion, 1, spots = 4, level = "intermedio", offPeak = true),
        ex("paseo_accesible", Sport.Senderismo, 20, "9:30", "Paseo accesible por la costa",
            "Ruta llana y asfaltada junto al mar, apta para sillas y carritos.",
            "Carril bici del paseo marítimo", "Zona este", setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Acompanante),
            Seal.Ayuntamiento, 1, spots = 12, confirmations = 10),
        ex("ciclismo_tandem", Sport.Ciclismo, 17, "11:00", "Tándem con guía",
            "Paseo en tándem para personas ciegas o con baja visión, con guía.",
            "Carril bici del paseo marítimo", "Zona este", setOf(Need.SinEscalones, Need.Aparcamiento, Need.Vestuario, Need.Guia, Need.Material),
            Seal.Ayuntamiento, 1, spots = 4),
    )

    /** Actividades que crea la persona usuaria durante la demo (solo en memoria). */
    val created = androidx.compose.runtime.mutableStateListOf<Activity>()

    /** Las creadas o publicadas primero, luego las de ejemplo (sin duplicar ids). */
    val all: List<Activity>
        get() = (created + activities + more).distinctBy { it.id }

    /** Todo lo que la app conoce, publicado o no (para interpretar el historial). */
    val allKnown: List<Activity> get() = (all + upcoming).distinctBy { it.id }

    fun activity(id: String): Activity = all.first { it.id == id }

    fun activityOrNull(id: String): Activity? = all.firstOrNull { it.id == id }

    /** Eventos a los que Lucía ya fue (ejemplo), para el apartado «Recientes». */
    val recentEvents = listOf(
        "golf" to "Hace dos semanas",
        "tenis" to "Hace tres semanas",
    )

    val barrierOptions = listOf(
        "Ascensor o grúa averiados",
        "Plaza reservada ocupada",
        "Escalón o rampa en mal estado",
        "Baño adaptado cerrado",
    )

    val barriers = listOf(
        Barrier("Pabellón polideportivo", "Ascensor averiado", 6, BarrierStatus.EnReparacion),
        Barrier("Pistas de tenis municipales", "Baño adaptado en otro edificio", 4, BarrierStatus.SinResolver),
        Barrier("Carril bici del paseo marítimo", "Bordillo sin rebajar en un cruce", 3, BarrierStatus.Resuelta),
    )

    val unmetDemand = listOf(
        UnmetDemand("Lengua de signos en natación", 7),
        UnmetDemand("Transporte hasta la piscina", 5),
        UnmetDemand("Actividades por la tarde en zona norte", 4),
        UnmetDemand("Guía para personas ciegas en golf", 2),
    )

    /** Ocupación (%) por hora, de 8:00 a 21:00, antes y con la app. */
    val hours = (8..21).toList()
    val occupancyBefore = listOf(35, 22, 15, 12, 14, 18, 20, 16, 30, 62, 85, 92, 88, 60)
    val occupancyNow = listOf(38, 34, 41, 47, 39, 30, 26, 29, 41, 66, 86, 92, 88, 61)

    const val NEW_PARTICIPANTS = 38
    const val SEALED_PLACES = 9
    const val TOTAL_PLACES = 14
    const val BACKED_VOLUNTEERS = 23
}
