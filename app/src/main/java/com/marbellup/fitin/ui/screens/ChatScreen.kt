package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.ChatOption
import com.marbellup.fitin.data.Sport
import com.marbellup.fitin.ui.icon
import com.marbellup.fitin.ui.tint
import com.marbellup.fitin.Step
import com.marbellup.fitin.ui.ActivityMiniRow
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.primaryBrush
import com.marbellup.fitin.ui.theme.LocalExtra


/** Pantalla 5 de la referencia: el asistente deportivo con IA. */
@Composable
fun ChatScreen(state: AppState, bottomBar: @Composable () -> Unit) {
    val x = LocalExtra.current
    var pickSport by rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()
    LaunchedEffect(state.chat.size) {
        if (state.chat.isNotEmpty()) list.animateScrollToItem(state.chat.size - 1)
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                // En vez de escribir, se elige: así el modelo recibe datos limpios.
                Column(Modifier.fillMaxWidth().background(x.gradientBottom).padding(vertical = 8.dp)) {
                    Text(
                        if (pickSport) "¿Qué deporte?" else "Elige una pregunta",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                    )
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pickSport) {
                            items(Sport.entries.toList()) { s ->
                                AssistChip(
                                    onClick = { state.ask(ChatOption.Deporte, s); pickSport = false },
                                    label = { Text(s.label) },
                                    leadingIcon = { Icon(s.icon, contentDescription = null, tint = s.tint, modifier = Modifier.size(18.dp)) },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                        } else {
                            items(ChatOption.entries.toList()) { o ->
                                AssistChip(
                                    onClick = { if (o == ChatOption.Deporte) pickSport = true else state.ask(o) },
                                    label = { Text(o.question) },
                                    leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    shape = RoundedCornerShape(50),
                                    colors = AssistChipDefaults.assistChipColors(containerColor = x.card),
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                        }
                    }
                }
                bottomBar()
            }
        },
    ) { padding ->
        GradientBackground(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(52.dp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("Fit-In", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                        Text("Tu asistente deportivo", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RoundIconButton(Icons.Filled.Tune, "Mis preferencias") { state.open(Step.AiSearch) }
                }
                LazyColumn(
                    state = list,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(state.chat) { i, m ->
                        if (m.fromUser) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text(
                                    m.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .widthIn(max = 290.dp)
                                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 6.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
                                        .background(x.userBubble)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.Top) {
                                Mascot(36.dp)
                                Spacer(Modifier.width(8.dp))
                                Column(
                                    Modifier
                                        .widthIn(max = 300.dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
                                        .background(x.card)
                                        .padding(12.dp)
                                        .semantics { if (i == state.chat.lastIndex) liveRegion = LiveRegionMode.Polite },
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(m.text, style = MaterialTheme.typography.bodyLarge)
                                    for (rec in m.recommendations) {
                                        ActivityMiniRow(
                                            rec.activity,
                                            subtitle = "${rec.match} % · ${rec.activity.shortDate} · ${rec.activity.place}",
                                        ) { state.open(Step.Detail(rec.activity.id)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
