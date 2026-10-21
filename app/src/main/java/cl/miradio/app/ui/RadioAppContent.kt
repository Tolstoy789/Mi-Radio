package cl.miradio.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.miradio.app.R
import cl.miradio.app.domain.model.Station
import cl.miradio.app.ui.components.rememberArtworkColor
import cl.miradio.app.ui.components.rememberHaptics
import cl.miradio.app.ui.favorites.FavoritesRoute
import cl.miradio.app.ui.home.HomeRoute
import cl.miradio.app.ui.navigation.RadioDestination
import cl.miradio.app.ui.player.FloatingMiniPlayer
import cl.miradio.app.ui.player.NowPlayingSheet
import cl.miradio.app.ui.player.PlayerMessage
import cl.miradio.app.ui.player.PlayerViewModel
import cl.miradio.app.ui.settings.DataSaverSheet
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.AndesBlue
import cl.miradio.app.ui.theme.Background
import cl.miradio.app.ui.theme.GlassBorder
import cl.miradio.app.ui.theme.MiniPlayerSurface
import cl.miradio.app.ui.theme.Motion
import cl.miradio.app.ui.theme.TextPrimary
import cl.miradio.app.ui.theme.TextSecondary

/** Raíz de la UI: navegación inferior, mini-player flotante y hoja Now Playing. */
@Composable
fun RadioAppContent(playerViewModel: PlayerViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()
    val isStationDown by playerViewModel.isStationDown.collectAsStateWithLifecycle()
    val isDataSaverActive by playerViewModel.isDataSaverActive.collectAsStateWithLifecycle()
    val isFavorite by playerViewModel.isFavorite.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showDataSaver by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    // El resplandor del fondo toma el color de la radio que suena.
    val glow = rememberArtworkColor(playerState.artworkUri?.toString(), fallback = AndesBlue)

    LaunchedEffect(playerViewModel) {
        playerViewModel.messages.collect { message ->
            val text = when (message) {
                PlayerMessage.NoSimilarStation -> context.getString(R.string.similar_not_found)
                is PlayerMessage.SwitchedTo -> context.getString(R.string.similar_switched, message.stationName)
            }
            snackbarHostState.showSnackbar(text)
        }
    }

    val play = rememberPlayWithNotificationPermission(playerViewModel::play)

    Box(
        Modifier
            .fillMaxSize()
            .auroraBackground(glow),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MiniPlayerSurface,
                        contentColor = TextPrimary,
                        actionColor = Accent,
                    )
                }
            },
            bottomBar = {
                Column {
                    AnimatedVisibility(
                        visible = playerState.hasStation,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
                        FloatingMiniPlayer(
                            state = playerState,
                            onClick = { showNowPlaying = true },
                            onTogglePlayPause = playerViewModel::togglePlayPause,
                        )
                    }
                    RadioNavigationBar(
                        currentRoute = navController.currentBackStackEntryAsState().value?.destination,
                        onNavigate = { destination ->
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = RadioDestination.HOME.route,
                // Las barras superiores de cada pantalla no deben volver a sumar la barra de estado.
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
                enterTransition = { fadeIn(tween(durationMillis = 260)) + scaleIn(tween(durationMillis = 260), initialScale = 0.97f) },
                exitTransition = { fadeOut(tween(durationMillis = 180)) },
                popEnterTransition = { fadeIn(tween(durationMillis = 260)) + scaleIn(tween(durationMillis = 260), initialScale = 0.97f) },
                popExitTransition = { fadeOut(tween(durationMillis = 180)) },
            ) {
                composable(RadioDestination.HOME.route) {
                    HomeRoute(
                        currentStationId = playerState.stationId,
                        isPlaying = playerState.isPlaying,
                        onPlay = play,
                        snackbarHostState = snackbarHostState,
                        onOpenDataSaver = { showDataSaver = true },
                    )
                }
                composable(RadioDestination.FAVORITES.route) {
                    FavoritesRoute(
                        currentStationId = playerState.stationId,
                        isPlaying = playerState.isPlaying,
                        onPlay = play,
                    )
                }
            }
        }
    }

    if (showNowPlaying && playerState.hasStation) {
        NowPlayingSheet(
            state = playerState,
            onDismiss = { showNowPlaying = false },
            onTogglePlayPause = playerViewModel::togglePlayPause,
            onVolumeChange = playerViewModel::setVolume,
            onSleepTimer = playerViewModel::setSleepTimer,
            isStationDown = isStationDown,
            onPlaySimilar = playerViewModel::playSimilar,
            isDataSaverActive = isDataSaverActive,
            isFavorite = isFavorite,
            onToggleFavorite = playerViewModel::toggleFavorite,
        )
    }

    if (showDataSaver) {
        DataSaverSheet(onDismiss = { showDataSaver = false })
    }
}

/**
 * Fondo azul noche con dos resplandores suaves arriba: uno del color de la radio
 * que suena y otro rojo de la marca. Se dibuja sin recomponer al cambiar el color.
 */
private fun Modifier.auroraBackground(glow: State<Color>): Modifier = drawBehind {
    drawRect(Background)
    val glowCenter = Offset(size.width * 0.1f, 0f)
    val glowRadius = size.width * 1.1f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glow.value.copy(alpha = 0.45f), Color.Transparent),
            center = glowCenter,
            radius = glowRadius,
        ),
        radius = glowRadius,
        center = glowCenter,
    )
    val accentCenter = Offset(size.width, size.height * 0.08f)
    val accentRadius = size.width * 0.75f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Accent.copy(alpha = 0.12f), Color.Transparent),
            center = accentCenter,
            radius = accentRadius,
        ),
        radius = accentRadius,
        center = accentCenter,
    )
}

@Composable
private fun RadioNavigationBar(
    currentRoute: NavDestination?,
    onNavigate: (RadioDestination) -> Unit,
) {
    val haptics = rememberHaptics()
    NavigationBar(
        containerColor = Background,
        modifier = Modifier.drawBehind {
            drawLine(GlassBorder, start = Offset.Zero, end = Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
        },
    ) {
        RadioDestination.entries.forEach { destination ->
            val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) haptics.tick()
                    onNavigate(destination)
                },
                icon = {
                    BouncyNavIcon(
                        selected = selected,
                        icon = if (selected) destination.selectedIcon else destination.unselectedIcon,
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Accent,
                    selectedTextColor = Accent,
                    indicatorColor = Accent.copy(alpha = 0.16f),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                ),
            )
        }
    }
}

/** Ícono de pestaña que rebota al elegirla (no al abrir la app). */
@Composable
private fun BouncyNavIcon(selected: Boolean, icon: ImageVector) {
    val scale = remember { Animatable(1f) }
    var initial by remember { mutableStateOf(true) }
    LaunchedEffect(selected) {
        if (initial) {
            initial = false
            return@LaunchedEffect
        }
        if (selected) {
            scale.animateTo(0.75f, tween(durationMillis = 90))
            scale.animateTo(1f, Motion.bouncy)
        }
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
    )
}

/**
 * En Android 13+ la notificación multimedia necesita permiso. Se pide la primera
 * vez que el usuario toca una radio; la reproducción arranca igual si lo rechaza.
 */
@Composable
private fun rememberPlayWithNotificationPermission(play: (Station) -> Unit): (Station) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var asked by rememberSaveable { mutableStateOf(false) }
    return { station ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !asked &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        play(station)
    }
}
