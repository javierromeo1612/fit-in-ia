package com.marbellup.fitin.ai.propio

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.exp

/**
 * Modelo propio de «Cuéntanoslo»: clasificador multietiqueta entrenado por el equipo
 * (ver `modelo/README.md`). Funciona dentro del móvil, sin internet y sin enviar nada.
 *
 * Es una regresión logística por etiqueta sobre los [Rasgos] del texto: para cada etiqueta
 * (una necesidad, un deporte, si habla un familiar, la franja horaria) suma los pesos de los
 * rasgos presentes y lo convierte en una probabilidad entre 0 y 1.
 *
 * Además de la probabilidad devuelve la EVIDENCIA: el trozo del relato que más la indica,
 * para enseñar «porque dijiste «…»» y que la persona pueda comprobarlo.
 */
class ModeloPropio(
    val etiquetas: List<String>,
    private val sesgos: FloatArray,
    /** Rasgos con peso, ordenados; [inicio] dice dónde empiezan los pesos de cada uno. */
    private val rasgos: IntArray,
    private val inicio: IntArray,
    private val etiquetaDePeso: ShortArray,
    private val pesos: FloatArray,
    /** Umbral de cada etiqueta: por encima, se propone; un poco por debajo, se pregunta. */
    val umbrales: FloatArray,
) {
    data class Deteccion(val etiqueta: String, val probabilidad: Float, val evidencia: String)

    private val indice = etiquetas.withIndex().associate { (i, e) -> e to i }

    fun umbral(etiqueta: String): Float = indice[etiqueta]?.let { umbrales[it] } ?: 0.5f

    /** Probabilidad de cada etiqueta para un trozo de texto ya convertido en palabras. */
    fun probabilidades(palabras: List<String>): FloatArray {
        val z = sesgos.copyOf()
        val presentes = Rasgos.extraer(palabras)
        // Cada rasgo pesa 1/√n: un trozo largo no suma más solo por tener más palabras.
        val x = valorRasgo(presentes.size)
        for (r in presentes) {
            val pos = rasgos.binarySearch(r)
            if (pos < 0) continue
            for (k in inicio[pos] until inicio[pos + 1]) z[etiquetaDePeso[k].toInt()] += pesos[k] * x
        }
        return FloatArray(z.size) { sigmoide(z[it]) }
    }

    /** Analiza un relato: para cada etiqueta, la mejor ventana y su probabilidad. */
    fun analizar(texto: String): List<Deteccion> {
        val ventanas = Rasgos.ventanas(texto)
        if (ventanas.isEmpty()) return emptyList()
        val mejor = FloatArray(etiquetas.size)
        val donde = arrayOfNulls<Rasgos.Trozo>(etiquetas.size)
        for (v in ventanas) {
            val p = probabilidades(v.palabras)
            for (i in p.indices) {
                // La cita es la ventana que más lo indica; si otra casi empata y es más corta,
                // se prefiere la corta (cita más precisa).
                val actual = donde[i]
                if (actual == null || p[i] > mejor[i] + 0.03f || (p[i] >= mejor[i] - 0.03f && v.palabras.size < actual.palabras.size)) {
                    donde[i] = v
                }
                if (p[i] > mejor[i]) mejor[i] = p[i]
            }
        }
        return etiquetas.indices.map { i -> Deteccion(etiquetas[i], mejor[i], cita(donde[i]?.texto.orEmpty())) }
    }

    /* ---------- Formato del archivo (binario, pequeño) ---------- */

    fun guardar(salida: OutputStream) {
        DataOutputStream(salida.buffered()).use { o ->
            o.writeUTF(MAGIA)
            o.writeInt(Rasgos.BITS)
            o.writeInt(etiquetas.size)
            etiquetas.forEach { o.writeUTF(it) }
            sesgos.forEach { o.writeFloat(it) }
            umbrales.forEach { o.writeFloat(it) }
            o.writeInt(rasgos.size)
            for (i in rasgos.indices) {
                o.writeInt(rasgos[i])
                o.writeShort(inicio[i + 1] - inicio[i])
                for (k in inicio[i] until inicio[i + 1]) {
                    o.writeByte(etiquetaDePeso[k].toInt())
                    // Pesos en 16 bits (media precisión casera): de sobra para este modelo.
                    o.writeShort((pesos[k] * ESCALA).toInt().coerceIn(-32768, 32767))
                }
            }
        }
    }

    companion object {
        private const val MAGIA = "FITIN-CUENTANOSLO-1"
        private const val ESCALA = 512f

        fun sigmoide(z: Float): Float = 1f / (1f + exp(-z))

        fun valorRasgo(cuantos: Int): Float = 1f / kotlin.math.sqrt(cuantos.coerceAtLeast(1).toFloat())

        fun cargar(entrada: InputStream): ModeloPropio = DataInputStream(entrada.buffered()).use { d ->
            check(d.readUTF() == MAGIA) { "Archivo de modelo no válido" }
            check(d.readInt() == Rasgos.BITS) { "El modelo se entrenó con otros rasgos" }
            val n = d.readInt()
            val etiquetas = List(n) { d.readUTF() }
            val sesgos = FloatArray(n) { d.readFloat() }
            val umbrales = FloatArray(n) { d.readFloat() }
            val m = d.readInt()
            val rasgos = IntArray(m)
            val inicio = IntArray(m + 1)
            val etiq = ArrayList<Short>()
            val pesos = ArrayList<Float>()
            for (i in 0 until m) {
                rasgos[i] = d.readInt()
                val cuantos = d.readShort().toInt()
                repeat(cuantos) {
                    etiq += d.readByte().toShort()
                    pesos += d.readShort() / ESCALA
                }
                inicio[i + 1] = pesos.size
            }
            ModeloPropio(etiquetas, sesgos, rasgos, inicio, etiq.toShortArray(), pesos.toFloatArray(), umbrales)
        }

        /** Construye el modelo a partir de pesos densos (los del entrenamiento), quitando los casi nulos. */
        fun desdeDensos(etiquetas: List<String>, sesgos: FloatArray, w: FloatArray, umbrales: FloatArray, minimo: Float): ModeloPropio {
            val n = etiquetas.size
            val rasgos = ArrayList<Int>()
            val inicio = arrayListOf(0)
            val etiq = ArrayList<Short>()
            val pesos = ArrayList<Float>()
            for (r in 0 until Rasgos.DIM) {
                var alguno = false
                for (l in 0 until n) {
                    val p = w[r * n + l]
                    if (kotlin.math.abs(p) >= minimo) {
                        etiq += l.toShort(); pesos += p; alguno = true
                    }
                }
                if (alguno) { rasgos += r; inicio += pesos.size }
            }
            return ModeloPropio(etiquetas, sesgos, rasgos.toIntArray(), inicio.toIntArray(), etiq.toShortArray(), pesos.toFloatArray(), umbrales)
        }

        /** Cita corta para «porque dijiste…»: como mucho 9 palabras del trozo original. */
        fun cita(texto: String): String {
            val p = texto.trim().split(Regex("\\s+"))
            return if (p.size <= 9) texto.trim() else p.take(9).joinToString(" ") + "…"
        }
    }
}
