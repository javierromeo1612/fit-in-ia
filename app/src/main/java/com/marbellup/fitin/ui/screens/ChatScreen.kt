package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.marbellup.fitin.Step
import com.marbellup.fitin.ui.ActivityMiniRow
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.primaryBrush
import com.marbellup.fitin.ui.theme.LocalExtra

private val suggestions = listOf(
    "¿Qué actividades hay cerca de mí?",
    "¿Qué hay para silla de ruedas?",
    "Pádel hoy por la tarde, pista cubierta",
)

/** Pantalla 5 de la referencia: el asistente deportivo con IA. */
@Composable
fun ChatScreen(state: AppState, bottomBar: @Composable () -> Unit) {
    val x = LocalExtra.current
    var input by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    LaunchedEffect(state.chat.size) {
        if (state.chat.isNotEmpty()) list.animateScrollToItem(state.chat.size - 1)
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(Modifier.imePadding()) {
                // Barra de escritura
                Row(
                    Modifier.fillMaxWidth().background(x.gradientBottom).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Escribe tu mensaje…") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        maxLines = 4,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = x.card,
                            unfocusedContainerColor = x.card,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = { state.sendChat(input); input = "" },
                        enabled = input.isNotBlank(),
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier.size(54.dp),
                    ) {
                        Box(Modifier.background(primaryBrush(), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White)
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
                        Text("Tu asistente deportivo · funciona sin Internet", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RoundIconButton(Icons.Filled.Tune, "Mis características") { state.open(Step.AiSearch) }
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
                            if (i == 0 && state.chat.size == 1) {
                                Column(Modifier.padding(start = 44.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (s in suggestions) {
                                        AssistChip(
                                            onClick = { state.sendChat(s) },
                                            label = { Text(s) },
                                            leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null,
                                                modifier = Modifier.size(18.dp)) },
                                            shape = RoundedCornerShape(50),
                                        )
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
