package cl.miradio.app.data.remote

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Radio Browser es una red de servidores espejo con los mismos datos.
 * Si el servidor actual no responde o devuelve 5xx, reintenta la misma
 * petición en el siguiente espejo y recuerda el que funcionó.
 *
 * Solo reescribe peticiones dirigidas a alguno de [mirrors]; el resto pasa intacto.
 */
class MirrorFailoverInterceptor(private val mirrors: List<String>) : Interceptor {

    init {
        require(mirrors.isNotEmpty()) { "Se necesita al menos un servidor espejo" }
    }

    private val preferred = AtomicInteger(0)

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.url.host !in mirrors) return chain.proceed(original)

        val start = preferred.get()
        var lastError: IOException? = null

        for (offset in mirrors.indices) {
            val index = (start + offset) % mirrors.size
            val request = original.newBuilder()
                .url(original.url.newBuilder().host(mirrors[index]).build())
                .build()
            try {
                val response = chain.proceed(request)
                val isLastMirror = offset == mirrors.lastIndex
                if (response.code < 500 || isLastMirror) {
                    preferred.set(index)
                    return response
                }
                response.close()
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Ningún servidor de Radio Browser respondió")
    }

    companion object {
        val DEFAULT_MIRRORS = listOf(
            "de1.api.radio-browser.info",
            "de2.api.radio-browser.info",
            "fi1.api.radio-browser.info",
        )
    }
}
