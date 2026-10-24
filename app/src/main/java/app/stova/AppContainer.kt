package app.stova

import android.content.Context
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import app.stova.apps.AppArtCache
import app.stova.apps.AppRepository
import app.stova.apps.PackageManagerAppCatalog
import app.stova.core.storage.JsonFileStore
import app.stova.home.AppsRowSource
import app.stova.home.HomeRowSource
import app.stova.homebutton.CrashGuard
import app.stova.homebutton.CrashGuardState
import app.stova.profiles.ProfileRepository
import app.stova.profiles.ProfileSession
import app.stova.profiles.ProfilesFile
import app.stova.settings.HouseholdSettings
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow

/** Hand-wired dependencies for the whole app. Later tasks add their repositories here. */
class AppContainer(
    context: Context,
    val crashGuard: JsonFileStore<CrashGuardState> = CrashGuard.store(context),
) {
    val appContext: Context = context.applicationContext

    /** Lives as long as the process: a launcher's process is the home screen itself. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Home pressed while Stova is already in front: whatever page is open goes back to Home. */
    val homeRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val profiles = ProfileRepository(
        JsonFileStore(File(appContext.filesDir, "profiles.json"), ProfilesFile.serializer(), ProfilesFile()),
    )

    val session = ProfileSession(profiles, appScope)

    val appCatalog = PackageManagerAppCatalog(
        appContext,
        PackageManagerAppCatalog.STOCK_LAUNCHERS + appContext.packageName,
    )

    val apps = AppRepository(appCatalog, profiles, appScope)

    val household = JsonFileStore(File(appContext.filesDir, "household.json"), HouseholdSettings.serializer(), HouseholdSettings())
    private val deviceSettings = household.writeBlocking { if (it.deviceId.isBlank()) it.copy(deviceId = java.util.UUID.randomUUID().toString()) else it }
    val identity = app.stova.media.net.ClientIdentity(android.os.Build.MODEL, deviceSettings.deviceId, BuildConfig.VERSION_NAME)
    val mediaHttp = app.stova.media.net.mediaHttp()

    val mediaAccounts = app.stova.media.MediaAccountRepository(
        JsonFileStore(File(appContext.filesDir, "media-accounts.json"), app.stova.media.MediaAccountsFile.serializer(), app.stova.media.MediaAccountsFile()),
        app.stova.core.security.KeystoreSecretStore(appContext),
    )
    val serverProbe = app.stova.media.net.ServerProbe(mediaHttp)
    val mediaAuth = app.stova.media.net.MediaAuth(mediaHttp, identity)
    val lanDiscovery = app.stova.media.net.LanDiscovery()

    @Volatile private var imageTokens: Map<String, Pair<app.stova.media.model.ServerKind, String>> = emptyMap()
    val imageAuth = app.stova.media.ui.MediaAuthInterceptor({ imageTokens }, identity)
    init {
        appScope.launch {
            kotlinx.coroutines.flow.combine(session.active, mediaAccounts.all) { profile, accounts -> accounts.filter { it.profileId == profile?.id && !it.needsSignIn } }.collect { accounts ->
                imageTokens = accounts.mapNotNull { account ->
                    mediaAccounts.token(account.id)?.let { token ->
                        val url = account.baseUrl.toHttpUrl()
                        "${url.host}:${url.port}" to (account.kind to token)
                    }
                }.toMap()
            }
        }
    }

    val appArt = AppArtCache(appContext)

    val media = app.stova.media.MediaRepository(mediaAccounts, { account ->
        mediaAccounts.token(account.id)?.let { app.stova.media.net.JellyfinEmbyClient(account, it, mediaHttp, identity) }
    }, File(appContext.filesDir, "snapshots"), appScope)

    val homeSources: List<HomeRowSource> = listOf(AppsRowSource(apps, session), app.stova.media.MediaRowSource(media))


}
