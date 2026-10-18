package cl.miradio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import cl.miradio.app.playback.PlayerController
import cl.miradio.app.ui.RadioAppContent
import cl.miradio.app.ui.intro.IntroAnimation
import cl.miradio.app.ui.theme.MiRadioTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerController: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // La animación de inicio solo corre en un arranque nuevo, no al rotar ni al volver.
        val isColdStart = savedInstanceState == null
        setContent {
            MiRadioTheme {
                var showIntro by rememberSaveable { mutableStateOf(isColdStart) }
                Box(Modifier.fillMaxSize()) {
                    RadioAppContent()
                    if (showIntro) {
                        IntroAnimation(onFinished = { showIntro = false })
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        playerController.connect()
    }

    override fun onStop() {
        playerController.disconnect()
        super.onStop()
    }
}
