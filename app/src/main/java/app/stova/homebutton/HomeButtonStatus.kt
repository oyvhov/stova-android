package app.stova.homebutton

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/** Reads, and with an adb-granted permission repairs, the system list of enabled services. */
object HomeButtonStatus {
    fun isEnabled(enabledServices: String?, component: ComponentName): Boolean =
        enabledServices.orEmpty().split(':').any { ComponentName.unflattenFromString(it) == component }

    fun withComponent(enabledServices: String?, component: ComponentName): String = when {
        isEnabled(enabledServices, component) -> enabledServices.orEmpty()
        enabledServices.isNullOrBlank() -> component.flattenToString()
        else -> "$enabledServices:${component.flattenToString()}"
    }

    fun component(context: Context) = ComponentName(context, HomeButtonService::class.java)

    fun serviceEnabled(context: Context): Boolean =
        isEnabled(Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), component(context))

    /**
     * Some TV builds switch accessibility services off after an update. With WRITE_SECURE_SETTINGS
     * granted over adb, Stova turns its own service back on; without it this does nothing.
     */
    fun ensureEnabled(context: Context): Boolean {
        if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) return false
        val resolver = context.contentResolver
        val current = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (isEnabled(current, component(context))) return true
        return runCatching {
            Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, withComponent(current, component(context)))
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        }.isSuccess
    }
}
