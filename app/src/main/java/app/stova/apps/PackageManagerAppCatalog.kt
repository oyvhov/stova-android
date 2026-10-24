package app.stova.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * TV apps first (`LEANBACK_LAUNCHER`), then ordinary launcher apps for sideloaded phone apps.
 * One entry per package, and the TV activity wins. Stova and the stock launchers are never listed.
 */
class PackageManagerAppCatalog(
    private val context: Context,
    private val excluded: Set<String>,
) : AppCatalog {
    private val pm = context.packageManager

    override suspend fun installed(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val seen = mutableSetOf<String>()
        buildList {
            for ((category, tv) in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER to true, Intent.CATEGORY_LAUNCHER to false)) {
                for (info in pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)) {
                    val activity = info.activityInfo
                    val pkg = activity.packageName
                    if (pkg in excluded || !seen.add(pkg)) continue
                    add(InstalledApp("$pkg/${activity.name}", pkg, activity.name, info.loadLabel(pm).toString(), tv, pm.getPackageInfo(pkg, 0).lastUpdateTime))
                }
            }
        }
    }

    override fun changes(): Flow<Unit> = callbackFlow {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageAdded(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageChanged(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) { trySend(Unit) }
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) { trySend(Unit) }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }

    override fun launch(app: InstalledApp): Boolean {
        val intent = (if (app.tv) pm.getLeanbackLaunchIntentForPackage(app.packageName) else null)
            ?: pm.getLaunchIntentForPackage(app.packageName)
            ?: Intent(Intent.ACTION_MAIN).setClassName(app.packageName, app.activityName)
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    override fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // Some TV builds ship without the details page; there is nothing better to open.
        }
    }

    override fun isInstalled(packageName: String): Boolean = try {
        pm.getPackageInfo(packageName, 0)
        true
    } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
        false
    }

    companion object {
        val STOCK_LAUNCHERS = setOf(
            "com.google.android.tvlauncher",
            "com.google.android.apps.tv.launcherx",
            "com.google.android.leanbacklauncher",
        )
    }
}
