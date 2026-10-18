package cl.miradio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint

/**
 * Punto de entrada de la app. En la Fase 1 solo muestra una pantalla base;
 * la UI completa (grilla, mini-player, Now Playing) se monta en la Fase 2.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121214)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.app_name), color = Color.White)
            }
        }
    }
}
