package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.Tab
import com.marbellup.fitin.data.Need
import com.marbellup.fitin.data.NeedGroup
import com.marbellup.fitin.ui.Avatar
import com.marbellup.fitin.ui.ExampleNote
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.MenuRow
import com.marbellup.fitin.ui.funColor
import com.marbellup.fitin.ui.theme.LocalExtra

/** Pantalla 7 de la referencia: perfil, progreso, preferencias, necesidades y ajustes de accesibilidad. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(state: AppState, bottomBar: @Composable () -> Unit) {
    val x = LocalExtra.current
    var showNeeds by rememberSaveable { mutableStateOf(false) }

    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = bottomBar) { padding ->
        GradientBackground(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, end = 16.dp).height(44.dp), horizontalArrangement = Arrangement.End) {
                    com.marbellup.fitin.ui.RoundIconButton(Icons.Filled.Settings, "Mis preferencias") { state.open(Step.AiSearch) }
                }
                Box {
                    // Aro de colores alrededor de la foto, como en la referencia
                    Box(Modifier.size(104.dp).clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(Color(0xFF7C4DFF), Color(0xFFFF6FA5), Color(0xFFFFB347), Color(0xFF4FC3F7), Color(0xFF7C4DFF))))
                        .padding(4.dp).clip(CircleShape).background(Color.White).padding(3.dp)) {
                        Avatar("${state.userName} García", size = 90.dp)
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(26.dp).clip(CircleShape).background(Color(0xFFF2A93B)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Check, contentDescription = "Perfil verificado", tint = Color.White, modifier = Modifier.size(16.dp)) }
                }
                Text("${state.userName} García", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 8.dp).semantics { heading() })
                Text("Deportista · Fit-In 💜", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(50)).background(x.card).padding(horizontal = 12.dp, vertical = 5.dp))

                // Progreso
                GlassCard(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), color = Color(0xFFD6F5B0)) {
                    Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFD3F7A8), Color(0xFFB8F0E0), Color(0xFFDCCBFF))))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("💜 Tu progreso", style = MaterialTheme.typography.labelLarge)
                            Text("${state.points}", style = MaterialTheme.typography.displaySmall)
                            Text("Puntos Fit-In", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            val bars = listOf(18 to Color(0xFF7C5CFF), 28 to Color(0xFF3CC7A0), 22 to Color(0xFF5ED3C4), 40 to Color(0xFF4FA3FF), 52 to Color(0xFF8B6CFF))
                            for ((h, c) in bars) {
                                Box(Modifier.width(11.dp).height(h.dp).clip(RoundedCornerShape(6.dp)).background(c))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        com.marbellup.fitin.ui.FloatingMascot(68.dp, happy = true)
                    }
                }

                // Menú como en la referencia: icono, texto y «>»
                GlassCard(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(4.dp))
                    MenuRow(Icons.Outlined.EventNote, "Mis actividades", color = funColor(0)) { state.tab = Tab.Calendario }
                    MenuRow(Icons.Outlined.Settings, "Mis preferencias", color = funColor(1)) { state.open(Step.AiSearch) }
                    MenuRow(Icons.Outlined.Accessibility, "Mis necesidades", color = funColor(3)) { showNeeds = !showNeeds }
                    if (showNeeds) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            for (g in NeedGroup.entries) {
                                Text(g.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (n in Need.entries.filter { it.group == g }) {
                                        FilterChip(
                                            selected = n in state.needs,
                                            onClick = { state.toggle(n) },
                                            label = { Text(n.label) },
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier.heightIn(min = 44.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    MenuRow(Icons.Filled.Mic, "Cuéntanoslo (voz o texto)", color = funColor(4)) { state.open(Step.Cuentanoslo) }
                    MenuRow(Icons.Outlined.EmojiEvents, "Mis logros", color = funColor(2)) { state.open(Step.Achievements) }
                    Spacer(Modifier.height(4.dp))
                }

                GlassCard(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Spacer(Modifier.height(4.dp))
                    MenuRow(Icons.Outlined.Psychology, "Cómo aprende tu IA", color = funColor(4)) { state.open(Step.Model) }
                    MenuRow(Icons.Outlined.Notifications, "Avisos de la IA", badge = state.unread, color = funColor(2)) { state.open(Step.Notifications) }
                    MenuRow(Icons.Outlined.AccountBalance, "Panel del Ayuntamiento", color = funColor(5)) { state.open(Step.Council) }
                    Spacer(Modifier.height(4.dp))
                }

                // Accesibilidad de la propia app
                GlassCard(Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp)) {
                    SettingSwitch("Texto más grande", state.bigText) { state.bigText = it }
                    SettingSwitch("Lectura fácil", state.easyRead) { state.easyRead = it }
                    SettingSwitch("Avisarme de actividades que encajan", state.alertsOn) { state.setAlerts(it) }
                }

                OutlinedButton(
                    onClick = { state.publishNext(); state.open(Step.Notifications) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().heightIn(min = 52.dp),
                ) { Text("Demo: el Ayuntamiento publica una actividad") }
                ExampleNote("Perfil de ejemplo para la demo")
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
