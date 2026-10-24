package app.stova.apps

/** One launchable activity. `key` is `"<package>/<activity>"`, the form profile prefs store. */
data class InstalledApp(
    val key: String,
    val packageName: String,
    val activityName: String,
    val label: String,
    val tv: Boolean,
    val lastUpdateTime: Long = 0,
)
