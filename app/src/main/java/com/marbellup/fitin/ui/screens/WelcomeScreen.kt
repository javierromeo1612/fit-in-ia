package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.R
import kotlin.math.max

/** Medidas de la imagen de bienvenida (res/drawable-nodpi/bienvenida.webp), en píxeles. */
private const val ANCHO = 852f
private const val ALTO = 1798f

/** Dónde está el «Comenzar» dibujado en la imagen (píxeles de la imagen). */
private const val BOTON_X = 175f
private const val BOTON_Y = 1572f
private const val BOTON_ANCHO = 505f
private const val BOTON_ALTO = 120f

/**
 * Primera pantalla, cada vez que se abre la app: la imagen de bienvenida del equipo a
 * pantalla completa. Se quitó la barra de inicio dibujada abajo para que no se confunda con
 * la del móvil. El «Comenzar» de la imagen lleva encima un botón real que entra en la app
 * (TalkBack lo lee como «Comenzar»).
 */
@Composable
fun WelcomeScreen(state: AppState) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.bienvenida),
            contentDescription = "Fit-In. Deporte inclusivo para todos. Un chico en silla de ruedas y una chica juegan al baloncesto.",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Misma cuenta que ContentScale.Crop: escala para cubrir la pantalla y centra.
        val escala = max(maxWidth.value / ANCHO, maxHeight.value / ALTO)
        val x0 = (maxWidth.value - ANCHO * escala) / 2
        val y0 = (maxHeight.value - ALTO * escala) / 2
        Box(
            Modifier
                .offset(x = (x0 + BOTON_X * escala).dp, y = (y0 + BOTON_Y * escala).dp)
                .size(width = (BOTON_ANCHO * escala).dp, height = (BOTON_ALTO * escala).dp)
                .clip(RoundedCornerShape(50))
                .clickable(role = Role.Button, onClickLabel = "Entrar en Fit-In") { state.finishOnboarding() }
                .semantics { contentDescription = "Comenzar" },
        )
    }
}
