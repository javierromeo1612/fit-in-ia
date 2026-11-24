package com.marbellup.fitin.ai.propio

import com.marbellup.fitin.ai.PerfilIA
import com.marbellup.fitin.data.Need
import com.marbellup.fitin.data.Sport
import java.io.InputStream

/**
 * «Cuéntanoslo» con el modelo propio: convierte lo que detecta [ModeloPropio] en el
 * [PerfilIA.Perfil] que enseña la pantalla. Todo ocurre en el móvil, sin red.
 *
 * - Necesidad muy probable → se propone ya marcada, con la cita que la indica.
 * - Necesidad dudosa (entre [PREGUNTA] y su umbral) → se convierte en una pregunta de un toque.
 *   Así el modelo reconoce cuándo no está seguro y la persona decide (principio 5).
 * - El nombre y el resumen se sacan con reglas sencillas: no hace falta IA para eso.
 */
object CuentanosloPropio {

    /** Por debajo del umbral pero por encima de esto, se pregunta en vez de proponer. */
    const val PREGUNTA = 0.35f
    private const val MAX_PREGUNTAS = 2

    @Volatile private var modelo: ModeloPropio? = null
    private var abrir: (() -> InputStream)? = null

    /** La app indica de dónde leer el archivo del modelo (assets). Se carga la primera vez que se usa. */
    fun iniciar(abrirArchivo: () -> InputStream) {
        abrir = abrirArchivo
    }

    private fun modelo(): ModeloPropio = modelo ?: synchronized(this) {
        modelo ?: (abrir ?: error("Modelo propio sin iniciar")).invoke().use { ModeloPropio.cargar(it) }.also { modelo = it }
    }

    fun interpretar(texto: String): PerfilIA.Perfil {
        val m = modelo()
        val det = m.analizar(texto).associateBy { it.etiqueta }
        fun p(e: String) = det[e]?.probabilidad ?: 0f

        val necesidades = Need.entries.mapNotNull { n -> det[n.name]?.let { n to it } }
        val propuestas = necesidades
            .filter { (n, d) -> d.probabilidad >= m.umbral(n.name) }
            .sortedByDescending { it.second.probabilidad }
        val familiar = p("FAMILIAR") >= 0.5f
        val nombre = nombre(texto, familiar)

        val dudas = necesidades
            .filter { (n, d) -> d.probabilidad >= PREGUNTA && d.probabilidad < m.umbral(n.name) }
            .sortedByDescending { it.second.probabilidad }
            .take(MAX_PREGUNTAS)
        val quien = nombre.ifBlank { "esta persona" }
        val preguntas = dudas.map { (n, _) ->
            PerfilIA.Pregunta(
                texto = if (familiar) "¿A $quien le vendría bien: ${minuscula(n.label)}?" else "¿Te vendría bien: ${minuscula(n.label)}?",
                opciones = listOf(PerfilIA.Opcion("Sí", listOf(n)), PerfilIA.Opcion("No", emptyList())),
            )
        }

        val deportes = Sport.entries.filter { p("D_${it.name}") >= 0.5f }
        val franja = listOf(
            PerfilIA.Franja.Manana to p("F_Manana"),
            PerfilIA.Franja.Tarde to p("F_Tarde"),
            PerfilIA.Franja.Noche to p("F_Noche"),
        ).filter { it.second >= 0.5f }.maxByOrNull { it.second }?.first ?: PerfilIA.Franja.Indiferente

        val sugerencias = propuestas.map { (n, d) -> PerfilIA.Sugerencia(n, d.evidencia) }
        return PerfilIA.Perfil(
            nombre = nombre,
            gestionaFamiliar = familiar,
            sugerencias = sugerencias,
            deportes = deportes,
            franja = franja,
            preguntas = preguntas,
            resumen = resumen(nombre, familiar, deportes, franja, sugerencias.map { it.need }),
        )
    }

    /* ---------- Nombre (reglas) ---------- */

    private const val MAYUS = "[A-ZÁÉÍÓÚÑ][a-záéíóúñü]+"
    private const val PARENTESCO = "hij[oa]|herman[oa]|sobrin[oa]|niet[oa]|madre|padre|abuel[oa]|marido|mujer|pareja|niñ[oa]|prim[oa]|tí[oa]|novi[oa]"
    private val NO_SON_NOMBRES = setOf("Tiene", "Es", "Le", "Me", "Va", "Que", "Y", "Pero", "Usa", "Lleva", "Hola", "Desde", "No", "Se", "Está")

    /**
     * «la madre de Lucía» → Lucía; «mi hija Lucía» → Lucía; «soy Pedro» / «me llamo Pedro» → Pedro
     * (este último solo si habla de sí misma: si habla un familiar, ese nombre es el suyo).
     */
    fun nombre(texto: String, familiar: Boolean): String {
        val patrones = buildList {
            add(Regex("(?:madre|padre|cuidador|cuidadora|abuel[oa]|herman[oa]|tí[oa]|pareja) de ($MAYUS)"))
            add(Regex("(?:^|\\b)(?:mi|a mi|de mi|para mi) (?:$PARENTESCO),? ($MAYUS)", RegexOption.IGNORE_CASE))
            if (!familiar) add(Regex("(?:\\bsoy|me llamo|mi nombre es) ($MAYUS)", RegexOption.IGNORE_CASE))
        }
        for (r in patrones) {
            for (m in r.findAll(texto)) {
                val candidato = m.groupValues[1]
                if (candidato.first().isUpperCase() && candidato !in NO_SON_NOMBRES) return candidato
            }
        }
        return ""
    }

    /* ---------- Resumen (plantilla) ---------- */

    private fun resumen(nombre: String, familiar: Boolean, deportes: List<Sport>, franja: PerfilIA.Franja, needs: List<Need>): String {
        if (needs.isEmpty() && deportes.isEmpty()) return ""
        val que = if (deportes.isEmpty()) "hacer deporte" else "practicar " + deportes.joinToString(" o ") { minuscula(it.label) }
        val cuando = when (franja) {
            PerfilIA.Franja.Manana -> " por la mañana"
            PerfilIA.Franja.Tarde -> " por la tarde"
            PerfilIA.Franja.Noche -> " por la noche"
            PerfilIA.Franja.Indiferente -> ""
        }
        // Solo etiquetas sin pronombre («Alguien que me acompañe» quedaría raro en tercera persona).
        val con = needs.filterNot { Regex("\\b(me|te|mi)\\b", RegexOption.IGNORE_CASE).containsMatchIn(it.label) }
            .take(2).map { minuscula(it.label) }.let { l ->
            when (l.size) {
                0 -> ""
                1 -> ", con ${l[0]}"
                else -> ", con ${l[0]} y ${l[1]}"
            }
        }
        val sujeto = when {
            familiar -> "${nombre.ifBlank { "Esta persona" }} busca"
            nombre.isNotBlank() -> "$nombre, buscas"
            else -> "Buscas"
        }
        return "$sujeto $que$cuando$con."
    }

    private fun minuscula(s: String) = s.replaceFirstChar { it.lowercase() }
}
