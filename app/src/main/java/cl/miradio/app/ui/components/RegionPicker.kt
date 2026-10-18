package cl.miradio.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.domain.model.ChileRegion
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.CardBorder
import cl.miradio.app.ui.theme.CardSurface
import cl.miradio.app.ui.theme.TextPrimary
import cl.miradio.app.ui.theme.TextSecondary

/** Chip "Todo Chile ▾" que abre la lista de regiones de norte a sur. */
@Composable
fun RegionChip(
    selected: ChileRegion?,
    regions: List<ChileRegion>,
    onSelected: (ChileRegion?) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val isSelected = selected != null

    FilterChip(
        selected = isSelected,
        onClick = { showPicker = true },
        enabled = regions.isNotEmpty() || isSelected,
        label = { Text(selected?.displayName ?: stringResource(R.string.region_all)) },
        leadingIcon = { Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(18.dp)) },
        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = CardSurface,
            labelColor = TextSecondary,
            iconColor = TextSecondary,
            selectedContainerColor = Accent,
            selectedLabelColor = TextPrimary,
            selectedLeadingIconColor = TextPrimary,
            selectedTrailingIconColor = TextPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = CardBorder,
            selectedBorderColor = Accent,
        ),
    )

    if (showPicker) {
        RegionPickerSheet(
            selected = selected,
            regions = regions,
            onSelected = {
                onSelected(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionPickerSheet(
    selected: ChileRegion?,
    regions: List<ChileRegion>,
    onSelected: (ChileRegion?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardSurface,
    ) {
        Text(
            text = stringResource(R.string.region_picker_title),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.navigationBarsPadding()) {
            item(key = "all") {
                RegionRow(stringResource(R.string.region_all), isSelected = selected == null) { onSelected(null) }
            }
            items(regions, key = { it.name }) { region ->
                RegionRow(region.displayName, isSelected = region == selected) { onSelected(region) }
            }
        }
    }
}

@Composable
private fun RegionRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) Accent else TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Accent)
    }
}
