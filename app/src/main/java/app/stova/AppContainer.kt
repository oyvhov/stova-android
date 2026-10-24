package app.stova

import android.content.Context
import app.stova.core.storage.JsonFileStore
import app.stova.profiles.ProfileRepository
import app.stova.profiles.ProfilesFile
import java.io.File

/** Hand-wired dependencies for the whole app. Later tasks add their repositories here. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val profiles = ProfileRepository(
        JsonFileStore(File(appContext.filesDir, "profiles.json"), ProfilesFile.serializer(), ProfilesFile()),
    )
}
