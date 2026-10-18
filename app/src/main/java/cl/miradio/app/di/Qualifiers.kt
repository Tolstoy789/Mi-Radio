package cl.miradio.app.di

import javax.inject.Qualifier

/** Cliente HTTP para la API de Radio Browser (failover entre espejos, timeouts cortos). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiHttpClient

/** Cliente HTTP para los streams de audio (sin interceptores de la API, lecturas largas). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StreamingHttpClient

/** Scope de coroutines que vive lo mismo que la aplicación. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
