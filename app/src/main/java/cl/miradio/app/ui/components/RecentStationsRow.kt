package cl.miradio.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.domain.model.Station
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.TextSecondary

private val RecentRingShape = RoundedCornerShape(20.dp)
private val RecentLogoShape = RoundedCornerShape(16.dp)

/** Fila horizontal "Escuchadas recientemente", sobre la grilla de Inicio. */
@Composable
fun RecentStationsRow(
    stations: List<Station>,
    currentStationId: String?,
    onStationClick: (Station) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        SectionTitle(
            text = stringResource(R.string.recent_title),
            actionLabel = stringResource(R.string.recent_clear),
            onAction = onClear,
        )
        LazyRow(
            modifier = Modifier.bleedHorizontally(GridHorizontalPadding),
            contentPadding = PaddingValues(horizontal = GridHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(stations, key = { it.id }) { station ->
                RecentStation(
                    station = station,
                    isCurrent = station.id == currentStationId,
                    onClick = { onStationClick(station) },
                )
            }
        }
    }
}

@Composable
private fun RecentStation(station: Station, isCurrent: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val ringColor by animateColorAsState(if (isCurrent) Accent else Color.Transparent, label = "recentRing")
    Column(
        modifier = Modifier
            .width(80.dp)
            .pressScale(interactionSource, pressedScale = 0.92f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StationLogo(
            url = station.faviconUrl,
            contentDescription = station.name,
            shape = RecentLogoShape,
            modifier = Modifier
                .size(72.dp)
                .border(2.dp, ringColor, RecentRingShape)
                .padding(4.dp),
            fallbackIconSize = 28.dp,
        )
        Text(
            text = station.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent) Accent else TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
