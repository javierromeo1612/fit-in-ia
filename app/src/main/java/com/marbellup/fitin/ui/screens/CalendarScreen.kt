package com.marbellup.fitin.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.data.Activity
import com.marbellup.fitin.data.Dates
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.ui.ActivityCover
import com.marbellup.fitin.ui.FloatingMascot
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.theme.LocalExtra
import java.time.LocalDate

private val dayLetters = listOf("L", "M", "X", "J", "V", "S", "D")
private val monthNames = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto",
    "Septiembre", "Octubre", "Noviembre", "Diciembre")

/**
 * Calendario limpio: una tarjeta blanca con la semana (o el mes) y, debajo, la agenda del día
 * en filas claras con la hora a la izquierda. Un punto violeta = hay actividades; naranja = tienes plaza.
 */
@Composable
fun CalendarScreen(state: AppState, bottomBar: @Composable () -> Unit) {
    val x = LocalExtra.current
    var monthView by rememberSaveable { mutableStateOf(false) }
    var selectedEpoch by rememberSaveable { mutableStateOf(Dates.today.toEpochDay()) }
    var monthShift by rememberSaveable { mutableIntStateOf(0) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)

    // Para que no se vea saturado: solo tus reservas y las actividades que más te encajan,
    // como mucho 2 por día (primero las que tienes reservadas).
    val picks = state.ranked().take(10).map { it.activity }.toSet()
    val all = SampleData.all.filter { it.startDate != null && (it.id in state.joined || it in picks) }
    val byDay = all.groupBy { it.startDate!! }
        .mapValues { (_, list) -> list.sortedByDescending { it.id in state.joined }.take(2) }
    val dayItems = (byDay[selected] ?: emptyList()).sortedBy { timeOf(it) }
    val monday = Dates.today.minusDays((Dates.today.dayOfWeek.value - 1).toLong())
    val week = byDay.filterKeys { !it.isBefore(monday) && it.isBefore(monday.plusDays(7)) }.values.sumOf { it.size }

    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = bottomBar) { padding ->
        GradientBackground(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                // Título + resumen de la semana en una línea, sin tarjetas extra
                Row(Modifier.padding(start = 20.dp, end = 16.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Mi calendario", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                        Text("$week planes para ti esta semana · ${state.joined.size} reservadas",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // Semana / Mes
                    Row(Modifier.clip(RoundedCornerShape(50)).background(x.card).padding(3.dp)) {
                        for ((label, isMonth) in listOf("Semana" to false, "Mes" to true)) {
                            val on = monthView == isMonth
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clip(RoundedCornerShape(50))
                                    .background(if (on) x.lilac else Color.Transparent)
                                    .clickable { monthView = isMonth; monthShift = 0 }
                                    .semantics { role = Role.Tab; this.selected = on }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }

                GlassCard(Modifier.padding(16.dp).animateContentSize()) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                        val days: List<LocalDate?>
                        val first = Dates.today.withDayOfMonth(1).plusMonths(monthShift.toLong())
                        val weekStart = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
                        val shown = if (monthView) first else weekStart
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (monthView) monthShift-- else selectedEpoch -= 7 }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (monthView) "Mes anterior" else "Semana anterior",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${monthNames[shown.monthValue - 1]} ${shown.year}", style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            IconButton(onClick = { if (monthView) monthShift++ else selectedEpoch += 7 }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = if (monthView) "Mes siguiente" else "Semana siguiente",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        days = if (monthView) {
                            List(first.dayOfWeek.value - 1) { null } + (0 until first.lengthOfMonth()).map { first.plusDays(it.toLong()) }
                        } else {
                            (0..6).map { weekStart.plusDays(it.toLong()) }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            for (l in dayLetters) {
                                Text(l, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        for (row in days.chunked(7)) {
                            Row(Modifier.fillMaxWidth()) {
                                for (i in 0 until 7) {
                                    val d = row.getOrNull(i)
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
                        // Leyenda sencilla
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Text("  Hay actividades", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(16.dp))
                            Box(Modifier.size(7.dp).clip(CircleShape).background(x.flame))
                            Text("  Tienes plaza", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Agenda del día elegido
                val title = when (selected) {
                    Dates.today -> "Hoy"
                    Dates.today.plusDays(1) -> "Mañana"
                    else -> dayNames[selected.dayOfWeek.value - 1]
                }
                Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Text("  ${selected.dayOfMonth} de ${monthNames[selected.monthValue - 1].lowercase()}",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                    if (dayItems.isNotEmpty()) {
                        Text(if (dayItems.size == 1) "1 actividad" else "${dayItems.size} actividades",
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Column(Modifier.padding(horizontal = 16.dp)) {
                    if (dayItems.isEmpty()) {
                        GlassCard {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                FloatingMascot(52.dp, pose = com.marbellup.fitin.ui.Cuac.Duerme)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text("Este día está libre", style = MaterialTheme.typography.titleMedium)
                                    Text("Elige un día con punto o mira las actividades en Inicio.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            GradientButton("Ver actividades", Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                state.tab = com.marbellup.fitin.Tab.Inicio
                            }
                        }
                    } else {
                        GlassCard {
                            dayItems.forEachIndexed { i, a ->
                                if (i > 0) HorizontalDivider(Modifier.padding(start = 84.dp, end = 16.dp), color = x.lilac)
                                AgendaRow(state, a)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

/** Hora de la actividad («10:00»), sacada del horario «Sábado, 10:00». */
private fun timeOf(a: Activity): String = a.schedule.substringAfterLast(", ", "")

@Composable
private fun DayCell(d: LocalDate, isSelected: Boolean, isToday: Boolean, items: List<Activity>, joined: List<String>, onClick: () -> Unit) {
    val x = LocalExtra.current
    val mine = items.any { it.id in joined }
    Column(
        Modifier
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Día ${d.dayOfMonth}, ${items.size} actividades" + if (mine) ", tienes plaza" else "" }
            .padding(vertical = 3.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) x.lilac else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium,
                color = when {
                    isSelected -> Color.White
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                })
        }
        // Un solo punto por día: naranja si tienes plaza, violeta si hay actividades
        Box(
            Modifier.padding(top = 3.dp).size(6.dp).clip(CircleShape)
                .background(if (mine) x.flame else if (items.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Transparent),
        )
    }
}

/** Fila de la agenda: hora · foto · título y lugar · flecha. */
@Composable
private fun AgendaRow(state: AppState, a: Activity) {
    val x = LocalExtra.current
    val booked = a.id in state.joined
    Column(Modifier.fillMaxWidth().clickable { state.open(Step.Detail(a.id)) }.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(timeOf(a), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(56.dp))
            ActivityCover(a, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)), small = true)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(a.title, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(a.place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (booked) {
                    Text("✓ Tienes plaza", style = MaterialTheme.typography.labelMedium, color = x.ok, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (booked) {
            // Valorar enseña al modelo qué le gusta de verdad (la señal más fuerte).
            val rating = state.ratingOf(a.id)
            Row(Modifier.padding(start = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(when (rating) { true -> "Te gustó"; false -> "No te gustó"; null -> "¿Te ha gustado?" },
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
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
