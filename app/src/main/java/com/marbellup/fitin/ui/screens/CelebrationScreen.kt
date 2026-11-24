package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.ui.ActivityMiniRow
import com.marbellup.fitin.ui.ExampleNote
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.theme.LocalExtra
import kotlin.random.Random

/** Pantalla 8 de la referencia: celebración tras apuntarse («¡Gracias por hacer deporte!»). */
@Composable
fun CelebrationScreen(state: AppState, activityId: String) {
    val a = SampleData.activityOrNull(activityId)
    val x = LocalExtra.current
    val companion = state.booking?.companion ?: "Con el grupo"
    val confetti = remember {
        val r = Random(7)
        List(46) { Triple(r.nextFloat(), r.nextFloat(), r.nextInt(4)) }
    }
    val colors = listOf(Color(0xFFFFC83D), Color(0xFF7C5CFF), Color(0xFFFF6FA5), Color(0xFF3CC7A0))
    // Confeti que cae sin parar
    val t = rememberInfiniteTransition(label = "confeti")
    val fall by t.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "caída")

    GradientBackground {
        Canvas(Modifier.fillMaxSize()) {
            for ((fx, fy, c) in confetti) {
                val y = ((fy + fall) % 1f) * size.height
                val x0 = fx * size.width + kotlin.math.sin((fall * 6.28f) + fx * 10f) * 18f
                rotate(degrees = (fall * 360f + fx * 200f), pivot = Offset(x0, y)) {
                    drawRoundRect(colors[c], Offset(x0, y), Size(16f, 30f), androidx.compose.ui.geometry.CornerRadius(6f))
                }
            }
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text("¡Gracias por hacer deporte!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
            Text("Juntos construimos una comunidad más inclusiva", style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            com.marbellup.fitin.ui.FloatingMascot(180.dp, Modifier.padding(vertical = 20.dp), happy = true)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("+${(a?.group?.size ?: 0) + 1}", "personas", x.lilac, Modifier.weight(1f))
                Stat("+${if ((a?.sport?.name ?: "") == "Senderismo") 4 else 1}h", "de deporte", x.mint, Modifier.weight(1f))
                Stat("100%", "inclusión", x.peach, Modifier.weight(1f))
            }

            if (a != null) {
                Text("Plaza reservada · ${companion.lowercase()}", style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp).fillMaxWidth())
                ActivityMiniRow(a, "${a.nextSession} · ${a.place}") { }
                Text("Te lo recordaremos el día antes. ${a.meetingNote}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp).fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))
            GradientButton("Seguir sumando  ♥", icon = null) { state.closeFlow() }
            ExampleNote("Reserva de demostración: no se ha enviado a ninguna instalación real")
        }
    }
}

@Composable
private fun Stat(value: String, label: String, bg: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(22.dp), color = bg, modifier = modifier) {
        Column(Modifier.padding(vertical = 16.dp).semantics(mergeDescendants = true) { },
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
