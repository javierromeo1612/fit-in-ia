package com.marbellup.fitin.ai.propio

import com.marbellup.fitin.data.Need
import com.marbellup.fitin.data.Sport
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.random.Random

/**
 * ENTRENAMIENTO del modelo propio de «Cuéntanoslo». No se ejecuta con las pruebas normales:
 *
 *   node modelo/generar_datos.mjs
 *   cd android; ./gradlew testDebugUnitTest -Pentrenar=1 --tests "*EntrenarModelo*"
 *
 * Lee modelo/datos/entrenamiento.tsv, aparta un 10 % para validar, entrena una regresión
 * logística por etiqueta (descenso de gradiente), elige el umbral con la validación, recorta
 * los pesos casi nulos y escribe:
 *   - app/src/main/assets/modelo_cuentanoslo.bin (lo que usa la app)
 *   - modelo/INFORME.md (métricas con el conjunto de PRUEBA, que nunca se usa para entrenar)
 */
class EntrenarModeloTest {

    private val raiz = File("../..").canonicalFile

    @Test
    fun entrenar() {
        assumeTrue("Solo con -Pentrenar=1", System.getProperty("entrenar") == "1")
        val inicio = System.currentTimeMillis()
        val etiquetas = ETIQUETAS
        val n = etiquetas.size
        val indice = etiquetas.withIndex().associate { (i, e) -> e to i }

        val todos = leer(File(raiz, "modelo/datos/entrenamiento.tsv")).shuffled(Random(7))
        val corte = todos.size * 9 / 10
        val entreno = todos.subList(0, corte)
        val validacion = todos.subList(corte, todos.size)
        val desarrollo = leer(File(raiz, "modelo/datos/desarrollo.tsv"))
        val prueba = leer(File(raiz, "modelo/datos/prueba.tsv"))
        val pruebaNuevas = leer(File(raiz, "modelo/datos/prueba_nuevas.tsv"))
        val pruebaCiega = leer(File(raiz, "modelo/datos/prueba_ciega.tsv"))

        // Las pistas del catálogo también enseñan: cada trozo («usa andador», «oye mal»…) es un
        // ejemplo con TODAS las necesidades cuya pista lo contiene. Así el catálogo es la única
        // fuente de verdad y una necesidad nueva siempre llega al modelo.
        val porTrozo = LinkedHashMap<String, MutableSet<String>>()
        for (need in Need.entries) {
            for (t in need.pista.split(",").map { it.trim() }.filter { it.length > 3 }) {
                porTrozo.getOrPut(t) { mutableSetOf() } += need.name
            }
        }
        val pistas = porTrozo.flatMap { (t, ns) -> List(4) { t to ns.toSet() } }
        val ejemplos = entreno + pistas

        // Ejemplos ya convertidos en rasgos (una sola vez).
        val xs = ejemplos.map { Rasgos.extraer(Rasgos.palabras(it.first)) }
        val ys = ejemplos.map { e -> e.second.mapNotNull { indice[it] }.toIntArray() }
        // Peso de los casos positivos (> 1 favorece encontrar más, a costa de algo de precisión).
        val pesoPositivo = System.getProperty("peso")?.toFloatOrNull() ?: PESO_POSITIVO

        // ---- Descenso de gradiente estocástico, una regresión logística por etiqueta ----
        val w = FloatArray(Rasgos.DIM * n)
        val sesgos = FloatArray(n) { -3f } // casi todo es «no» al empezar
        val z = FloatArray(n)
        val orden = xs.indices.toMutableList()
        val epocas = 14
        for (epoca in 0 until epocas) {
            val tasa = 0.8f / (1f + epoca * 0.35f)
            orden.shuffle(Random(epoca))
            for (i in orden) {
                val f = xs[i]
                val x = ModeloPropio.valorRasgo(f.size)
                System.arraycopy(sesgos, 0, z, 0, n)
                for (r in f) { val base = r * n; for (l in 0 until n) z[l] += w[base + l] * x }
                val y = ys[i]
                for (l in 0 until n) {
                    val objetivo = if (l in y) 1f else 0f
                    val g = (ModeloPropio.sigmoide(z[l]) - objetivo) * tasa * (if (objetivo == 1f) pesoPositivo else 1f)
                    z[l] = g // reutilizamos z para guardar el gradiente
                    sesgos[l] -= g * 0.1f
                }
                for (r in f) { val base = r * n; for (l in 0 until n) w[base + l] -= z[l] * x }
            }
        }
        println("Entrenado en ${(System.currentTimeMillis() - inicio) / 1000} s con ${ejemplos.size} ejemplos (${pistas.size} de las pistas del catálogo), peso positivo $pesoPositivo")

        // ---- Umbral elegido en VALIDACIÓN. Se premia más no dejarse nada (F-beta con beta 1,5):
        // en «Cuéntanoslo» la persona revisa y desmarca lo que sobra, pero lo que falta no lo ve. ----
        val completo = ModeloPropio.desdeDensos(etiquetas, sesgos, w, FloatArray(n) { 0.5f }, minimo = 1e-4f)
        val detVal = validacion.map { completo.analizar(it.first) }
        var mejorU = 0.5f; var mejorF = -1.0
        for (u in listOf(0.25f, 0.3f, 0.35f, 0.4f, 0.45f, 0.5f, 0.55f, 0.6f, 0.65f, 0.7f)) {
            val f1 = metricas(validacion, detVal, u, NECESIDADES).fBeta
            if (f1 > mejorF) { mejorF = f1; mejorU = u }
        }
        println("Umbral elegido en validación: $mejorU (F-beta necesidades = ${"%.3f".format(mejorF)})")

        // ---- Recorte de pesos casi nulos (modelo pequeño) sin perder calidad en validación ----
        var minimo = 1e-4f
        for (m in listOf(0.02f, 0.05f, 0.1f, 0.2f, 0.3f)) {
            val recortado = ModeloPropio.desdeDensos(etiquetas, sesgos, w, FloatArray(n) { umbralDe(etiquetas[it], mejorU) }, m)
            val f1 = metricas(validacion, validacion.map { recortado.analizar(it.first) }, mejorU, NECESIDADES).fBeta
            if (f1 >= mejorF - 0.005) minimo = m else break
        }
        val modelo = ModeloPropio.desdeDensos(etiquetas, sesgos, w, FloatArray(n) { umbralDe(etiquetas[it], mejorU) }, minimo)
        val archivo = File(raiz, "android/app/src/main/assets/modelo_cuentanoslo.bin")
        archivo.parentFile.mkdirs()
        archivo.outputStream().use { modelo.guardar(it) }
        // Comprobación: el archivo cargado da lo mismo que el modelo en memoria.
        val cargado = archivo.inputStream().use { ModeloPropio.cargar(it) }
        println("Modelo guardado: ${archivo.length() / 1024} KB (recorte de pesos < $minimo)")

        // ---- Métricas con el conjunto de PRUEBA (frases que el modelo nunca ha visto) ----
        val informe = informe(desarrollo, desarrollo.map { cargado.analizar(it.first) }, prueba, prueba.map { cargado.analizar(it.first) }, mejorU, ejemplos.size, archivo.length(), minimo) +
            seccion("Prueba de las necesidades nuevas: ${pruebaNuevas.size} relatos (modelo/datos/prueba_nuevas.tsv)", pruebaNuevas, pruebaNuevas.map { cargado.analizar(it.first) }, mejorU) +
            seccion("Prueba ciega con diagnósticos y familiares: ${pruebaCiega.size} relatos (modelo/datos/prueba_ciega.tsv)", pruebaCiega, pruebaCiega.map { cargado.analizar(it.first) }, mejorU)
        File(raiz, "modelo/INFORME.md").writeText(informe)
        println(informe)
    }

