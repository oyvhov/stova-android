package app.stova

import android.app.Application
import app.stova.homebutton.CrashGuard
import app.stova.homebutton.HomeButtonStatus

class StovaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CrashGuard.install(container.crashGuard)
        HomeButtonStatus.ensureEnabled(this)
    }
}
