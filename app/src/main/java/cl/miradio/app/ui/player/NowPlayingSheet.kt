package cl.miradio.app.ui.player

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.data.settings.megabytesPerHour
import cl.miradio.app.playback.NowPlaying
import cl.miradio.app.playback.PlaybackContract
import cl.miradio.app.playback.PlayerUiState
import cl.miradio.app.ui.components.FavoriteToggle
import cl.miradio.app.ui.components.LiveBadge
import cl.miradio.app.ui.components.PlayPauseButton
import cl.miradio.app.ui.components.StationLogo
import cl.miradio.app.ui.components.rememberArtworkColor
import cl.miradio.app.ui.components.rememberHaptics
import cl.miradio.app.ui.components.shareStation
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.Background
import cl.miradio.app.ui.theme.GlassBorder
import cl.miradio.app.ui.theme.GlassSurface
import cl.miradio.app.ui.theme.Motion
import cl.miradio.app.ui.theme.PillShape
import cl.miradio.app.ui.theme.TextPrimary
import cl.miradio.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ArtworkShape = RoundedCornerShape(32.dp)

/**
 * Reproductor a pantalla completa. El fondo toma el color del logo de la radio,
 * la portada se encoge en pausa y un ecualizador con forma de cordillera
 * acompaña la transmisión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    state: PlayerUiState,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSleepTimer: (minutes: Int) -> Unit,
    isStationDown: Boolean = false,
    onPlaySimilar: () -> Unit = {},
    isDataSaverActive: Boolean = false,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val artColor by rememberArtworkColor(state.artworkUri?.toString())
    var showTimerOptions by rememberSaveable { mutableStateOf(false) }
    val timerActive = state.sleepTimerEndElapsed != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Background,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(artColor.copy(alpha = 0.7f), Background)))
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SheetHeader(
                isPlaying = state.isPlaying,
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onDismiss()
                    }
                },
                onShare = {
                    context.shareStation(state.stationName, song = state.nowPlaying?.display)
                },
            )
            Spacer(Modifier.height(12.dp))
            Artwork(
                url = state.artworkUri?.toString(),
                contentDescription = state.stationName,
                isPlaying = state.isPlaying,
                glowColor = artColor,
            )
            Spacer(Modifier.height(28.dp))
            TitleRow(
                state = state,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
            )
            Spacer(Modifier.height(16.dp))
            CordilleraVisualizer(
                isPlaying = state.isPlaying,
                color = artColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            )
            AnimatedContent(
                targetState = state.nowPlaying.takeIf { state.error == null },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "nowPlaying",
            ) { nowPlaying ->
                if (nowPlaying != null) {
                    NowPlayingCard(nowPlaying, accent = artColor, modifier = Modifier.padding(top = 16.dp))
                }
            }
            if (isStationDown) {
                Button(
                    onClick = onPlaySimilar,
                    modifier = Modifier.padding(top = 16.dp),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = TextPrimary),
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.similar_action))
                }
            }
            Spacer(Modifier.height(24.dp))
            PlaybackControls(
                state = state,
                glowColor = artColor,
                timerHighlighted = timerActive || showTimerOptions,
                onToggleTimerOptions = { showTimerOptions = !showTimerOptions },
                onTogglePlayPause = onTogglePlayPause,
                onPlaySimilar = onPlaySimilar,
            )
            Spacer(Modifier.height(20.dp))
            VolumeControl(volume = state.volume, onVolumeChange = onVolumeChange)
            AnimatedVisibility(
                visible = showTimerOptions || timerActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                SleepTimerControl(
                    endElapsed = state.sleepTimerEndElapsed,
                    onSleepTimer = onSleepTimer,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            DataConsumptionLine(bitrateKbps = state.bitrateKbps, isDataSaverActive = isDataSaverActive)
        }
    }
}

/** Asa para arrastrar, botón para cerrar, "EN VIVO" y compartir. */
@Composable
private fun SheetHeader(isPlaying: Boolean, onClose: () -> Unit, onShare: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .padding(top = 10.dp)
                .size(width = 36.dp, height = 4.dp)
                .background(TextPrimary.copy(alpha = 0.3f), PillShape),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.action_close_player),
                    tint = TextPrimary,
                    modifier = Modifier.size(30.dp),
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                LiveBadge(pulsing = isPlaying)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.action_share), tint = TextPrimary)
            }
        }
    }
}

/** Portada grande con un halo del color de la radio. En pausa se encoge un poco. */
@Composable
private fun Artwork(url: String?, contentDescription: String, isPlaying: Boolean, glowColor: Color) {
    val scale by animateFloatAsState(if (isPlaying) 1f else 0.86f, Motion.gentle, label = "artworkScale")
    StationLogo(
        url = url,
        contentDescription = contentDescription,
        shape = ArtworkShape,
        modifier = Modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth(0.8f)
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation = 32.dp, shape = ArtworkShape, ambientColor = glowColor, spotColor = glowColor),
        fallbackIconSize = 96.dp,
    )
}

