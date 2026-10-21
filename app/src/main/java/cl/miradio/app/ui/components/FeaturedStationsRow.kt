package cl.miradio.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.domain.model.Station
import cl.miradio.app.domain.model.dialAndPlace
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.CardSurface
import cl.miradio.app.ui.theme.GlassBorder
import cl.miradio.app.ui.theme.TextPrimary

private val FeaturedCardShape = RoundedCornerShape(24.dp)

/** Carrusel "Populares en Chile": tarjetas grandes teñidas con el color de cada logo. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeaturedStationsRow(
    stations: List<Station>,
    currentStationId: String?,
    isPlaying: Boolean,
    onStationClick: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Column(modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.featured_title))
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            modifier = Modifier.bleedHorizontally(GridHorizontalPadding),
            contentPadding = PaddingValues(horizontal = GridHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(stations, key = { _, station -> station.id }) { index, station ->
                val isCurrent = station.id == currentStationId
                FeaturedStationCard(
                    station = station,
                    rank = index + 1,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onClick = { onStationClick(station) },
                )
            }
        }
    }
}

@Composable
private fun FeaturedStationCard(
    station: Station,
    rank: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val color by rememberArtworkColor(station.faviconUrl)
    val interactionSource = remember { MutableInteractionSource() }
    val border = if (isCurrent) BorderStroke(1.5.dp, Accent) else BorderStroke(1.dp, GlassBorder)

    Box(
        modifier = Modifier
            .width(272.dp)
            .height(132.dp)
            .pressScale(interactionSource)
            .clip(FeaturedCardShape)
            .background(
                Brush.horizontalGradient(
                    listOf(color, color.copy(alpha = 0.3f).compositeOver(CardSurface)),
                ),
            )
            .border(border, FeaturedCardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StationLogo(
                url = station.faviconUrl,
                contentDescription = station.name,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(108.dp),
                fallbackIconSize = 44.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Text(
                    text = stringResource(R.string.featured_rank, rank),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = station.dialAndPlace ?: stringResource(R.string.station_online_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .size(30.dp)
                        .background(if (isCurrent) Accent else TextPrimary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCurrent) {
                        EqualizerBars(animating = isPlaying, color = TextPrimary, modifier = Modifier.size(13.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = CardSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
