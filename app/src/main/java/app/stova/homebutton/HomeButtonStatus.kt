package app.stova.homebutton

import android.content.ComponentName
import android.content.Context
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

}
