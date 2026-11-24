package com.marbellup.fitin.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.data.Sport
import com.marbellup.fitin.ui.AnimatedBar
import com.marbellup.fitin.ui.FloatingMascot
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.emoji
import com.marbellup.fitin.ui.theme.LocalExtra

/** «Mis logros»: estadísticas de lo que has hecho y medallas con su progreso. */
@Composable
fun AchievementsScreen(state: AppState) {
    val x = LocalExtra.current
    val st = state.stats()

    GradientBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver") { state.back() }
                Text("Mis logros", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(start = 12.dp).semantics { heading() })
            }

            // Cabecera con la mascota y las medallas conseguidas
            GlassCard(Modifier.padding(horizontal = 16.dp), color = x.lilac) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("🏆 ${st.unlocked} de ${st.medals.size} medallas", style = MaterialTheme.typography.titleLarge)
                        Text("¡Sigue sumando, ${state.userName}!", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        AnimatedBar(st.unlocked.toFloat() / st.medals.size, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.width(10.dp))
                    FloatingMascot(76.dp, happy = true)
                }
            }

            // Estadísticas
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${st.activities}", "actividades\nhechas", "🎯", x.peach, Modifier.weight(1f))
                StatTile("${st.sports}", "deportes\nprobados", "🧭", x.sky, Modifier.weight(1f))
                StatTile("${st.hours} h", "de deporte", "⏱️", x.mint, Modifier.weight(1f))
            }
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${st.ratings}", "valoraciones", "⭐", x.pink, Modifier.weight(1f))
                StatTile("${st.favorites}", "favoritos", "❤️", x.lilac, Modifier.weight(1f))
                StatTile("${state.points}", "puntos\nFit-In", "💜", x.sky, Modifier.weight(1f))
            }

            // Deportes probados
            Text("Deportes que has probado", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 8.dp).semantics { heading() })
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in Sport.entries.take(7)) SportBubble(s, s in st.sportsTried)
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in Sport.entries.drop(7)) SportBubble(s, s in st.sportsTried)
            }

            // Medallas
            Text("Medallas", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp).semantics { heading() })
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (m in st.medals.sortedByDescending { it.progress }) {
                    GlassCard(color = if (m.unlocked) x.card else x.card.copy(alpha = 0.75f)) {
                        Row(Modifier.padding(14.dp).semantics(mergeDescendants = true) {
                            contentDescription = "${m.title}: ${if (m.unlocked) "conseguida" else "${m.current} de ${m.target}"}"
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(56.dp).clip(CircleShape).background(if (m.unlocked) x.peach else x.tile)
                                    .graphicsLayer { alpha = if (m.unlocked) 1f else 0.45f },
                                contentAlignment = Alignment.Center,
                            ) { Text(m.emoji, fontSize = 28.sp) }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(m.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    Text(
                                        if (m.unlocked) "✓ Conseguida" else "${m.current.coerceAtMost(m.target)}/${m.target}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (m.unlocked) x.ok else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(m.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                AnimatedBar(m.progress, if (m.unlocked) x.ok else MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, emoji: String, bg: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(22.dp), color = bg, shadowElevation = 2.dp, modifier = modifier) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 6.dp).semantics(mergeDescendants = true) { },
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RowScope.SportBubble(s: Sport, tried: Boolean) {
    val scale by animateFloatAsState(if (tried) 1f else 0.85f, label = "deporte")
    Box(
        Modifier.weight(1f).aspectRatio(1f).graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (tried) 1f else 0.35f }
            .clip(CircleShape).background(if (tried) LocalExtra.current.card else LocalExtra.current.tile)
            .semantics { contentDescription = s.label + if (tried) ", probado" else ", sin probar" },
        contentAlignment = Alignment.Center,
    ) { Text(s.emoji, fontSize = 22.sp) }
}
