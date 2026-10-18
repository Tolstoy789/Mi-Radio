package cl.miradio.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector
import cl.miradio.app.R

enum class RadioDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("home", R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    FAVORITES("favorites", R.string.nav_favorites, Icons.Rounded.Favorite, Icons.Rounded.FavoriteBorder),
}
