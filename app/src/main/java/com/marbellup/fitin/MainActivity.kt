package com.marbellup.fitin

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.ui.FitInBottomBar
import com.marbellup.fitin.ui.LocalEasyRead
import com.marbellup.fitin.ui.NotificationBanner
import com.marbellup.fitin.ui.screens.ActivityScreen
import com.marbellup.fitin.ui.screens.SearchScreen
import com.marbellup.fitin.ui.screens.AiSearchScreen
import com.marbellup.fitin.ui.screens.CalendarScreen
import com.marbellup.fitin.ui.screens.CelebrationScreen
import com.marbellup.fitin.ui.screens.CompanyScreen
import com.marbellup.fitin.ui.screens.CouncilScreen
import com.marbellup.fitin.ui.screens.CreateScreen
import com.marbellup.fitin.ui.screens.ChatScreen
import com.marbellup.fitin.ui.screens.CuentanosloScreen
import com.marbellup.fitin.ui.screens.QueTalFueScreen
import com.marbellup.fitin.ui.screens.HomeScreen
import com.marbellup.fitin.ui.screens.ModelScreen
import com.marbellup.fitin.ui.screens.NotificationsScreen
import com.marbellup.fitin.ui.screens.ProfileScreen
import com.marbellup.fitin.ui.screens.WelcomeScreen
import com.marbellup.fitin.notify.Alerts
import com.marbellup.fitin.ui.theme.FitInTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val state: AppState by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fondo siempre claro: iconos de las barras del sistema en oscuro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        // La IA revisa actividades nuevas cada 6 h aunque la app esté cerrada.
        Alerts.schedule(this)
        handleIntent(intent)
        setContent {
            FitInTheme(easyRead = state.easyRead) {
                val density = LocalDensity.current
                val scaled = Density(density.density, density.fontScale * if (state.bigText) 1.3f else 1f)
                CompositionLocalProvider(
                    LocalDensity provides scaled,
                    LocalEasyRead provides state.easyRead,
                ) { App(state) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Al tocar una notificación se abre la ficha de esa actividad. */
    private fun handleIntent(intent: Intent?) {
        val id = intent?.getStringExtra(Alerts.EXTRA_ACTIVITY) ?: return
        intent.removeExtra(Alerts.EXTRA_ACTIVITY)
        state.openFromNotification(id)
    }
}

@Composable
private fun App(state: AppState) {
    BackHandler(enabled = state.steps.isNotEmpty()) { state.back() }

    // Permiso de notificaciones (Android 13+) y primera revisión de actividades que encajan.
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(state.onboarded) {
        if (state.onboarded) {
            if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            delay(1_500)
            state.checkForMatches()
        }
    }
    // El aviso flotante se oculta solo.
    LaunchedEffect(state.banner) {
        if (state.banner != null) {
            delay(7_000)
            state.banner = null
        }
    }

    val tabs: @Composable () -> Unit = { FitInBottomBar(state) }

    Box(Modifier.fillMaxSize()) {
        val step = state.steps.lastOrNull()
        // Qué pantalla toca y a qué «profundidad» está: así se sabe si se avanza, se vuelve o se cambia de pestaña.
        val destino = when {
            !state.onboarded && step == null -> Destino(BIENVENIDA, -1, 0)
            step != null -> Destino(step, state.steps.size, 0)
            else -> Destino(state.tab, 0, state.tab.ordinal)
        }
        val quieto = com.marbellup.fitin.ui.rememberReducedMotion()
        androidx.compose.animation.AnimatedContent(
            targetState = destino,
            transitionSpec = { transicion(initialState, targetState, quieto) },
            label = "pantallas",
        ) { d ->
            // Se pinta siempre a partir del destino de la animación (d), no del estado de fuera.
            when (val p = d.pantalla) {
                BIENVENIDA -> WelcomeScreen(state)
                is Step.AiSearch -> AiSearchScreen(state)
                is Step.Detail -> ActivityScreen(state, p.activityId)
                is Step.Company -> CompanyScreen(state, p.activityId)
                is Step.Confirmed -> CelebrationScreen(state, p.activityId)
                is Step.Create -> CreateScreen(state)
                is Step.Notifications -> NotificationsScreen(state)
                is Step.Council -> CouncilScreen(state, onBack = { state.back() })
                is Step.Model -> ModelScreen(state)
                is Step.Search -> SearchScreen(state)
                is Step.Cuentanoslo -> CuentanosloScreen(state)
                is Step.QueTalFue -> QueTalFueScreen(state, p.activityId)
                Tab.Inicio -> HomeScreen(state, tabs)
                Tab.Calendario -> CalendarScreen(state, tabs)
                Tab.ParaTi -> ChatScreen(tabs)
                else -> ProfileScreen(state, tabs)
            }
        }

        AnimatedVisibility(
            visible = state.banner != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp),
        ) {
            val n = state.banner
            if (n != null) {
                NotificationBanner(
                    n,
                    onOpen = { state.openNotification(n) },
                    onDismiss = { state.banner = null },
                )
            }
        }
    }
}

private const val BIENVENIDA = "bienvenida"

/** Pantalla que se muestra, con su profundidad en la pila y su orden entre pestañas. */
private data class Destino(val pantalla: Any, val nivel: Int, val orden: Int)

/**
 * Cómo se pasa de una pantalla a otra:
 * - avanzar (abrir ficha, reservar…): la nueva entra desde la derecha con un muelle;
 * - volver: al revés;
 * - cambiar de pestaña: fundido rápido (la barra de abajo no se mueve);
 * - de la bienvenida a la app: zoom suave.
 */
private fun androidx.compose.animation.AnimatedContentTransitionScope<Destino>.transicion(
    desde: Destino, hacia: Destino, quieto: Boolean,
): androidx.compose.animation.ContentTransform {
    if (quieto) return androidx.compose.animation.ContentTransform(androidx.compose.animation.EnterTransition.None, androidx.compose.animation.ExitTransition.None)
    val muelle = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.82f, stiffness = 380f)
    val entra = androidx.compose.animation.core.tween<Float>(260)
    val sale = androidx.compose.animation.core.tween<Float>(160)
    return when {
        desde.pantalla == BIENVENIDA ->
            (fadeIn(androidx.compose.animation.core.tween(420)) + androidx.compose.animation.scaleIn(androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 300f), initialScale = 0.92f)) togetherWith
                (fadeOut(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleOut(targetScale = 1.08f))
        hacia.nivel > desde.nivel ->
            (androidx.compose.animation.slideInHorizontally(muelle) { it / 3 } + fadeIn(entra)) togetherWith
                (androidx.compose.animation.slideOutHorizontally(muelle) { -it / 8 } + fadeOut(sale))
        hacia.nivel < desde.nivel ->
            (androidx.compose.animation.slideInHorizontally(muelle) { -it / 8 } + fadeIn(entra)) togetherWith
                (androidx.compose.animation.slideOutHorizontally(muelle) { it / 3 } + fadeOut(sale))
        else -> fadeIn(androidx.compose.animation.core.tween(220)) togetherWith fadeOut(androidx.compose.animation.core.tween(120))
    }
}
