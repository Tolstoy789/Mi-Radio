package cl.miradio.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.miradio.app.R
import cl.miradio.app.domain.model.StationCategory
import cl.miradio.app.ui.theme.CardBorder
import cl.miradio.app.ui.theme.CardSurface
import cl.miradio.app.ui.theme.Accent
import cl.miradio.app.ui.theme.TextPrimary
import cl.miradio.app.ui.theme.TextSecondary

@Composable
fun CategoryChips(
    selected: StationCategory,
    onSelected: (StationCategory) -> Unit,
    modifier: Modifier = Modifier,
    /** Chip extra al inicio de la fila (p. ej. el selector de región). */
    leadingContent: (@Composable () -> Unit)? = null,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leadingContent != null) item(key = "leading") { leadingContent() }
        items(StationCategory.entries, key = { it.name }) { category ->
            val isSelected = category == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(category) },
                label = { Text(stringResource(category.labelRes)) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = CardSurface,
                    labelColor = TextSecondary,
                    selectedContainerColor = Accent,
                    selectedLabelColor = TextPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = CardBorder,
                    selectedBorderColor = Accent,
                ),
            )
        }
    }
}

@get:StringRes
val StationCategory.labelRes: Int
    get() = when (this) {
        StationCategory.ALL -> R.string.category_all
        StationCategory.NEWS -> R.string.category_news
        StationCategory.POP -> R.string.category_pop
        StationCategory.ROCK -> R.string.category_rock
    }
