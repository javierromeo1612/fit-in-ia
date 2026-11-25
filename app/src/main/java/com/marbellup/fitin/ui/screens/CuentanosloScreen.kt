package com.marbellup.fitin.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.Tab
import com.marbellup.fitin.ai.PerfilIA
import com.marbellup.fitin.ai.rememberDictado
import com.marbellup.fitin.data.Need
import com.marbellup.fitin.ui.AppScreen
import com.marbellup.fitin.ui.BottomAction
import com.marbellup.fitin.ui.Cuac
import com.marbellup.fitin.ui.FloatingMascot
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.emoji
import com.marbellup.fitin.ui.primaryBrush
import com.marbellup.fitin.ui.say
import com.marbellup.fitin.ui.theme.LocalExtra
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Paso { Contar, Pensando, Revisar }

/** Una necesidad propuesta, con el motivo y si sigue marcada. */
private class Propuesta(val need: Need, val porque: String, marcada: Boolean = true) {
    var marcada by mutableStateOf(marcada)
}

/**
 * «Cuéntanoslo» (traído de la versión de Talent Land): la persona cuenta con sus palabras,
 * escribiendo o con la voz, qué necesita; el modelo propio (en el móvil, sin internet) propone
 * las necesidades del catálogo con su «porque dijiste…». Nada se guarda sin revisarlo.
 */
