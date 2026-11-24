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
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, end = 16.dp), horizontalArrangement = Arrangement.End) {
                    com.marbellup.fitin.ui.RoundIconButton(Icons.Filled.Settings, "Mis preferencias") { state.open(Step.AiSearch) }
                }
                Box {
                    Avatar("${state.userName} García", size = 104.dp)
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(x.flame),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Check, contentDescription = "Perfil verificado", tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
                Text("${state.userName} García", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 10.dp).semantics { heading() })
                Text("Deportista Fit-In 💜", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Progreso
                GlassCard(Modifier.padding(16.dp), color = x.lilac) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("💜 Tu progreso", style = MaterialTheme.typography.titleSmall)
                            Text("${state.points}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                            Text("Puntos Fit-In", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            for (h in listOf(18, 28, 22, 40, 52)) {
                                Box(Modifier.width(10.dp).height(h.dp).clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f + h / 100f)))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        com.marbellup.fitin.ui.FloatingMascot(76.dp, happy = true)
                    }
                }

                GlassCard(Modifier.padding(horizontal = 16.dp)) {
                    MenuRow(Icons.Outlined.EventNote, "Mis actividades", "${state.joined.size} con plaza") { state.tab = Tab.Calendario }
                    MenuRow(Icons.Outlined.Tune, "Mis preferencias", "Lo que necesitas, deportes, horario, nivel y zona") { state.open(Step.AiSearch) }
                    MenuRow(Icons.Outlined.Psychology, "Cómo aprende tu IA", "${state.history.size} acciones aprendidas · todo en tu móvil") { state.open(Step.Model) }
                    MenuRow(Icons.Outlined.Accessibility, "Mis necesidades", "${state.needs.size} elegidas · se usan para recomendarte") {
                        showNeeds = !showNeeds
                    }
                    if (showNeeds) {
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                            for (g in NeedGroup.entries) {
                                Text(g.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (n in Need.entries.filter { it.group == g }) {
                                        FilterChip(
                                            selected = n in state.needs,
                                            onClick = { state.toggle(n) },
                                            label = { Text(n.label) },
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier.heightIn(min = 48.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    MenuRow(Icons.Outlined.EmojiEvents, "Mis logros", "${state.stats().unlocked} medallas · ${state.stats().sports} deportes probados") { state.open(Step.Achievements) }
                    MenuRow(Icons.Outlined.Notifications, "Avisos de la IA", badge = state.unread) { state.open(Step.Notifications) }
                    MenuRow(Icons.Outlined.AccountBalance, "Panel del Ayuntamiento", "Impacto y demanda no atendida") { state.open(Step.Council) }
                }

                // Accesibilidad de la propia app
                GlassCard(Modifier.padding(16.dp)) {
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
        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
