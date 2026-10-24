package app.stova

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.stova.homebutton.LaunchGuard
import app.stova.navigation.StovaApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container get() = (application as StovaApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A launcher never closes itself on Back; the navigation's own handler runs first.
        onBackPressedDispatcher.addCallback(this) { }
        setContent { StovaApp(container) }
    }

    /** Home while Stova is in front (from the remote or the Home service) goes back to Home. */
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) container.homeRequests.tryEmit(Unit)
    }

    override fun onResume() {
        super.onResume()
        // Tells the Home service this start succeeded (see LaunchGuard).
        container.appScope.launch(Dispatchers.IO) { LaunchGuard.markAlive(filesDir) }
    }
}
