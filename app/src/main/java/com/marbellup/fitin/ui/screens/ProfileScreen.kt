package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
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
import androidx.compose.ui.semantics.role
import com.marbellup.fitin.ui.entrada
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
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

/** Perfil: «Cuéntanoslo» bien a mano, preferencias, necesidades y ajustes de accesibilidad. */
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
                        .background(Brush.sweepGradient(listOf(Color(0xFF145BCC), Color(0xFFFF6FA5), Color(0xFFFFB347), Color(0xFF4FC3F7), Color(0xFF145BCC))))
                        .padding(4.dp).clip(CircleShape).background(Color.White).padding(3.dp)) {
                        Avatar(state.userName, size = 90.dp)
                    }
                }
                Text(state.userName, style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 8.dp).semantics { heading() })
                // «Cuéntanoslo», lo primero: contar lo que necesitas con la voz o escribiendo.
                CuentanosloCard(Modifier.padding(horizontal = 16.dp, vertical = 14.dp).entrada(0)) { state.open(Step.Cuentanoslo) }

                // Menú como en la referencia: icono, texto y «>»
                GlassCard(Modifier.padding(horizontal = 16.dp).entrada(1)) {
                    Spacer(Modifier.height(4.dp))
                    MenuRow(Icons.AutoMirrored.Outlined.EventNote, "Mis actividades", color = funColor(0)) { state.tab = Tab.Calendario }
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
                    Spacer(Modifier.height(4.dp))
                }


                // Accesibilidad de la propia app
                GlassCard(Modifier.padding(horizontal = 16.dp, vertical = 14.dp).entrada(2)) {
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

/** Tarjeta destacada de «Cuéntanoslo»: degradado azul, la mascota y un botón claro. */
@Composable
private fun CuentanosloCard(modifier: Modifier, onClick: () -> Unit) {
    com.marbellup.fitin.ui.GlassCard(modifier, color = LocalExtra.current.hero, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().background(com.marbellup.fitin.ui.heroBrush())
                .padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 16.dp)
                .semantics(mergeDescendants = true) { role = androidx.compose.ui.semantics.Role.Button },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Cuéntanoslo", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("Dilo con tu voz o escríbelo.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                // «Empezar» respira suavemente para invitar a tocarlo.
                val quieto = com.marbellup.fitin.ui.rememberReducedMotion()
                val respira by androidx.compose.animation.core.rememberInfiniteTransition(label = "empezar").animateFloat(
                    1f, if (quieto) 1f else 1.07f,
                    androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900), androidx.compose.animation.core.RepeatMode.Reverse),
                    label = "respira",
                )
                Row(
                    Modifier.padding(top = 10.dp).graphicsLayer { scaleX = respira; scaleY = respira; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }
                        .clip(RoundedCornerShape(50)).background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Empezar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp))
                }
            }
            com.marbellup.fitin.ui.FloatingMascot(88.dp)
        }
    }
}
