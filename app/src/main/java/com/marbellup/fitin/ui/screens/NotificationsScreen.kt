package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.notify.Alerts
import com.marbellup.fitin.ui.ActivityMiniRow
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.theme.LocalExtra

/**
 * Avisos de la IA: actividades que encajan contigo (≥ 70 %) según tus características y lo que
 * has elegido antes. Abrir o descartar un aviso también enseña al modelo.
 */
@Composable
fun NotificationsScreen(state: AppState) {
    val x = LocalExtra.current
    GradientBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver") { state.back() }
                Text("Avisos", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(start = 12.dp).semantics { heading() })
            }
            Text(
                "Te aviso solo cuando una actividad encaja contigo al menos un ${(Alerts.THRESHOLD * 100).toInt()} %.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            GradientButton("Demo: el Ayuntamiento publica una actividad", Modifier.padding(16.dp), icon = Icons.Filled.Campaign) {
                state.publishNext()
            }

            if (state.notifications.isEmpty()) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(56.dp)
                    Text("Aún no hay avisos. Cuando salga algo que encaje contigo, aparecerá aquí y en tu móvil.",
                        style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                }
            }

            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (n in state.notifications) {
                    val a = n.eventId?.let { SampleData.activityOrNull(it) }
                    GlassCard {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    Modifier.size(42.dp).clip(CircleShape).background(if (n.kind == "match") x.lilac else x.sky),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(if (n.kind == "match") Icons.Filled.AutoAwesome else Icons.Filled.Info,
                                        contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(n.title, style = MaterialTheme.typography.titleMedium)
                                    Text(n.body, style = MaterialTheme.typography.bodyMedium)
                                    n.compat?.let {
                                        Text("Encaje calculado por la IA: ${(it * 100).toInt()} %",
                                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            }
                            if (a != null) {
                                Spacer(Modifier.height(10.dp))
                                Box(Modifier.clip(RoundedCornerShape(18.dp)).background(x.tile).padding(2.dp)) {
                                    ActivityMiniRow(a) { state.openNotification(n) }
                                }
                            }
                            when (n.status) {
                                "pending" -> Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(onClick = { state.openNotification(n) }, shape = RoundedCornerShape(50),
                                        modifier = Modifier.weight(1f).heightIn(min = 50.dp)) { Text("Ver actividad") }
                                    OutlinedButton(onClick = { state.dismissNotification(n) }, shape = RoundedCornerShape(50),
                                        modifier = Modifier.weight(1f).heightIn(min = 50.dp)) { Text("No me interesa") }
                                }
                                "opened" -> Text("✓ La abriste", style = MaterialTheme.typography.labelLarge, color = x.ok,
                                    modifier = Modifier.padding(top = 10.dp))
                                "dismissed" -> Text("Descartada: la IA lo tendrá en cuenta", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}
