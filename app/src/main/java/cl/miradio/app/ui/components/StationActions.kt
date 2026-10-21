package cl.miradio.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import cl.miradio.app.R

/** Abre el selector de apps para compartir la radio y, si se conoce, la canción que suena. */
fun Context.shareStation(stationName: String, song: String? = null, homepage: String? = null) {
    val text = buildString {
        append(getString(R.string.share_listening, stationName))
        song?.let { append('\n').append(getString(R.string.share_song, it)) }
        homepage?.takeIf { it.isNotBlank() }?.let { append('\n').append(it) }
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    startActivitySafely(Intent.createChooser(send, null))
}

/** Abre el sitio web de la radio en el navegador. */
fun Context.openWebsite(url: String) {
    val address = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
    startActivitySafely(Intent(Intent.ACTION_VIEW, Uri.parse(address)))
}

private fun Context.startActivitySafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Sin una app que lo abra no hay nada más que hacer.
    }
}
