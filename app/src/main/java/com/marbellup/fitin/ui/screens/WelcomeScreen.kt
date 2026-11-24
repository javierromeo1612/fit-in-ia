package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.ui.ExampleNote
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.Logo
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.theme.LocalExtra

/** Pantalla 1 de la referencia: logo, lema, ilustración y «Comenzar». */
@Composable
fun WelcomeScreen(state: AppState) {
    val x = LocalExtra.current
    GradientBackground {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Logo(size = 96.dp)
            Text(
                "Fit-In",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp).semantics { heading() },
            )
            Text(
                "Deporte inclusivo\npara todos",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )

            // Ilustración: escena deportiva al aire libre (cielo, nubes, colina) con personajes que se mueven.
            val t = rememberInfiniteTransition(label = "escena")
            val bob by t.animateFloat(0f, -10f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bote")
            val ball by t.animateFloat(0f, -36f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "balón")
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp).clip(RoundedCornerShape(36.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFB9A6FF), Color(0xFFD9CCFF), Color(0xFFF1ECFF)))),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    // Nubes
                    for ((cx, cy, r) in listOf(Triple(0.2f, 0.18f, 0.07f), Triple(0.28f, 0.16f, 0.09f), Triple(0.75f, 0.12f, 0.08f), Triple(0.84f, 0.14f, 0.06f))) {
                        drawCircle(Color.White.copy(alpha = 0.85f), radius = w * r, center = Offset(w * cx, h * cy))
                    }
                    // Colina verde
                    drawOval(Color(0xFF8FD694), topLeft = Offset(-w * 0.2f, h * 0.68f), size = Size(w * 1.4f, h * 0.6f))
                    drawOval(Color(0xFF6CC47A), topLeft = Offset(w * 0.35f, h * 0.76f), size = Size(w * 0.9f, h * 0.5f))
                    // Canasta
                    drawRect(Color(0xFFE8E2FF), topLeft = Offset(w * 0.08f, h * 0.3f), size = Size(w * 0.025f, h * 0.45f))
                    drawRect(Color.White, topLeft = Offset(w * 0.03f, h * 0.26f), size = Size(w * 0.16f, h * 0.1f))
                    drawCircle(Color(0xFFF2852B), radius = w * 0.045f, center = Offset(w * 0.15f, h * 0.38f), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.01f))
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom) {
                    Text("🧑‍🦽", fontSize = 86.sp, modifier = Modifier.graphicsLayer { translationY = bob * density })
                    Box(contentAlignment = Alignment.TopCenter) {
                        Text("⛹️‍♀️", fontSize = 86.sp, modifier = Modifier.graphicsLayer { translationY = -bob * density })
                    }
                }
                Text("🏀", fontSize = 40.sp, modifier = Modifier.align(Alignment.Center).graphicsLayer { translationY = ball * density })
                Mascot(70.dp, Modifier.align(Alignment.TopEnd).padding(14.dp).graphicsLayer { translationY = bob * density })
            }

            Text(
                "Dinos qué necesitas y la IA te propone actividad, sitio comprobado y compañía.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )
            GradientButton("Comenzar") { state.startOnboarding() }
            TextButton(onClick = { state.finishOnboarding() }, modifier = Modifier.padding(top = 6.dp).heightIn(min = 48.dp)) {
                Text("Entrar sin contar nada", style = MaterialTheme.typography.titleSmall)
            }
            ExampleNote("Prototipo de hackathon · datos de ejemplo")
        }
    }
}

@Composable
private fun Bubble(icon: ImageVector, bg: Color, tint: Color, size: Dp, modifier: Modifier) {
    Surface(shape = CircleShape, color = bg, shadowElevation = 4.dp, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
    }
}
