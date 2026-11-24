package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.data.Seal
import com.marbellup.fitin.ui.ActivityCover
import com.marbellup.fitin.ui.AiReason
import com.marbellup.fitin.ui.Avatar
import com.marbellup.fitin.ui.ExampleNote
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.shortLabel
import com.marbellup.fitin.ui.IconBadge
import com.marbellup.fitin.ui.deeper
import com.marbellup.fitin.ui.funColor
import com.marbellup.fitin.ui.pastel
import com.marbellup.fitin.ui.emoji
import com.marbellup.fitin.ui.tint
import com.marbellup.fitin.ui.theme.LocalExtra

/**
 * Ficha de actividad, como la pantalla 4 de la referencia: foto arriba con volver y corazón,
 * tarjeta blanca flotante con título, descripción, fecha, sitio, accesibilidad en píldoras,
 * plazas y el botón «Apuntarme →».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityScreen(state: AppState, activityId: String) {
    val a = SampleData.activityOrNull(activityId)
    if (a == null) {
        LaunchedEffect(Unit) { state.back() }
        return
    }
    val x = LocalExtra.current
    val rec = state.scoreFor(a.id)
    // Abrir la ficha es una señal débil de interés: el modelo aprende de ella (una vez por actividad).
    LaunchedEffect(a.id) { state.viewed(a.id) }
    val booked = a.id in state.joined
    val fav = a.id in state.favorites

    GradientBackground {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 20.dp)) {
            // Foto con botones redondos encima
            Box(Modifier.fillMaxWidth().height(270.dp)) {
                ActivityCover(a, Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)))
                Row(Modifier.statusBarsPadding().padding(14.dp).fillMaxWidth()) {
                    RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver") { state.back() }
                    Spacer(Modifier.weight(1f))
                    RoundIconButton(Icons.Outlined.IosShare, "Compartir") { }
                }
            }

            // Tarjeta blanca flotante (con márgenes, como en la referencia)
            Box(Modifier.padding(horizontal = 14.dp).offset(y = (-44).dp)) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = x.card,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                ) {
                    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp)) {
                        if (a.status == "propuesta") {
                            Text("✨ Grupo propuesto · se confirma con ${a.minParticipants} personas",
                                style = MaterialTheme.typography.labelMedium, color = x.onWarnContainer,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(x.warnContainer).padding(horizontal = 10.dp, vertical = 4.dp))
                            Spacer(Modifier.height(8.dp))
                        }
                        // Etiqueta del deporte en su color
                        Text("${a.sport.emoji}  ${a.sport.label}", style = MaterialTheme.typography.labelLarge, color = a.sport.tint.deeper(),
                            modifier = Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(50)).background(a.sport.pastel)
                                .padding(horizontal = 12.dp, vertical = 5.dp))
                        Text(a.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                        Text(a.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                        Spacer(Modifier.height(14.dp))
                        Line(Icons.Outlined.CalendarMonth, a.nextSession.replaceFirstChar { it.uppercase() }, color = funColor(0))
                        Line(Icons.Filled.Place, a.place, color = funColor(2))
                        Line(Icons.AutoMirrored.Filled.Accessible, "Accesibilidad", color = funColor(1))
                        FlowRow(
                            Modifier.padding(start = 46.dp, top = 2.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            a.features.sortedBy { it.ordinal }.forEachIndexed { i, n -> Chip(n.shortLabel, n in state.needs, funColor(i + 1)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        Line(Icons.Filled.Groups, if (a.spotsLeft == 1) "Queda 1 plaza" else "Quedan ${a.spotsLeft} plazas", bold = true, color = funColor(3))

                        if (rec != null) {
                            AiReason("${rec.match} % de encaje contigo. ${rec.explanation}", Modifier.padding(top = 10.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        if (booked) {
                            Text("✓ Ya tienes tu plaza", style = MaterialTheme.typography.titleMedium, color = x.ok,
                                modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedButton(onClick = { state.closeFlow() }, shape = RoundedCornerShape(50),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                                Text("Ver en mi calendario", style = MaterialTheme.typography.titleMedium)
                            }
                        } else {
                            GradientButton(if (a.spotsLeft > 0) "Apuntarme" else "Sin plazas", enabled = a.spotsLeft > 0) {
                                state.open(Step.Company(a.id))
                            }
                            TextButton(onClick = { state.dismiss(a.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                                Text("No me interesa", style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                // Corazón sobre el borde de la foto, a la derecha
                Surface(
                    onClick = { state.toggleFavorite(a.id) },
                    shape = CircleShape, color = Color.White, shadowElevation = 4.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 18.dp).size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (fav) "Quitar de favoritos" else "Guardar en favoritos",
                            tint = if (fav) Color(0xFFE0447A) else MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Más información (verificación, grupo, cómo llegar)
            Column(Modifier.padding(horizontal = 20.dp).offset(y = (-30).dp)) {
                Text("Más información", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(6.dp))
                Line(
                    if (a.seal == Seal.Ninguno) Icons.Outlined.Info else Icons.Filled.Verified,
                    a.seal.label + if (a.verifiedOn.isNotBlank()) " · comprobado en ${a.verifiedOn}" else "",
                    color = funColor(3),
                )
                if (a.confirmations > 0) Line(Icons.Filled.ThumbUp, "${a.confirmations} personas confirman que es accesible", color = funColor(1))
                if (a.howToArrive.isNotBlank()) Line(Icons.Filled.Directions, a.howToArrive, color = funColor(5))
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    a.group.take(4).forEach { Avatar(it.name, size = 32.dp, modifier = Modifier.padding(end = 4.dp)) }
                    Text("  Ya van ${a.group.size}", style = MaterialTheme.typography.bodyMedium)
                }
                ExampleNote()
            }
        }
    }
}

@Composable
private fun Line(icon: ImageVector, text: String, bold: Boolean = false, color: Color = MaterialTheme.colorScheme.primary) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, color, size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Text(text, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium)
    }
}

/** Píldora de color, como «Silla de ruedas» / «Aseo adaptado» en la referencia. Las tuyas llevan ✓. */
@Composable
private fun Chip(text: String, mine: Boolean, color: Color) {
    Text(
        (if (mine) "✓ " else "") + text,
        style = MaterialTheme.typography.labelMedium,
        color = color.deeper(),
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(color.pastel(if (mine) 0.32f else 0.18f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
