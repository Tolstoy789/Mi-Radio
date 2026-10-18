package cl.miradio.app.ui.settings

import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.miradio.app.R
import cl.miradio.app.data.settings.DataSaverMode
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.CardBorder
import cl.miradio.app.ui.theme.CardSurface
import cl.miradio.app.ui.theme.MiniPlayerSurface
import cl.miradio.app.ui.theme.TextPrimary
import cl.miradio.app.ui.theme.TextSecondary

/** Hoja "Ahorro de datos": cuándo activarlo y cuánto consumió la radio este mes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSaverSheet(
    onDismiss: () -> Unit,
    viewModel: DataSaverViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.data_saver_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Text(
                text = stringResource(
                    if (state.isActive) R.string.data_saver_status_on else R.string.data_saver_status_off,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isActive) Accent else TextSecondary,
            )

            Column(Modifier.selectableGroup()) {
                DataSaverMode.entries.forEach { mode ->
                    ModeRow(
                        mode = mode,
                        isSelected = mode == state.mode,
                        onClick = { viewModel.onModeSelected(mode) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.data_saver_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            Text(
                text = stringResource(R.string.data_usage_title),
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UsageTile(
                    icon = Icons.Rounded.SignalCellularAlt,
                    label = stringResource(R.string.data_usage_mobile),
                    value = Formatter.formatShortFileSize(context, state.usage.mobileBytes),
                    highlighted = state.isMetered,
                    modifier = Modifier.weight(1f),
                )
                UsageTile(
                    icon = Icons.Rounded.Wifi,
                    label = stringResource(R.string.data_usage_wifi),
                    value = Formatter.formatShortFileSize(context, state.usage.wifiBytes),
                    highlighted = !state.isMetered,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ModeRow(mode: DataSaverMode, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = Accent, unselectedColor = TextSecondary),
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text(stringResource(mode.labelRes), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(stringResource(mode.descriptionRes), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun UsageTile(
    icon: ImageVector,
    label: String,
    value: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MiniPlayerSurface,
        border = BorderStroke(1.dp, if (highlighted) Accent else CardBorder),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = if (highlighted) Accent else TextSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

private val DataSaverMode.labelRes: Int
    get() = when (this) {
        DataSaverMode.OFF -> R.string.data_saver_mode_off
        DataSaverMode.MOBILE_ONLY -> R.string.data_saver_mode_mobile
        DataSaverMode.ALWAYS -> R.string.data_saver_mode_always
    }

private val DataSaverMode.descriptionRes: Int
    get() = when (this) {
        DataSaverMode.OFF -> R.string.data_saver_mode_off_desc
        DataSaverMode.MOBILE_ONLY -> R.string.data_saver_mode_mobile_desc
        DataSaverMode.ALWAYS -> R.string.data_saver_mode_always_desc
    }