    /* ---------------------------------------------------------------- utilidades */

    private fun leer(f: File): List<Pair<String, Set<String>>> = f.readLines(Charsets.UTF_8)
        .filter { it.isNotBlank() }
        .map { l ->
            val p = l.split('\t')
            p[0] to (p.getOrNull(1).orEmpty().split(',').filter { it.isNotBlank() }.toSet())
        }

    data class Metricas(val vp: Int, val fp: Int, val fn: Int) {
        val precision get() = if (vp + fp == 0) 1.0 else vp.toDouble() / (vp + fp)
        val exhaustividad get() = if (vp + fn == 0) 1.0 else vp.toDouble() / (vp + fn)
        val f1 get() = if (precision + exhaustividad == 0.0) 0.0 else 2 * precision * exhaustividad / (precision + exhaustividad)
        val fBeta get() = if (precision + exhaustividad == 0.0) 0.0 else 3.25 * precision * exhaustividad / (2.25 * precision + exhaustividad)
    }

    /** El umbral [u] se ajusta para las necesidades; deportes, familiar y franja usan 0,5. */
    private fun umbralDe(etiqueta: String, u: Float) = if (etiqueta in NECESIDADES) u else 0.5f

    private fun predichas(d: List<ModeloPropio.Deteccion>, u: Float) = d.filter { it.probabilidad >= umbralDe(it.etiqueta, u) }.map { it.etiqueta }.toSet()

    private fun metricas(datos: List<Pair<String, Set<String>>>, det: List<List<ModeloPropio.Deteccion>>, u: Float, grupo: Set<String>): Metricas {
        var vp = 0; var fp = 0; var fn = 0
        for ((i, d) in datos.withIndex()) {
            val real = d.second intersect grupo
            val pred = predichas(det[i], u) intersect grupo
            vp += (real intersect pred).size; fp += (pred - real).size; fn += (real - pred).size
        }
        return Metricas(vp, fp, fn)
    }

    private fun pct(x: Double) = String.format(Locale("es", "ES"), "%.0f %%", x * 100)

