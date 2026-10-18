package cl.miradio.app.ui.player

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.playback.NowPlaying
import cl.miradio.app.playback.PlaybackContract
import cl.miradio.app.playback.PlayerUiState
import cl.miradio.app.ui.components.LiveBadge
import cl.miradio.app.ui.components.PlayPauseButton
import cl.miradio.app.ui.components.StationLogo
import cl.miradio.app.ui.theme.CardBorder
import cl.miradio.app.ui.theme.CardSurface
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.MiniPlayerSurface
import cl.miradio.app.ui.theme.TextPrimary
import cl.miradio.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    state: PlayerUiState,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSleepTimer: (minutes: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StationLogo(
                url = state.artworkUri?.toString(),
                contentDescription = state.stationName,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f),
                fallbackIconSize = 96.dp,
            )
            Spacer(Modifier.height(24.dp))
            LiveBadge(pulsing = state.isPlaying)
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.stationName,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            playerStatusText(state, includeNowPlaying = false)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AnimatedContent(
                targetState = state.nowPlaying.takeIf { state.error == null },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "nowPlaying",
            ) { nowPlaying ->
                if (nowPlaying != null) {
                    NowPlayingCard(nowPlaying, modifier = Modifier.padding(top = 16.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            PlayPauseButton(
                isPlaying = state.isPlaying,
                isLoading = state.showLoading,
                onClick = onTogglePlayPause,
                size = 72.dp,
            )
            Spacer(Modifier.height(24.dp))
            VolumeControl(volume = state.volume, onVolumeChange = onVolumeChange)
            Spacer(Modifier.height(16.dp))
            SleepTimerControl(endElapsed = state.sleepTimerEndElapsed, onSleepTimer = onSleepTimer)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Canción en curso. Tocarla la busca en la web para saber más o guardarla. */
@Composable
private fun NowPlayingCard(nowPlaying: NowPlaying, modifier: Modifier = Modifier) {
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
        shape = RoundedCornerShape(16.dp),
        color = MiniPlayerSurface,
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Accent)
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
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = CardBorder,
            ),
        )
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = stringResource(R.string.cd_volume), tint = TextSecondary)
    }
}

@Composable
private fun SleepTimerControl(endElapsed: Long?, onSleepTimer: (Int) -> Unit) {
    var remainingMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(endElapsed) {
        while (endElapsed != null) {
            remainingMs = (endElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            delay(1_000L - remainingMs % 1_000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
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
                    onClick = { onSleepTimer(minutes) },
                    label = { Text(stringResource(R.string.sleep_timer_option, minutes)) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MiniPlayerSurface,
                        labelColor = TextPrimary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = CardBorder),
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
