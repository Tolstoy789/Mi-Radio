package cl.miradio.app.playback

/**
 * Contrato entre [PlaybackService] y sus controladores (la UI de la Fase 2,
 * Android Auto, etc.): comandos personalizados y claves de los extras de sesión.
 */
object PlaybackContract {

    /** Programa el Sleep Timer. Argumento: [EXTRA_SLEEP_MINUTES]; 0 lo cancela. */
    const val ACTION_SET_SLEEP_TIMER = "cl.miradio.app.action.SET_SLEEP_TIMER"
    const val EXTRA_SLEEP_MINUTES = "sleep_minutes"

    /** Duraciones que ofrece la UI. */
    val SLEEP_TIMER_OPTIONS_MINUTES = listOf(15, 30, 60)

    /**
     * Extra de sesión con el instante en que el Sleep Timer pausará la radio,
     * en `SystemClock.elapsedRealtime()`. Ausente si no hay timer activo.
     */
    const val KEY_SLEEP_TIMER_END_ELAPSED = "sleep_timer_end_elapsed"

    /** Extra de sesión: `true` mientras el servicio reintenta tras un corte de red. */
    const val KEY_IS_RECONNECTING = "is_reconnecting"
}
