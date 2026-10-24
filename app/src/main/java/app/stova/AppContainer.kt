package app.stova

import android.content.Context
import app.stova.core.storage.JsonFileStore
import app.stova.profiles.ProfileRepository
import app.stova.profiles.ProfileSession
import app.stova.profiles.ProfilesFile
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hand-wired dependencies for the whole app. Later tasks add their repositories here. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    /** Lives as long as the process: a launcher's process is the home screen itself. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val profiles = ProfileRepository(
        JsonFileStore(File(appContext.filesDir, "profiles.json"), ProfilesFile.serializer(), ProfilesFile()),
    )

    val session = ProfileSession(profiles, appScope)
}
