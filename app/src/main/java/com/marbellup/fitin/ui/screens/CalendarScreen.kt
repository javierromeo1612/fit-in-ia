package com.marbellup.fitin.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.sp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.data.Activity
import com.marbellup.fitin.data.Dates
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.ui.FloatingMascot
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.emoji
import com.marbellup.fitin.ui.theme.LocalExtra
import com.marbellup.fitin.ui.tint
import java.time.LocalDate

private val dayLetters = listOf("L", "M", "X", "J", "V", "S", "D")
private val monthNames = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto",
    "Septiembre", "Octubre", "Noviembre", "Diciembre")

/**
 * Calendario grande: vista de semana (como la referencia) que se amplía a mes completo.
 * Cada día muestra puntos de colores por actividad; tus reservas llevan aro naranja.
 */
@Composable
fun CalendarScreen(state: AppState, bottomBar: @Composable () -> Unit) {
    val x = LocalExtra.current
    var monthView by rememberSaveable { mutableStateOf(false) }
    var selectedEpoch by rememberSaveable { mutableStateOf(Dates.today.toEpochDay()) }
    var monthShift by rememberSaveable { mutableIntStateOf(0) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)

    val all = SampleData.all.filter { it.startDate != null }
    val byDay = all.groupBy { it.startDate!! }
    val dayItems = (byDay[selected] ?: emptyList()).sortedWith(compareByDescending<Activity> { it.id in state.joined }.thenBy { it.shortDate })
    val pastel = listOf(x.lilac, x.mint, x.pink, x.peach, x.sky)

    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = bottomBar) { padding ->
        GradientBackground(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Mi calendario", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f).semantics { heading() })
                    // Semana / Mes
                    Row(Modifier.clip(RoundedCornerShape(50)).background(x.card).padding(4.dp)) {
                        for ((label, isMonth) in listOf("Semana" to false, "Mes" to true)) {
                            val on = monthView == isMonth
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (on) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clip(RoundedCornerShape(50))
                                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { monthView = isMonth; monthShift = 0 }
                                    .semantics { role = Role.Tab; this.selected = on }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }

                GlassCard(Modifier.padding(16.dp).animateContentSize(), color = if (monthView) x.card else Color.White.copy(alpha = 0.55f)) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                        val days: List<LocalDate?>
                        if (monthView) {
                            val first = Dates.today.withDayOfMonth(1).plusMonths(monthShift.toLong())
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { monthShift-- }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes anterior") }
                                Text("${monthNames[first.monthValue - 1]} ${first.year}", style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                IconButton(onClick = { monthShift++ }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mes siguiente") }
                            }
                            val lead = first.dayOfWeek.value - 1
                            days = List(lead) { null } + (0 until first.lengthOfMonth()).map { first.plusDays(it.toLong()) }
                        } else {
                            val monday = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selectedEpoch -= 7 }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Semana anterior") }
                                Text("${monthNames[monday.monthValue - 1]} ${monday.year}", style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                IconButton(onClick = { selectedEpoch += 7 }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Semana siguiente") }
                            }
                            days = (0..6).map { monday.plusDays(it.toLong()) }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            for (l in dayLetters) {
                                Text(l, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        for (week in days.chunked(7)) {
                            Row(Modifier.fillMaxWidth()) {
                                for (i in 0 until 7) {
                                    val d = week.getOrNull(i)
                                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        if (d != null) {
                                            DayCell(d, d == selected, d == Dates.today, byDay[d].orEmpty(), state.joined) {
                                                selectedEpoch = d.toEpochDay()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Leyenda
                        Row(Modifier.padding(start = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(CircleShape).border(2.dp, x.flame, CircleShape))
                            Text(" Tus reservas   ", style = MaterialTheme.typography.bodySmall)
                            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Text(" Actividades", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                val title = when (selected) {
                    Dates.today -> "Hoy"
                    Dates.today.plusDays(1) -> "Mañana"
                    else -> "${selected.dayOfMonth} de ${monthNames[selected.monthValue - 1].lowercase()}"
                }
                Text(title, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp).semantics { heading() })

                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (dayItems.isEmpty()) {
                        GlassCard {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                FloatingMascot(56.dp)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text("Nada este día", style = MaterialTheme.typography.titleMedium)
                                    Text("Toca un día con puntos o mira lo que te recomiendo.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            GradientButton("Ver recomendaciones", Modifier.padding(16.dp)) { state.tab = com.marbellup.fitin.Tab.ParaTi }
                        }
                    }
                    dayItems.forEachIndexed { i, a -> DayActivity(state, a, pastel[i % pastel.size]) }

                    // Resumen semanal como en la referencia
                    val monday = Dates.today.minusDays((Dates.today.dayOfWeek.value - 1).toLong())
                    val week = all.count { !it.startDate!!.isBefore(monday) && it.startDate.isBefore(monday.plusDays(7)) }
                    Surface(shape = RoundedCornerShape(22.dp), color = x.sky, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐", fontSize = 30.sp)
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text("$week actividades esta semana", style = MaterialTheme.typography.titleMedium)
                                Text("${state.joined.size} con tu plaza reservada", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, isSelected: Boolean, isToday: Boolean, items: List<Activity>, joined: List<String>, onClick: () -> Unit) {
    val x = LocalExtra.current
    val mine = items.any { it.id in joined }
    Column(
        Modifier
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Día ${d.dayOfMonth}, ${items.size} actividades" + if (mine) ", tienes reserva" else "" }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) x.lilac else Color.Transparent)
                .then(if (mine) Modifier.border(2.5.dp, x.flame, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
        }
        Row(Modifier.height(10.dp).padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (a in items.take(3)) Box(Modifier.size(6.dp).clip(CircleShape).background(a.sport.tint))
        }
    }
}

@Composable
private fun DayActivity(state: AppState, a: Activity, bg: Color) {
    val x = LocalExtra.current
    val booked = a.id in state.joined
    Surface(
        onClick = { state.open(Step.Detail(a.id)) },
        shape = RoundedCornerShape(24.dp),
        color = bg,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Text(a.sport.emoji, fontSize = 36.sp)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(a.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Text(a.shortDate, style = MaterialTheme.typography.bodyMedium)
                    Text(a.place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            if (booked) {
                // Valorar enseña al modelo qué le gusta de verdad (la señal más fuerte).
                val rating = state.ratingOf(a.id)
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✓ Apuntada", style = MaterialTheme.typography.labelLarge, color = x.ok, modifier = Modifier.weight(1f))
                    Text(when (rating) { true -> "Te gustó"; false -> "No te gustó"; null -> "¿Te ha gustado?" },
                        style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { state.rate(a.id, true) }) {
                        Icon(Icons.Filled.ThumbUp, contentDescription = "Me ha gustado",
                            tint = if (rating == true) x.ok else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { state.rate(a.id, false) }) {
                        Icon(Icons.Filled.ThumbDown, contentDescription = "No me ha gustado",
                            tint = if (rating == false) x.barrier else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
