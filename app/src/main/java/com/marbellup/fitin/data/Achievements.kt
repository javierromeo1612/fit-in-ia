package com.marbellup.fitin.data

/** Una medalla de «Mis logros», con su progreso. */
data class Medal(
    val emoji: String,
    val title: String,
    val description: String,
    val current: Int,
    val target: Int,
) {
    val unlocked: Boolean get() = current >= target
    val progress: Float get() = (current.toFloat() / target).coerceIn(0f, 1f)
}

data class Stats(
    val activities: Int,
    val sports: Int,
    val hours: Int,
    val ratings: Int,
    val favorites: Int,
    val sportsTried: List<Sport>,
    val medals: List<Medal>,
) {
    val unlocked: Int get() = medals.count { it.unlocked }
}

object Achievements {

    /**
     * Calcula estadísticas y medallas a partir de lo que ha hecho la persona: actividades en las
     * que se ha apuntado (y las que ya hizo antes), deportes distintos, valoraciones y favoritos.
     */
    fun compute(joined: Collection<String>, history: List<Interaction>, favorites: Collection<String>): Stats {
        val catalog = SampleData.allKnown.associateBy { it.id }
        // Actividades ya hechas antes de la demo (ejemplo) + en las que se ha apuntado.
        val done = (SampleData.recentEvents.map { it.first } + joined).distinct().mapNotNull { catalog[it] }
        val sports = done.map { it.sport }.distinct()
        val hours = done.sumOf { if (it.sport == Sport.Senderismo) 4 else if (it.sport == Sport.Golf) 2 else 1 }
        val ratings = history.count { it.signal == Signal.RateUp || it.signal == Signal.RateDown }
        val teach = history.size
        val morning = done.count { Recommender.slotOf(it) == Slot.Manana }
        val water = done.count { it.sport == Sport.Natacion || it.sport == Sport.Vela }
        val team = joined.mapNotNull { catalog[it] }.count { it.group.size < it.minParticipants || it.status == "propuesta" }

        val medals = listOf(
            Medal("🥇", "Primer paso", "Apúntate a tu primera actividad con Fit-In", joined.size, 1),
            Medal("🧭", "Exploradora", "Prueba 3 deportes distintos", sports.size, 3),
            Medal("🌈", "Todoterreno", "Prueba 6 deportes distintos", sports.size, 6),
            Medal("🔥", "Constante", "Participa en 5 actividades", done.size, 5),
            Medal("🌅", "Madrugadora", "Haz una actividad por la mañana", morning, 1),
            Medal("🌊", "Al agua", "Haz una actividad acuática", water, 1),
            Medal("🤝", "En equipo", "Únete a un grupo al que le faltaba gente", team, 1),
            Medal("⭐", "Opinadora", "Valora 3 actividades en el calendario", ratings, 3),
            Medal("❤️", "Fan", "Guarda 5 actividades en favoritos", favorites.size, 5),
            Medal("🤖", "Entrena a tu IA", "Haz 10 acciones de las que aprende la IA", teach, 10),
        )
        return Stats(done.size, sports.size, hours, ratings, favorites.size, sports, medals)
    }
}
