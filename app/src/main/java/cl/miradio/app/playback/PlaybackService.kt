package cl.miradio.app.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import cl.miradio.app.BuildConfig
import cl.miradio.app.MainActivity
import cl.miradio.app.R
import cl.miradio.app.data.local.dao.StationDao
import cl.miradio.app.data.mapper.toDomain
import cl.miradio.app.di.StreamingHttpClient
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import okhttp3.OkHttpClient

/**
 * Motor de audio de la app.
 *
 * - **Foreground service**: [MediaSessionService] publica la notificación multimedia
 *   y pasa el servicio a primer plano mientras se reproduce, así el sistema no lo mata.
 * - **Audio focus**: ExoPlayer pausa con llamadas, baja el volumen con notificaciones
 *   y pausa si se desconectan los audífonos.
 * - **Reconexión**: [LiveStreamLoadErrorPolicy] reintenta cortes breves sin error visible;
 *   [StreamReconnectionHandler] reconecta tras cortes largos o cuando vuelve la red.
 * - **Sleep Timer**: comando personalizado [PlaybackContract.ACTION_SET_SLEEP_TIMER].
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var stationDao: StationDao
    @Inject lateinit var preferences: PlaybackPreferences
    @Inject @StreamingHttpClient lateinit var streamingClient: OkHttpClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaSession: MediaSession? = null
    private lateinit var sleepTimer: SleepTimer
    private lateinit var reconnectionHandler: StreamReconnectionHandler

    override fun onCreate() {
        super.onCreate()
        val player = buildPlayer()

        sleepTimer = SleepTimer(player, serviceScope) { publishSessionExtras() }
        reconnectionHandler = StreamReconnectionHandler(this, player, serviceScope) {
            publishSessionExtras()
        }
        player.addListener(NowPlayingTracker(player))
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.takeIf { it.isNotEmpty() }?.let { preferences.lastStationId = it }
            }
        })

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply {
                setSmallIcon(R.drawable.ic_stat_radio)
            },
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(buildSessionActivity())
            .setCallback(SessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /** Si el usuario cierra la app desde Recientes sin estar escuchando, se detiene el servicio. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null ||
            !player.playWhenReady ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        sleepTimer.cancel()
        reconnectionHandler.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildPlayer(): ExoPlayer {
        val httpDataSourceFactory = OkHttpDataSource.Factory(streamingClient)
            .setUserAgent(BuildConfig.USER_AGENT)
        val mediaSourceFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(this, httpDataSourceFactory))
            .setLoadErrorHandlingPolicy(LiveStreamLoadErrorPolicy())

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Buffer más amplio que el de fábrica para absorber microcortes de red móvil.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000,
            )
            .build()

        return ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }

    private fun buildSessionActivity(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun publishSessionExtras() {
        val session = mediaSession ?: return
        session.setSessionExtras(
            Bundle().apply {
                sleepTimer.endElapsedRealtime?.let { putLong(PlaybackContract.KEY_SLEEP_TIMER_END_ELAPSED, it) }
                putBoolean(PlaybackContract.KEY_IS_RECONNECTING, reconnectionHandler.isReconnecting)
            },
        )
    }

    /** Resuelve el stream de un [MediaItem] que llega desde un controlador sin URI. */
    private suspend fun resolve(item: MediaItem): MediaItem {
        if (item.localConfiguration != null) return item
        stationDao.getStation(item.mediaId)?.let { return it.toDomain().toMediaItem() }
        val requestedUri = item.requestMetadata.mediaUri ?: return item
        return item.buildUpon().setUri(requestedUri).build()
    }

    private inner class SessionCallback : MediaSession.Callback {

        private val sleepTimerCommand = SessionCommand(PlaybackContract.ACTION_SET_SLEEP_TIMER, Bundle.EMPTY)

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ConnectionResult {
            val sessionCommands = ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(sleepTimerCommand)
                .build()
            return ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction != PlaybackContract.ACTION_SET_SLEEP_TIMER) {
                return super.onCustomCommand(session, controller, customCommand, args)
            }
            val minutes = args.getInt(PlaybackContract.EXTRA_SLEEP_MINUTES, 0)
            if (minutes > 0) sleepTimer.start(minutes) else sleepTimer.cancel()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = serviceScope.future {
            mediaItems.map { resolve(it) }.toMutableList()
        }

        /** Permite retomar la última radio desde los botones multimedia (Bluetooth, Android 12+). */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaItemsWithStartPosition> = serviceScope.future {
            val stationId = checkNotNull(preferences.lastStationId) { "No hay una radio anterior" }
            val station = checkNotNull(stationDao.getStation(stationId)) { "La radio ya no está en caché" }
            MediaItemsWithStartPosition(listOf(station.toDomain().toMediaItem()), 0, C.TIME_UNSET)
        }
    }
}
