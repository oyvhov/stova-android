package app.stova.homebutton

import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Only actual HOME activities. A launcher's settings and dialogs must remain usable. */
internal class HomeActivities(private val activities: Set<ComponentName>) {
    val packages: Set<String> = activities.mapTo(mutableSetOf()) { it.packageName }

    fun contains(packageName: CharSequence?, className: CharSequence?): Boolean {
        val pkg = packageName?.toString() ?: return false
        val name = className?.toString() ?: return false
        return ComponentName(pkg, fullName(pkg, name)) in activities
    }

    companion object {
        private fun fullName(pkg: String, name: String) = when {
            name.startsWith('.') -> pkg + name
            '.' !in name -> "$pkg.$name"
            else -> name
        }

        fun discover(context: Context): HomeActivities {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val activities = context.packageManager.queryIntentActivities(intent, 0).flatMap { result ->
                val info = result.activityInfo ?: return@flatMap emptyList()
                if (info.packageName == context.packageName) return@flatMap emptyList()
                // An activity alias may emit the concrete target's name in accessibility events.
                listOfNotNull(info.name, info.targetActivity).map { ComponentName(info.packageName, fullName(info.packageName, it)) }
            }.toSet()
            return HomeActivities(activities)
        }
    }
}
