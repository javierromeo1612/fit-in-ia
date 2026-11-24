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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.ui.ActivityMiniRow
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.primaryBrush
import com.marbellup.fitin.ui.theme.LocalExtra

/**
 * Bandeja de notificaciones inteligentes: invitaciones del smart grouping para completar grupos.
 * El botón «Simular» hace que otra persona compatible busque lo mismo, para enseñar el lado receptor.
 */
@Composable
fun NotificationsScreen(state: AppState) {
    val x = LocalExtra.current
    LaunchedEffect(Unit) { state.refreshNotifications() }
    GradientBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver") { state.back() }
                Text("Notificaciones", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(start = 12.dp).semantics { heading() })
            }
            OutlinedButton(
                onClick = { state.simulateIncoming() },
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Demo: alguien compatible te busca", style = MaterialTheme.typography.titleSmall)
            }

            if (state.notifications.isEmpty()) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(56.dp)
                    Text("Aún no tienes avisos. Cuando la IA encuentre gente o planes que encajen contigo, aparecerán aquí.",
                        style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                }
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (n in state.notifications) {
                    val a = n.eventId?.let { SampleData.activityOrNull(it) }
                    GlassCard {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    Modifier.size(42.dp).clip(CircleShape).background(
                                        when (n.kind) { "invite" -> x.lilac; "group_complete" -> x.mint; else -> x.sky },
                                    ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        when (n.kind) { "invite" -> Icons.Filled.GroupAdd; "group_complete" -> Icons.Filled.CheckCircle; else -> Icons.Filled.Info },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(n.title, style = MaterialTheme.typography.titleMedium)
                                    Text(n.body, style = MaterialTheme.typography.bodyMedium)
                                    n.compat?.let {
                                        Text("Compatibilidad ${(it * 100).toInt()} % · calculada por la IA",
                                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            }
                            if (a != null) {
                                Spacer(Modifier.height(10.dp))
                                Box(Modifier.clip(RoundedCornerShape(18.dp)).background(x.tile).padding(2.dp)) {
                                    ActivityMiniRow(a) { state.open(Step.Detail(a.id)) }
                                }
                            }
                            when (n.status) {
                                "pending" -> Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(
                                        onClick = { state.respond(n, true) },
                                        shape = RoundedCornerShape(50),
                                        color = Color.Transparent,
                                        modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                                    ) {
                                        Box(Modifier.background(primaryBrush(), RoundedCornerShape(50)).padding(14.dp),
                                            contentAlignment = Alignment.Center) {
                                            Text("Me apunto", color = Color.White, style = MaterialTheme.typography.titleSmall)
                                        }
                                    }
                                    OutlinedButton(onClick = { state.respond(n, false) }, shape = RoundedCornerShape(50),
                                        modifier = Modifier.weight(1f).heightIn(min = 50.dp)) { Text("Ahora no") }
                                }
                                "accepted" -> Text("✓ Te has apuntado", style = MaterialTheme.typography.titleSmall, color = x.ok,
                                    modifier = Modifier.padding(top = 10.dp))
                                "declined" -> Text("No te apuntaste", style = MaterialTheme.typography.bodySmall,
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