@Composable
fun CuentanosloScreen(state: AppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dictado = rememberDictado()

    var paso by remember { mutableStateOf(Paso.Contar) }
    var texto by remember { mutableStateOf("") }
    var perfil by remember { mutableStateOf<PerfilIA.Perfil?>(null) }
    val propuestas = remember { mutableStateListOf<Propuesta>() }
    val preguntas = remember { mutableStateListOf<PerfilIA.Pregunta>() }

    fun ordenar() {
        if (texto.isBlank()) return
        paso = Paso.Pensando
        scope.launch {
            val p = PerfilIA.interpretar(texto)
            delay(700) // El modelo tarda milisegundos: una pausa corta para que se vea a Cuac pensar.
            perfil = p
            propuestas.clear()
            p.sugerencias.forEach { propuestas.add(Propuesta(it.need, it.porque)) }
            preguntas.clear()
            preguntas.addAll(p.preguntas)
            paso = Paso.Revisar
        }
    }

    val permisoMicro = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) dictado.empezar { final -> if (final.isNotBlank()) texto = final }
    }

    fun pulsarMicro() {
        if (dictado.escuchando) {
            dictado.parar()
            return
        }
        val tienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (tienePermiso) dictado.empezar { final -> if (final.isNotBlank()) texto = final }
        else permisoMicro.launch(Manifest.permission.RECORD_AUDIO)
    }

    AppScreen(
        title = "Cuéntanoslo",
        subtitle = say("Tú lo cuentas, tú decides.", "Habla. Luego revisas."),
        pato = false,
        onBack = { if (!state.back()) state.tab = Tab.Inicio },
        bottomBar = {
            when (paso) {
                Paso.Contar -> BottomAction {
                    GradientButton(
                        say("Ordenar lo que he contado", "Seguir"),
                        enabled = texto.isNotBlank() && !dictado.escuchando,
                        onClick = ::ordenar,
                    )
                    TextButton(
                        onClick = { state.open(Step.AiSearch) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Prefiero marcarlo a mano") }
                }
                Paso.Pensando -> {}
                Paso.Revisar -> BottomAction {
                    val n = propuestas.count { it.marcada }
                    GradientButton(
                        if (n == 0) "Seguir sin añadir nada" else "Guardar $n en mi perfil",
                        icon = Icons.Filled.Check,
                        onClick = {
                            val p = perfil
                            state.aplicarPerfil(
                                elegidas = propuestas.filter { it.marcada }.map { it.need }.toSet(),
                                deportes = p?.deportes ?: emptyList(),
                                franja = p?.franja ?: PerfilIA.Franja.Indiferente,
                            )
                            state.steps.clear()
                            state.tab = Tab.Inicio
                        },
                    )
                    TextButton(
                        onClick = { paso = Paso.Contar },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Volver a contarlo") }
                }
            }
        },
    ) {
        when (paso) {
            Paso.Contar -> Contar(
                texto = texto,
                onTexto = { texto = it },
                escuchando = dictado.escuchando,
                parcial = dictado.parcial,
                disponible = dictado.disponible,
                error = dictado.error,
                onMicro = ::pulsarMicro,
                onEjemplo = { texto = PerfilIA.FRASE_DEMO },
            )
            Paso.Pensando -> Pensando(texto)
            Paso.Revisar -> Revisar(
                perfil = perfil,
                propuestas = propuestas,
                preguntas = preguntas,
                onResponder = { pregunta, opcion ->
                    opcion.necesidades.forEach { need ->
                        val ya = propuestas.find { it.need == need }
                        if (ya != null) ya.marcada = true
                        else propuestas.add(Propuesta(need, "respondiste: «${opcion.texto}»"))
                    }
                    preguntas.remove(pregunta)
                },
            )
        }
    }
}

/** Cuac con un bocadillo de texto al lado. */
@Composable
internal fun CuacDice(mensaje: String, pose: Cuac = Cuac.Saluda, tamano: Dp = 72.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FloatingMascot(tamano, pose = pose)
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 10.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp))
                .background(LocalExtra.current.card)
                .padding(12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** Botón redondo grande del micrófono (pulsar y hablar / pulsar para terminar). */
@Composable
internal fun BotonMicro(escuchando: Boolean, onClick: () -> Unit) {
    val x = LocalExtra.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier
                .size(88.dp)
                .shadow(14.dp, CircleShape, ambientColor = x.hero, spotColor = x.hero)
                .semantics { contentDescription = if (escuchando) "Terminar de hablar" else "Pulsar y hablar" },
        ) {
            Box(
                Modifier.fillMaxSize().background(if (escuchando) androidx.compose.ui.graphics.SolidColor(x.flame) else primaryBrush()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (escuchando) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Text(
            if (escuchando) "Pulsa para terminar" else "Pulsa y habla",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/* ---------------------------------------------------------------- Contar */

@Composable
private fun Contar(
    texto: String,
    onTexto: (String) -> Unit,
    escuchando: Boolean,
    parcial: String,
    disponible: Boolean,
    error: String?,
    onMicro: () -> Unit,
    onEjemplo: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CuacDice(
            if (escuchando) "Te escucho." else say(
                "Cuéntame qué necesitas y qué te gusta.",
                "Cuéntame cómo eres y qué te gusta.",
            ),
            pose = Cuac.Saluda,
        )

        if (disponible) {
            BotonMicro(escuchando, onMicro)
        } else {
            Text(
                "Este móvil no tiene dictado. Escríbelo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        OutlinedTextField(
            value = if (escuchando && parcial.isNotBlank()) parcial else texto,
            onValueChange = onTexto,
            enabled = !escuchando,
            minLines = 3,
            label = { Text("O escríbelo aquí") },
            placeholder = { Text("Ej.: voy en silla, me canso rápido y me gusta nadar.") },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = LocalExtra.current.card,
                unfocusedContainerColor = LocalExtra.current.card,
                disabledContainerColor = LocalExtra.current.card,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        TextButton(onClick = onEjemplo, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Usar el ejemplo de Lucía")
        }
        Text(
            "Solo se guarda lo que confirmes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* ---------------------------------------------------------------- Pensando */

@Composable
private fun Pensando(texto: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FloatingMascot(130.dp, pose = Cuac.Piensa)
        Text(
            "Ordenando…",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            "«$texto»",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* ---------------------------------------------------------------- Revisar */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Revisar(
    perfil: PerfilIA.Perfil?,
    propuestas: List<Propuesta>,
    preguntas: List<PerfilIA.Pregunta>,
    onResponder: (PerfilIA.Pregunta, PerfilIA.Opcion) -> Unit,
) {
    val x = LocalExtra.current
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val n = propuestas.size
        CuacDice(
            when {
                n == 0 -> perfil?.resumen?.ifBlank { null } ?: "No he sacado nada claro. Prueba otra vez."
                else -> say(
                    if (n == 1) "He marcado 1 cosa. Revísala." else "He marcado $n cosas. Revísalas.",
                    if (n == 1) "He marcado 1 cosa. Quítala si no es." else "He marcado $n cosas. Quita las que no son.",
                )
            },
            pose = if (n == 0) Cuac.Piensa else Cuac.Feliz,
            tamano = 64.dp,
        )

        if (perfil != null && (perfil.nombre.isNotBlank() || perfil.gestionaFamiliar)) {
            val quien = perfil.nombre.ifBlank { "esta persona" }
            Text(
                if (perfil.gestionaFamiliar) "Perfil de $quien · lo gestionas tú" else "Perfil de $quien",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        if (perfil != null && n > 0 && perfil.resumen.isNotBlank()) {
            Text(
                perfil.resumen,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        if (n > 0) {
            GlassCard {
                propuestas.forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = x.lilac)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = p.marcada, role = Role.Checkbox, onValueChange = { p.marcada = it })
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = p.marcada, onCheckedChange = null)
                        Text(p.need.emoji, fontSize = 22.sp, modifier = Modifier.padding(start = 6.dp))
                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text(p.need.label, style = MaterialTheme.typography.titleSmall)
                            if (p.porque.isNotBlank()) {
                                Text(
                                    if (p.porque.startsWith("respondiste")) p.porque else "porque dijiste «${p.porque}»",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        for (pregunta in preguntas) {
            GlassCard(color = x.lilac) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CuacDice(pregunta.texto, pose = Cuac.Piensa, tamano = 48.dp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (opcion in pregunta.opciones) {
                            OutlinedButton(
                                onClick = { onResponder(pregunta, opcion) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text(opcion.texto) }
                        }
                    }
                }
            }
        }

        if (perfil != null && perfil.deportes.isNotEmpty()) {
            Text(
                (if (perfil.gestionaFamiliar) "Le gusta: " else "Te gusta: ") + perfil.deportes.joinToString(" · ") { it.label },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Text(
            "Solo se guarda lo que confirmes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** Tarjeta que invita a «Cuéntanoslo» (en Inicio, Mis preferencias y Perfil). */
@Composable
internal fun CuentanosloBanner(modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(modifier, color = LocalExtra.current.hero, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().background(com.marbellup.fitin.ui.heroBrush()).padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Cuéntanoslo", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    say("Dilo con tu voz o escríbelo.", "Habla y yo lo marco."),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
            }
            FloatingMascot(56.dp, pose = Cuac.Saluda)
        }
    }
}
