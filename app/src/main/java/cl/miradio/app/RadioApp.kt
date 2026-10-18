package cl.miradio.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

@HiltAndroidApp
class RadioApp : Application(), ImageLoaderFactory {

    @Inject lateinit var okHttpClient: OkHttpClient

    /** Coil reutiliza el cliente HTTP de la app (mismo User-Agent y pool de conexiones). */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(okHttpClient)
        .crossfade(true)
        // Muchos favicons de radios no envían cabeceras de caché; se guardan igual.
        .respectCacheHeaders(false)
        .build()
}
