package cl.miradio.app.di

import cl.miradio.app.BuildConfig
import cl.miradio.app.data.remote.MirrorFailoverInterceptor
import cl.miradio.app.data.remote.RadioBrowserApi
import cl.miradio.app.data.remote.UserAgentInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    /** Cliente base compartido: un solo pool de conexiones y un solo dispatcher. */
    @Provides
    @Singleton
    fun provideBaseOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(UserAgentInterceptor(BuildConfig.USER_AGENT))
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Provides
    @Singleton
    @ApiHttpClient
    fun provideApiOkHttpClient(base: OkHttpClient): OkHttpClient = base.newBuilder()
        .addInterceptor(MirrorFailoverInterceptor(MirrorFailoverInterceptor.DEFAULT_MIRRORS))
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC),
                )
            }
        }
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    /**
     * Los streams en vivo nunca terminan, así que no hay callTimeout; el readTimeout
     * detecta un stream "colgado" y deja que ExoPlayer reintente.
     */
    @Provides
    @Singleton
    @StreamingHttpClient
    fun provideStreamingOkHttpClient(base: OkHttpClient): OkHttpClient = base.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(@ApiHttpClient client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.RADIO_BROWSER_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideRadioBrowserApi(retrofit: Retrofit): RadioBrowserApi =
        retrofit.create(RadioBrowserApi::class.java)
}
