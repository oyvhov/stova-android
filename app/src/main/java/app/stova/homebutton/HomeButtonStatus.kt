package app.stova.homebutton

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/** Reports the user's configured accessibility access; does not change system settings. */
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
        Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1 &&
            isEnabled(Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), component(context))

}
