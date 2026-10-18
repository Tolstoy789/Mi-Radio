package cl.miradio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.miradio.app.playback.PlayerController
import cl.miradio.app.ui.RadioAppContent
import cl.miradio.app.ui.theme.MiRadioTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerController: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiRadioTheme {
                RadioAppContent()
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
