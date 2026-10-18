package cl.miradio.app

import android.app.Application
import cl.miradio.app.data.settings.DataSaverSettings
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.intercept.Interceptor
import coil.request.CachePolicy
import coil.request.ImageResult
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

@HiltAndroidApp
class RadioApp : Application(), ImageLoaderFactory {

    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var dataSaverSettings: DataSaverSettings

    /** Coil reutiliza el cliente HTTP de la app (mismo User-Agent y pool de conexiones). */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(okHttpClient)
        .crossfade(true)
        // Muchos favicons de radios no envían cabeceras de caché; se guardan igual.
        .respectCacheHeaders(false)
        .components {
            // Con ahorro de datos activo, los logos salen solo de la caché; si no
            // están guardados, la tarjeta muestra el ícono de radio.
            add(DataSaverImageInterceptor())
        }
        .build()

    private inner class DataSaverImageInterceptor : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val request = if (dataSaverSettings.isActiveNow()) {
                chain.request.newBuilder().networkCachePolicy(CachePolicy.DISABLED).build()
            } else {
                chain.request
            }
            return chain.proceed(request)
        }
    }
}
