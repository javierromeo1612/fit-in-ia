package com.marbellup.fitin.ai.propio

import java.text.Normalizer

/**
 * Cómo ve el texto el modelo propio de «Cuéntanoslo».
 *
 * Kotlin puro (sin Android): lo usan igual el entrenamiento (en el PC) y la app (en el
 * móvil), así el modelo ve el texto exactamente igual en los dos sitios.
 *
 * Rasgos de cada trozo de texto:
 * - palabras sueltas («cansa»),
 * - parejas de palabras seguidas («va_en», «no_oye»): capturan expresiones y negaciones,
 * - trocitos de 3 a 5 letras de cada palabra («can», «ansa»…): así «cansa», «cansada» y
 *   «cansancio» se parecen, y aguanta faltas de ortografía.
 * Cada rasgo se convierte en un número con un hash (no hace falta guardar un diccionario).
 */
object Rasgos {
    const val BITS = 18
    const val DIM = 1 shl BITS

    private val NEGACIONES = setOf("no", "ni", "nunca", "tampoco", "jamas")

    /** Minúsculas, sin tildes y solo letras y números. «¡Va en SILLA!» → «va en silla». */
    fun normalizar(texto: String): String {
        val sinTildes = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return sinTildes.replace(Regex("[^a-z0-9]+"), " ").trim()
    }

    fun palabras(texto: String): List<String> =
        normalizar(texto).split(' ').filter { it.isNotEmpty() }

    /** Índices (sin repetir) de los rasgos presentes en estas palabras. */
    fun extraer(palabras: List<String>): IntArray {
        val salida = HashSet<Int>(palabras.size * 8)
        // Negación: las dos palabras que siguen a «no», «ni», «nunca» o «tampoco» llevan además
        // un rasgo propio («n:silla»), para que «no usa silla» no se parezca a «usa silla».
        var negadas = 0
        for ((i, p) in palabras.withIndex()) {
            if (negadas > 0) {
                salida += hash("n:$p")
                negadas--
            }
            if (p in NEGACIONES) negadas = 2
            salida += hash("w:$p")
            if (i + 1 < palabras.size) salida += hash("b:${p}_${palabras[i + 1]}")
            if (p.length >= 4) {
                val conBordes = "<$p>"
                for (n in 3..5) {
                    for (j in 0..conBordes.length - n) salida += hash("c:" + conBordes.substring(j, j + n))
                }
            }
        }
        salida += hash("sesgo")
        return salida.toIntArray()
    }

    /** FNV-1a de 32 bits, recortado a [BITS]. Determinista en cualquier JVM o Android. */
    fun hash(s: String): Int {
        var h = 0x811c9dc5.toInt()
        for (c in s) {
            h = h xor c.code
            h *= 0x01000193
        }
        return (h ushr (32 - BITS)) and (DIM - 1)
    }

    /* ---------- Trozos del relato ---------- */

    /**
     * Un trozo del relato: [texto] tal como lo dijo la persona (para citarlo en «porque dijiste…»)
     * y sus [palabras] normalizadas.
     */
    data class Trozo(val texto: String, val palabras: List<String>)

    private val separadores = Regex("[.;:!?¡¿\\n]+|,|\\s+(?:y|e|pero|aunque|porque|pues|ya que|así que|asi que)\\s+", RegexOption.IGNORE_CASE)

    /** Frases y subfrases del relato (se corta por puntos, comas y conectores como «y» o «pero»). */
    fun trozos(texto: String): List<Trozo> =
        texto.split(separadores)
            .map { it.trim().trim('"', '«', '»', '(', ')', '-') }
            .filter { it.isNotBlank() }
            .map { Trozo(it, palabras(it)) }
            .filter { it.palabras.isNotEmpty() }

    /**
     * Ventanas que evalúa el modelo: cada trozo, cada pareja de trozos seguidos (por si la
     * idea queda partida) y el relato entero si es corto. Para cada necesidad se queda con
     * la ventana que más la indica; así un relato largo con muchas cosas no diluye ninguna.
     */
    fun ventanas(texto: String): List<Trozo> {
        val t = trozos(texto)
        val salida = ArrayList<Trozo>(t.size * 4 + 1)
        salida += t
        // Ventanas cortas (5 palabras, avanzando de 2 en 2) dentro de los trozos largos: en
        // «querría jugar los sábados por la mañana» la franja no se diluye entre el resto.
        for (tr in t) {
            if (tr.palabras.size <= 4) continue
            val originales = tr.texto.split(Regex("\\s+"))
            var i = 0
            while (i < tr.palabras.size - 2) {
                val fin = minOf(i + 5, tr.palabras.size)
                // Cita aproximada con las palabras originales (mismo recuento si no hay signos raros).
                val cita = if (originales.size == tr.palabras.size) originales.subList(i, fin).joinToString(" ") else tr.texto
                salida += Trozo(cita, tr.palabras.subList(i, fin))
                if (fin == tr.palabras.size) break
                i += 2
            }
        }
        for (i in 0 until t.size - 1) {
            salida += Trozo("${t[i].texto}, ${t[i + 1].texto}", t[i].palabras + t[i + 1].palabras)
        }
        val todas = palabras(texto)
        if (t.size > 2 && todas.size <= 30) salida += Trozo(texto.trim(), todas)
        return salida
    }
}
