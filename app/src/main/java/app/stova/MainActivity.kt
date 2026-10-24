package app.stova

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.stova.navigation.StovaApp

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        (application as StovaApplication).container.haSocket.start()
    }
    override fun onStop() {
        (application as StovaApplication).container.haSocket.stop()
        super.onStop()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A launcher never closes itself on Back; the navigation's own handler runs first.
        onBackPressedDispatcher.addCallback(this) { }
        val container = (application as StovaApplication).container
        setContent { StovaApp(container) }
    }
}