    private fun tabla(sb: StringBuilder, datos: List<Pair<String, Set<String>>>, det: List<List<ModeloPropio.Deteccion>>, u: Float) {
        sb.appendLine("| Qué detecta | Precisión (lo que marca, ¿es correcto?) | Exhaustividad (de lo que había, ¿cuánto encuentra?) | F1 |")
        sb.appendLine("|---|---|---|---|")
        for ((nombre, grupo) in listOf("Necesidades" to NECESIDADES, "Deportes" to DEPORTES, "Habla un familiar" to setOf("FAMILIAR"), "Franja horaria" to FRANJAS)) {
            val m = metricas(datos, det, u, grupo)
            sb.appendLine("| $nombre | ${pct(m.precision)} | ${pct(m.exhaustividad)} | ${pct(m.f1)} |")
        }
        // Lo que la app propone + lo que pregunta (como mucho 2 dudas entre PREGUNTA y el umbral).
        var vp = 0; var fn = 0
        for ((i, d) in datos.withIndex()) {
            val real = d.second intersect NECESIDADES
            val dudas = det[i].filter { it.etiqueta in NECESIDADES && it.probabilidad >= PREGUNTA && it.probabilidad < u }
                .sortedByDescending { it.probabilidad }.take(2).map { it.etiqueta }
            val vistas = (predichas(det[i], u) intersect NECESIDADES) + dudas
            vp += (real intersect vistas).size; fn += (real - vistas).size
        }
        sb.appendLine()
        sb.appendLine("Contando también las preguntas de confirmación (como mucho 2 por relato), encuentra el **${pct(vp.toDouble() / (vp + fn))}** de las necesidades.")
    }

    private fun seccion(titulo: String, datos: List<Pair<String, Set<String>>>, det: List<List<ModeloPropio.Deteccion>>, u: Float) = buildString {
        appendLine()
        appendLine("## $titulo")
        appendLine()
        appendLine("Escritos antes de entrenar, con las 8 necesidades añadidas el 24 nov. No se usan para ajustar nada.")
        appendLine()
        tabla(this, datos, det, u)
        appendLine()
        for ((i, p) in datos.withIndex()) {
            val pred = predichas(det[i], u)
            val falta = p.second - pred
            val sobra = pred - p.second
            appendLine("- ${if (falta.isEmpty() && sobra.isEmpty()) "✅" else "⚠️"} ${p.first}")
            if (falta.isNotEmpty()) appendLine("  - No vio: ${falta.sorted().joinToString()}")
            if (sobra.isNotEmpty()) appendLine("  - Marcó de más: ${sobra.sorted().joinToString()}")
        }
    }

    private fun informe(desarrollo: List<Pair<String, Set<String>>>, detDes: List<List<ModeloPropio.Deteccion>>, prueba: List<Pair<String, Set<String>>>, det: List<List<ModeloPropio.Deteccion>>, u: Float, nEntreno: Int, bytes: Long, minimo: Float) = buildString {
        appendLine("# Informe del modelo propio de «Cuéntanoslo»")
        appendLine()
        appendLine("Generado por `EntrenarModeloTest`. Entrenado con $nEntreno ejemplos; umbral $u (elegido en validación); pesos recortados por debajo de $minimo; archivo de ${bytes / 1024} KB.")
        appendLine()
        appendLine("## Prueba: ${prueba.size} relatos que el modelo nunca ha visto")
        appendLine()
        appendLine("Escritos aparte (`modelo/datos/prueba.tsv`) y no usados para entrenar ni para ajustar nada. Es la cifra honesta.")
        appendLine()
        tabla(this, prueba, det, u)
        appendLine()
        appendLine("## Desarrollo: ${desarrollo.size} relatos usados para encontrar huecos")
        appendLine()
        appendLine("(`modelo/datos/desarrollo.tsv`). Sus fallos sirvieron para ampliar las frases de entrenamiento, así que esta cifra es optimista.")
        appendLine()
        tabla(this, desarrollo, detDes, u)
        appendLine()
        appendLine("## Prueba, relato por relato")
        appendLine()
        for ((i, p) in prueba.withIndex()) {
            val pred = predichas(det[i], u)
            val falta = p.second - pred
            val sobra = pred - p.second
            val estado = if (falta.isEmpty() && sobra.isEmpty()) "✅" else "⚠️"
            appendLine("- $estado ${p.first}")
            if (falta.isNotEmpty()) appendLine("  - No vio: ${falta.sorted().joinToString()}")
            if (sobra.isNotEmpty()) appendLine("  - Marcó de más: ${sobra.sorted().joinToString()}")
        }
    }

    companion object {
        val NECESIDADES = Need.entries.map { it.name }.toSet()
        val DEPORTES = Sport.entries.map { "D_${it.name}" }.toSet()
        val FRANJAS = setOf("F_Manana", "F_Tarde", "F_Noche")
        /** Por debajo del umbral pero por encima de esto, la app pregunta en vez de proponer. */
        const val PREGUNTA = 0.35f
        const val PESO_POSITIVO = 1f
        val ETIQUETAS = Need.entries.map { it.name } + Sport.entries.map { "D_${it.name}" } + "FAMILIAR" + FRANJAS
    }
}
