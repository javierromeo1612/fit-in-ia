package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.data.Sport
import com.marbellup.fitin.ui.ActivityPhotoCard
import com.marbellup.fitin.ui.FloatingMascot
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.emoji
import com.marbellup.fitin.ui.theme.LocalExtra

private val suggestions = listOf("pádel", "piscina", "silla", "yoga", "tarde", "cubierta", "signos", "playa", "fútbol", "centro")

/** Buscador por palabras clave: «yoga tarde», «piscina grúa», «pádel cubierta»… */
@Composable
fun SearchScreen(state: AppState) {
    val x = LocalExtra.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val results = state.search()

    GradientBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver") { state.back() }
                Spacer(Modifier.width(10.dp))
                TextField(
                    value = state.query,
                    onValueChange = { state.query = it },
                    placeholder = { Text("Buscar actividades…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { state.query = "" }) { Icon(Icons.Filled.Close, contentDescription = "Borrar") }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = x.card, unfocusedContainerColor = x.card,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f).focusRequester(focus),
                )
            }

            // Sugerencias y deportes para tocar
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Sport.entries.toList()) { s ->
                    AssistChip(
                        onClick = { state.query = s.label },
                        label = { Text("${s.emoji} ${s.label}") },
                        shape = RoundedCornerShape(50),
                        colors = AssistChipDefaults.assistChipColors(containerColor = x.card),
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { w ->
                    SuggestionChip(onClick = { state.query = w }, label = { Text(w) }, shape = RoundedCornerShape(50),
                        modifier = Modifier.heightIn(min = 44.dp))
                }
            }

            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                when {
                    state.query.isBlank() -> Hint("Escribe una o varias palabras: deporte, sitio, zona, hora o lo que necesitas (por ejemplo «piscina grúa» o «yoga tarde»).")
                    results.isEmpty() -> Hint("No hay actividades con «${state.query}». Prueba con otra palabra.")
                    else -> {
                        Text(
                            "${results.size} ${if (results.size == 1) "actividad" else "actividades"} · ordenadas por la IA para ti",
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            for (r in results) ActivityPhotoCard(r.activity, state, r, showReason = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingMascot(96.dp)
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
