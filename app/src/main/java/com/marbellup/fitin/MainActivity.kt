package com.marbellup.fitin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.marbellup.fitin.ui.screens.AiSearchScreen
import com.marbellup.fitin.ui.screens.CalendarScreen
import com.marbellup.fitin.ui.screens.CelebrationScreen
import com.marbellup.fitin.ui.screens.ChatScreen
import com.marbellup.fitin.ui.screens.CompanyScreen
import com.marbellup.fitin.ui.screens.CouncilScreen
import com.marbellup.fitin.ui.screens.CreateScreen
import com.marbellup.fitin.ui.screens.HomeScreen
import com.marbellup.fitin.ui.screens.NotificationsScreen
import com.marbellup.fitin.ui.screens.ProfileScreen
import com.marbellup.fitin.ui.screens.WelcomeScreen
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
        setContent {
            FitInTheme {
                val density = LocalDensity.current
                val scaled = Density(density.density, density.fontScale * if (state.bigText) 1.3f else 1f)
                CompositionLocalProvider(
                    LocalDensity provides scaled,
                    LocalEasyRead provides state.easyRead,
                ) { App(state) }
            }
        }
    }
}

@Composable
private fun App(state: AppState) {
    BackHandler(enabled = state.steps.isNotEmpty()) { state.back() }

    // Comprueba el servidor al arrancar y consulta avisos cada 20 s (invitaciones del smart grouping).
    LaunchedEffect(Unit) {
        state.checkServer()
        while (true) {
            delay(20_000)
            if (state.engine != null && state.engine != "Sin conexión") state.refreshNotifications()
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
        when {
            !state.onboarded && step == null -> WelcomeScreen(state)
            step is Step.AiSearch -> AiSearchScreen(state)
            step is Step.Detail -> ActivityScreen(state, step.activityId)
            step is Step.Company -> CompanyScreen(state, step.activityId)
            step is Step.Confirmed -> CelebrationScreen(state, step.activityId)
            step is Step.Create -> CreateScreen(state)
            step is Step.Notifications -> NotificationsScreen(state)
            step is Step.Council -> CouncilScreen(state, onBack = { state.back() })
            state.tab == Tab.Inicio -> HomeScreen(state, tabs)
            state.tab == Tab.Calendario -> CalendarScreen(state, tabs)
            state.tab == Tab.Chat -> ChatScreen(state, tabs)
            else -> ProfileScreen(state, tabs)
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
                    onOpen = { state.banner = null; state.open(Step.Notifications) },
                    onDismiss = { state.banner = null },
                )
            }
        }
    }
}
