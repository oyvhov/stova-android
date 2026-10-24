package app.stova

import android.app.Application
import app.stova.homebutton.CrashGuard

class StovaApplication : Application(), coil3.SingletonImageLoader.Factory {
    override fun newImageLoader(context: coil3.PlatformContext): coil3.ImageLoader =
        app.stova.media.ui.buildImageLoader(context, container.mediaHttp, container.imageAuth)

    private var launcherContainer: AppContainer? = null
    val container: AppContainer
        get() = checkNotNull(launcherContainer) {
            "Launcher container is unavailable before launcher startup or in :homebutton; use process-local Home service state."
        }

    override fun onCreate() {
        super.onCreate()
        // The Home service's own process needs none of the launcher: no stores, no session.
        if (getProcessName().endsWith(":homebutton")) return
        // The guard goes in first, so even a crash while the container is built is counted.
        val guard = CrashGuard.store(this)
        CrashGuard.install(guard)
        launcherContainer = AppContainer(this, crashGuard = guard)
    }
}
