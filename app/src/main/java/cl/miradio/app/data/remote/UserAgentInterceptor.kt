package cl.miradio.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/** Agrega el `User-Agent` que Radio Browser exige para identificar a la app. */
class UserAgentInterceptor(private val userAgent: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", userAgent)
                .build(),
        )
}