/** Nombre de la radio, estado (dial, error o reconexión) y el corazón de favoritas. */
@Composable
private fun TitleRow(state: PlayerUiState, isFavorite: Boolean, onToggleFavorite: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = state.stationName,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            playerStatusText(state, includeNowPlaying = false)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.error != null) Accent else TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FavoriteToggle(
            isFavorite = isFavorite,
            onToggle = onToggleFavorite,
            iconSize = 28.dp,
            modifier = Modifier.size(52.dp),
        )
    }
}

/** Temporizador, Play/Pause y "otra radio parecida". */
@Composable
private fun PlaybackControls(
    state: PlayerUiState,
    glowColor: Color,
    timerHighlighted: Boolean,
    onToggleTimerOptions: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlaySimilar: () -> Unit,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundAction(
            icon = Icons.Rounded.Bedtime,
            contentDescription = stringResource(R.string.sleep_timer_title),
            highlighted = timerHighlighted,
            onClick = {
                haptics.tap()
                onToggleTimerOptions()
            },
        )
        PlayPauseButton(
            isPlaying = state.isPlaying,
            isLoading = state.showLoading,
            onClick = onTogglePlayPause,
            size = 80.dp,
            glowColor = glowColor,
        )
        RoundAction(
            icon = Icons.Rounded.Shuffle,
            contentDescription = stringResource(R.string.action_similar_station),
            highlighted = false,
            onClick = {
                haptics.tap()
                onPlaySimilar()
            },
        )
    }
}

@Composable
private fun RoundAction(icon: ImageVector, contentDescription: String, highlighted: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (highlighted) Accent.copy(alpha = 0.22f) else GlassSurface,
        label = "roundActionContainer",
    )
    val tint by animateColorAsState(if (highlighted) Accent else TextPrimary, label = "roundActionTint")
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .background(container, CircleShape),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

/** Canción en curso. Tocarla la busca en la web para saber más o guardarla. */
@Composable
private fun NowPlayingCard(nowPlaying: NowPlaying, accent: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        onClick = {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, nowPlaying.display)
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                // Sin buscador instalado: la tarjeta queda solo informativa.
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = GlassSurface,
        border = BorderStroke(1.dp, GlassBorder),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.now_playing_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Text(
                    text = nowPlaying.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                nowPlaying.artist?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                Icons.Rounded.Search,
                contentDescription = stringResource(R.string.action_search_song),
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** "≈ 58 MB por hora · Ahorro de datos activo", según el bitrate de la radio. */
@Composable
private fun DataConsumptionLine(bitrateKbps: Int?, isDataSaverActive: Boolean) {
    val parts = listOfNotNull(
        bitrateKbps?.let { stringResource(R.string.data_per_hour, megabytesPerHour(it)) },
        if (isDataSaverActive) stringResource(R.string.data_saver_active_short) else null,
    )
    if (parts.isEmpty()) return
    Row(
        modifier = Modifier.padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Rounded.DataSaverOn,
            contentDescription = null,
            tint = if (isDataSaverActive) Accent else TextSecondary,
            modifier = Modifier.size(16.dp),
        )
        Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun VolumeControl(volume: Float, onVolumeChange: (Float) -> Unit) {
    // Estado local para que el slider responda al instante mientras se arrastra.
    var sliderValue by remember { mutableFloatStateOf(volume) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(volume) { if (!dragging) sliderValue = volume }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Rounded.VolumeDown, contentDescription = null, tint = TextSecondary)
        Slider(
            value = sliderValue,
            onValueChange = {
                dragging = true
                sliderValue = it
                onVolumeChange(it)
            },
            onValueChangeFinished = { dragging = false },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = TextPrimary,
                activeTrackColor = TextPrimary,
                inactiveTrackColor = TextPrimary.copy(alpha = 0.2f),
            ),
        )
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = stringResource(R.string.cd_volume), tint = TextSecondary)
    }
}

@Composable
private fun SleepTimerControl(endElapsed: Long?, onSleepTimer: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    var remainingMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(endElapsed) {
        while (endElapsed != null) {
            remainingMs = (endElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            delay(1_000L - remainingMs % 1_000L)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Icons.Rounded.Bedtime,
                contentDescription = null,
                tint = if (endElapsed != null) Accent else TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = if (endElapsed != null) {
                    stringResource(R.string.sleep_timer_remaining, formatRemaining(remainingMs))
                } else {
                    stringResource(R.string.sleep_timer_title)
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (endElapsed != null) Accent else TextSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PlaybackContract.SLEEP_TIMER_OPTIONS_MINUTES.forEach { minutes ->
                FilterChip(
                    selected = false,
                    onClick = {
                        haptics.tick()
                        onSleepTimer(minutes)
                    },
                    label = { Text(stringResource(R.string.sleep_timer_option, minutes)) },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = GlassSurface,
                        labelColor = TextPrimary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = GlassBorder),
                )
            }
            if (endElapsed != null) {
                TextButton(onClick = { onSleepTimer(0) }) {
                    Text(stringResource(R.string.sleep_timer_cancel), color = Accent)
                }
            }
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalSeconds = (ms + 999) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
