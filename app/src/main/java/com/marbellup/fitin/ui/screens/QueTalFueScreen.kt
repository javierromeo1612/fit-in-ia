package com.marbellup.fitin.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.marbellup.fitin.AppState
import com.marbellup.fitin.ai.PerfilIA
import com.marbellup.fitin.ai.rememberDictado
import com.marbellup.fitin.data.Carita
import com.marbellup.fitin.data.Need
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.ui.AppScreen
import com.marbellup.fitin.ui.BottomAction
import com.marbellup.fitin.ui.Cuac
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.emoji
import com.marbellup.fitin.ui.funColor
import com.marbellup.fitin.ui.pastel
import com.marbellup.fitin.ui.say
import com.marbellup.fitin.ui.theme.LocalExtra

/** Frase de ejemplo para la demo (historia de Lucía), según el deporte. */
private fun valoracionDemo(deporte: String) = when (deporte) {
    "Natacion" -> "¡Me ha encantado nadar con Marta! Estaba todo tranquilo y he aguantado bien."
    "Baloncesto" -> "Me ha encantado jugar, pero había muchísimo ruido y he acabado agotada."
    else -> "Me lo he pasado bien. Me gustaría repetir con las mismas personas."
}

private val Carita.emoji: String
    get() = when (this) {
        Carita.Regular -> "😕"
        Carita.Bien -> "🙂"
        Carita.Genial -> "🤩"
    }

/**
 * «¿Qué tal fue?» (traído de la versión de Talent Land): valoración de 10 segundos después de una
 * actividad, con carita y, si quiere, unas palabras (escritas o con la voz). Enseña al modelo
 * qué le gusta y, si en lo que cuenta aparece una necesidad nueva («había mucho ruido»),
 * Cuac propone añadirla al perfil.
 */
@Composable
fun QueTalFueScreen(state: AppState, activityId: String) {
    val context = LocalContext.current
    val actividad = SampleData.activityOrNull(activityId)
    val dictado = rememberDictado()
    val previa = state.reviews[activityId]
    var carita by remember { mutableStateOf(previa?.carita) }
    var texto by remember { mutableStateOf(previa?.texto ?: "") }
    var guardado by remember { mutableStateOf(false) }
    // Necesidades nuevas que aparecen en lo que ha contado (las que aún no tiene en el perfil).
    var nuevas by remember { mutableStateOf<List<PerfilIA.Sugerencia>>(emptyList()) }

    LaunchedEffect(guardado) {
        if (guardado && texto.isNotBlank()) {
            nuevas = PerfilIA.interpretar(texto).sugerencias.filter { it.need !in state.needs }.take(3)
        }
    }

    val permisoMicro = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) dictado.empezar { if (it.isNotBlank()) texto = it }
    }

    AppScreen(
        title = say("¿Qué tal fue?", "¿Qué tal?"),
        subtitle = actividad?.title,
        onBack = { state.back() },
        bottomBar = {
            BottomAction {
                if (!guardado) {
                    GradientButton(
                        "Guardar",
                        icon = Icons.Filled.Check,
                        enabled = (carita != null || texto.isNotBlank()) && !dictado.escuchando,
                        onClick = {
                            state.valorar(activityId, carita ?: Carita.Bien, texto)
                            guardado = true
                        },
                    )
                } else {
                    GradientButton("Volver", onClick = { state.back() })
                }
            }
        },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (guardado) {
                CuacDice(say("¡Gracias! Lo tendré en cuenta para proponerte planes.", "¡Gracias! Lo tendré en cuenta."), pose = Cuac.Feliz)
                if (nuevas.isNotEmpty()) {
                    GlassCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Por lo que cuentas, ¿añado esto a tu perfil?", style = MaterialTheme.typography.titleMedium)
                            for (s in nuevas) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(s.need.emoji, fontSize = 22.sp)
                                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                        Text(s.need.label, style = MaterialTheme.typography.titleSmall)
                                        if (s.porque.isNotBlank()) {
                                            Text("porque dijiste «${s.porque}»", style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    AnadirNecesidad(state, s.need)
                                }
                            }
                        }
                    }
                }
                return@Column
            }

            CuacDice(
                if (dictado.escuchando) "Te escucho." else say(
                    "¿Qué tal te ha ido? Cuéntamelo como quieras.",
                    "¿Qué tal te ha ido?",
                ),
                pose = if (dictado.escuchando) Cuac.Piensa else Cuac.Saluda,
            )

            // Tres caritas, siempre con texto.
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Carita.entries.forEachIndexed { i, c ->
                    val sel = c == carita
                    val color = funColor(if (i == 0) 2 else if (i == 1) 1 else 3)
                    val shape = RoundedCornerShape(22.dp)
                    Surface(
                        shape = shape,
                        color = if (sel) color.pastel(0.4f) else LocalExtra.current.card,
                        shadowElevation = if (sel) 6.dp else 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 96.dp)
                            .then(if (sel) Modifier.border(3.dp, color, shape) else Modifier)
                            .selectable(selected = sel, role = Role.RadioButton, onClick = { carita = c }),
                    ) {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(c.emoji, fontSize = 36.sp)
                            Text(c.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            if (dictado.disponible) {
                BotonMicro(dictado.escuchando) {
                    if (dictado.escuchando) dictado.parar()
                    else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                        dictado.empezar { if (it.isNotBlank()) texto = it }
                    else permisoMicro.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            if (dictado.error != null) {
                Text(dictado.error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            OutlinedTextField(
                value = if (dictado.escuchando && dictado.parcial.isNotBlank()) dictado.parcial else texto,
                onValueChange = { texto = it },
                enabled = !dictado.escuchando,
                minLines = 3,
                label = { Text("O escríbelo (opcional)") },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = LocalExtra.current.card,
                    unfocusedContainerColor = LocalExtra.current.card,
                    disabledContainerColor = LocalExtra.current.card,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = {
                    val deporte = actividad?.sport?.name ?: ""
                    carita = if (deporte == "Baloncesto") Carita.Bien else Carita.Genial
                    texto = valoracionDemo(deporte)
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Usar el ejemplo de Lucía") }
            Text(
                "Solo sirve para proponerte mejores planes. Se queda en tu móvil.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AnadirNecesidad(state: AppState, need: Need) {
    val ya = need in state.needs
    FilledTonalButton(
        onClick = { if (!ya) state.toggle(need) },
        enabled = !ya,
        shape = RoundedCornerShape(50),
        modifier = Modifier.heightIn(min = 44.dp),
    ) {
        Icon(if (ya) Icons.Filled.Check else Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(if (ya) " Añadida" else " Añadir")
    }
}
