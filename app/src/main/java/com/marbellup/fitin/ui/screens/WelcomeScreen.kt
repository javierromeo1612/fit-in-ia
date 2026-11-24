package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
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

            // Ilustración: escena deportiva hecha con iconos sobre círculos pastel.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(250.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color.White, x.lilac, x.gradientTop))),
                )
                Bubble(Icons.AutoMirrored.Filled.Accessible, x.sky, Color(0xFF2F5FD0), 92.dp, Modifier.offset((-70).dp, 30.dp))
                Bubble(Icons.Filled.SportsBasketball, x.peach, Color(0xFFC2501F), 70.dp, Modifier.offset(80.dp, (-70).dp).rotate(-12f))
                Bubble(Icons.Filled.Pool, x.mint, Color(0xFF1B7F52), 62.dp, Modifier.offset(90.dp, 60.dp))
                Bubble(Icons.Filled.SportsTennis, x.pink, Color(0xFFA3346B), 56.dp, Modifier.offset((-90).dp, (-75).dp))
                Mascot(110.dp, Modifier.offset(10.dp, (-6).dp))
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
