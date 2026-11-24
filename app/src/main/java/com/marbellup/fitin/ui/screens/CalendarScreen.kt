package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.data.Dates
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.icon
import com.marbellup.fitin.ui.theme.LocalExtra
import com.marbellup.fitin.ui.tint

private val dayLetters = listOf("L", "M", "X", "J", "V", "S", "D")

/** Pantalla 3 de la referencia: semana, actividades del día en tarjetas pastel y resumen semanal. */
@Composable
fun CalendarScreen(state: AppState, bottomBar: @Composable () -> Unit) {
    val x = LocalExtra.current
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val monday = Dates.today.minusDays((Dates.today.dayOfWeek.value - 1).toLong())
    val selected = Dates.today.plusDays(offset.toLong())
    val all = SampleData.all.filter { it.startDate != null }
    val mine = all.filter { it.id in state.joined }
    val dayItems = all.filter { it.startDate == selected }
        .sortedWith(compareByDescending<com.marbellup.fitin.data.Activity> { it.id in state.joined }.thenBy { it.shortDate })
    val pastel = listOf(x.lilac, x.peach, x.pink, x.mint, x.sky)
    val weekCount = all.count { it.startDate != null && !it.startDate.isBefore(monday) && it.startDate.isBefore(monday.plusDays(7)) }

    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = bottomBar) { padding ->
        GradientBackground(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Row(Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Mi calendario", style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f).semantics { heading() })
                    RoundIconButton(Icons.Filled.Tune, "Buscar con IA") { state.open(Step.AiSearch) }
                }

                // Tira de la semana
                GlassCard(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 6.dp)) {
                        for (i in 0..6) {
                            val day = monday.plusDays(i.toLong())
                            val isSel = day == selected
                            val hasMine = mine.any { it.startDate == day }
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { offset = (day.toEpochDay() - Dates.today.toEpochDay()).toInt() }
                                    .semantics { role = Role.Tab; this.selected = isSel; contentDescription = "Día ${day.dayOfMonth}" }
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(dayLetters[i], style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Box(
                                    Modifier.padding(top = 6.dp).size(40.dp).clip(CircleShape)
                                        .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${day.dayOfMonth}", style = MaterialTheme.typography.titleMedium,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                                Box(Modifier.padding(top = 4.dp).size(6.dp).clip(CircleShape)
                                    .background(if (hasMine) x.flame else Color.Transparent))
                            }
                        }
                    }
                }

                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (dayItems.isEmpty()) {
                        GlassCard {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Mascot(56.dp)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text("Nada este día", style = MaterialTheme.typography.titleMedium)
                                    Text("¿Te busco algo? Dime qué te apetece.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            GradientButton("Buscar con IA", Modifier.padding(16.dp)) { state.open(Step.AiSearch) }
                        }
                    }
                    dayItems.forEachIndexed { i, a ->
                        val booked = a.id in state.joined
                        Surface(
                            onClick = { state.open(Step.Detail(a.id)) },
                            shape = RoundedCornerShape(22.dp),
                            color = pastel[i % pastel.size],
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White),
                                    contentAlignment = Alignment.Center) {
                                    Icon(a.sport.icon, contentDescription = null, tint = a.sport.tint, modifier = Modifier.size(30.dp))
                                }
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(a.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                    Text(a.shortDate, style = MaterialTheme.typography.bodyMedium)
                                    Text(a.place, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (booked) {
                                        Text("✓ Apuntada", style = MaterialTheme.typography.labelLarge, color = x.ok)
                                    }
                                }
                                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }

                    // Resumen semanal, como «Fitness 3 actividades esta semana»
                    Surface(shape = RoundedCornerShape(22.dp), color = x.sky, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp))
                            Column(Modifier.padding(start = 12.dp)) {
                                Text("Esta semana", style = MaterialTheme.typography.titleMedium)
                                Text("$weekCount actividades cerca · ${mine.size} con tu plaza",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
