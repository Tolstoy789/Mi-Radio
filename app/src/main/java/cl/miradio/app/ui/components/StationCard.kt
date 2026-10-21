package cl.miradio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.dialAndPlace
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.Background
import cl.miradio.app.ui.theme.CardBorder
import cl.miradio.app.ui.theme.CardBorderWidth
import cl.miradio.app.ui.theme.CardShape
import cl.miradio.app.ui.theme.CardSurface
import cl.miradio.app.ui.theme.LiveRed
import cl.miradio.app.ui.theme.TextPrimary
import cl.miradio.app.ui.theme.TextSecondary

/**
 * Tarjeta de una radio. Se hunde al tocarla; manteniéndola presionada ofrece
 * favorita, compartir y sitio web. La radio que suena lleva un ecualizador.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationCard(
    station: Station,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
) {
    val haptics = rememberHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    var showActions by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(if (isCurrent) Accent else CardBorder, label = "cardBorder")

    Box(modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(interactionSource)
                .clip(CardShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onLongClickLabel = stringResource(R.string.action_more_options),
                    onLongClick = {
                        haptics.longPress()
                        showActions = true
                    },
                    onClick = onClick,
                ),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(CardBorderWidth, borderColor),
        ) {
            Column(Modifier.padding(10.dp)) {
                Box {
                    StationLogo(
                        url = station.faviconUrl,
                        contentDescription = station.name,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .alpha(if (station.isUnavailable) 0.45f else 1f),
                    )
                    FavoriteToggle(
                        isFavorite = station.isFavorite,
                        onToggle = onToggleFavorite,
                        iconSize = 20.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(34.dp)
                            .background(Background.copy(alpha = 0.6f), CircleShape),
                    )
                    NowPlayingBadge(
                        visible = isCurrent,
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCurrent) Accent else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (station.isUnavailable) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SignalCellularConnectedNoInternet0Bar,
                            contentDescription = null,
                            tint = LiveRed,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.station_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = LiveRed,
                            maxLines = 1,
                        )
                    }
                } else {
                    Text(
                        text = station.dialAndPlace ?: stringResource(R.string.station_online_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        StationActionsMenu(
            station = station,
            expanded = showActions,
            onDismiss = { showActions = false },
            onToggleFavorite = onToggleFavorite,
        )
    }
}

/** Pastilla roja con el ecualizador, sobre el logo de la radio que suena. Aparece con un pequeño salto. */
@Composable
private fun NowPlayingBadge(visible: Boolean, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_now_playing)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .background(Accent, RoundedCornerShape(8.dp))
                .padding(horizontal = 7.dp, vertical = 6.dp),
        ) {
            EqualizerBars(
                animating = isPlaying,
                color = TextPrimary,
                modifier = Modifier
                    .size(12.dp)
                    .semantics { contentDescription = description },
            )
        }
    }
}

/** Menú que aparece al mantener presionada una tarjeta. */
@Composable
private fun StationActionsMenu(
    station: Station,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val context = LocalContext.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        shape = RoundedCornerShape(16.dp),
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(
                        if (station.isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite,
                    ),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = Accent,
                )
            },
            onClick = {
                onDismiss()
                onToggleFavorite()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_share)) },
            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
            onClick = {
                onDismiss()
                context.shareStation(station.name, homepage = station.homepage)
            },
        )
        val homepage = station.homepage?.takeIf { it.isNotBlank() }
        if (homepage != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_open_website)) },
                leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = null) },
                onClick = {
                    onDismiss()
                    context.openWebsite(homepage)
                },
            )
        }
    }
}

/** Tarjeta vacía con shimmer, del mismo tamaño que [StationCard]. */
@Composable
fun StationCardPlaceholder(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(CardBorderWidth, CardBorder),
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .shimmerEffect(),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(),
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.45f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(),
            )
        }
    }
}
