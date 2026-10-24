package app.stova

import android.app.Application
import app.stova.homebutton.CrashGuard
import app.stova.homebutton.HomeButtonStatus

class StovaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // The Home service's own process needs none of the launcher: no stores, no session.
        if (getProcessName().endsWith(":homebutton")) return
        container = AppContainer(this)
        CrashGuard.install(container.crashGuard)
        HomeButtonStatus.ensureEnabled(this)
    }
}
