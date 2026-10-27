package app.stova

import android.content.Context
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import app.stova.core.security.KeystoreSecretStore
import app.stova.ha.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Hand-wired dependencies for the whole app. Later tasks add their repositories here. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppContainer(
    context: Context,
    val crashGuard: JsonFileStore<CrashGuardState> = CrashGuard.store(context),
    val secrets: app.stova.core.security.SecretStore = KeystoreSecretStore(context),
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
    internal val appearanceEdits = app.stova.settings.AppearanceEditsStore(appScope) { id, prefs ->
        profiles.update(id) { it.copy(appearance = prefs) }
    }

    val appCatalog = PackageManagerAppCatalog(
        appContext,
        PackageManagerAppCatalog.STOCK_LAUNCHERS + appContext.packageName,
    )

    val apps = AppRepository(appCatalog, profiles, appScope)

    val household = JsonFileStore(File(appContext.filesDir, "household.json"), HouseholdSettings.serializer(), HouseholdSettings())
    private val identity = appScope.async(Dispatchers.IO) {
        val settings = household.update { if (it.deviceId.isBlank()) it.copy(deviceId = java.util.UUID.randomUUID().toString()) else it }
        app.stova.media.net.ClientIdentity(android.os.Build.MODEL, settings.deviceId, BuildConfig.VERSION_NAME)
    }
    val mediaHttp = app.stova.media.net.mediaHttp()

    val mediaAccounts = app.stova.media.MediaAccountRepository(
        JsonFileStore(File(appContext.filesDir, "media-accounts.json"), app.stova.media.MediaAccountsFile.serializer(), app.stova.media.MediaAccountsFile()),
        app.stova.core.security.KeystoreSecretStore(appContext),
    )
    val serverProbe = app.stova.media.net.ServerProbe(mediaHttp)
    val mediaAuth = app.stova.media.net.MediaAuth(mediaHttp) { identity.await() }
    val lanDiscovery = app.stova.media.net.LanDiscovery()

    @Volatile private var imageTokens: Map<String, Pair<app.stova.media.model.ServerKind, String>> = emptyMap()
    // OkHttp invokes this supplier on its network thread, never while composing or starting Home.
    val imageAuth = app.stova.media.ui.MediaAuthInterceptor({ imageTokens }) { kotlinx.coroutines.runBlocking { identity.await() } }
    init {
        appScope.launch {
            session.active.distinctUntilChangedBy { it?.id }.flatMapLatest { profile ->
                // Clear the old profile before the new profile's Keystore reads complete.
                imageTokens = emptyMap()
                if (profile == null) kotlinx.coroutines.flow.flowOf(emptyMap()) else mediaAccounts.imageCredentials(profile.id)
            }.collect { imageTokens = it }
        }
    }

    val appArt = AppArtCache(appContext)
    val iconPacks get() = appArt.iconPacks

    init {
        appScope.launch {
            apps.packageChanges.collect { iconPacks.packagesChanged() }
        }
    }

    val media = app.stova.media.MediaRepository(mediaAccounts, { account ->
        mediaAccounts.token(account.id)?.let { app.stova.media.net.JellyfinEmbyClient(account, it, mediaHttp, identity.await()) }
    }, File(appContext.filesDir, "snapshots"), appScope)

    val watchNext = app.stova.watchnext.WatchNextReader(appContext.contentResolver, {
        androidx.core.content.ContextCompat.checkSelfPermission(appContext, "android.permission.READ_TV_LISTINGS") == android.content.pm.PackageManager.PERMISSION_GRANTED
    }, appContext.packageName)

    /** Favourite libraries and scenes: local facts for Home, and lists from the servers for the Apps page. */
    val shortcuts = app.stova.apps.ShortcutCatalog(
        accounts = mediaAccounts::forProfile,
        haReady = household.data.map { it.haBaseUrl != null }.distinctUntilChanged(),
        libraries = { profileId ->
            val owned = mediaAccounts.forProfile(profileId).first().associateBy { it.id }
            if (owned.isEmpty()) emptyList() else media.backgroundLibraries(profileId).libraries.mapNotNull { library ->
                owned[library.ref.accountId]?.let { account ->
                    app.stova.apps.FavoriteTile.Library(app.stova.apps.ShortcutKey.Library(account.id, library.ref.libraryId).key,
                        library.name, library.image, account, library.ref.libraryId)
                }
            }
        },
        scenes = { if (household.current().haBaseUrl == null) emptyList() else app.stova.ha.haShortcutScenes(haSocket, haRepository::loadRegistry) },
    )

    val homeSources: List<HomeRowSource> = listOf(AppsRowSource(apps, session, shortcuts.accounts, shortcuts.haReady), app.stova.media.MediaRowSource(media), app.stova.watchnext.WatchNextRowSource(watchNext))


    val haHttp = OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()
    suspend fun haAddress(): HaAddress? = household.current().haBaseUrl?.let { (checkHaAddress(it) as? AddressCheck.Ok)?.address }
    val haAuth = HaAuth(haHttp, secrets, ::haAddress)
    private val haScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val haSocket = HaSocket(haHttp, haAuth, ::haAddress, haScope)
    val haRepository = HaRepository(haSocket, session, profiles, appScope,
        JsonFileStore(File(appContext.filesDir, "ha-cache.json"), HaSnapshot.serializer(), HaSnapshot()))
    val haImages by lazy {
        coil3.ImageLoader.Builder(appContext).components {
            add(coil3.network.okhttp.OkHttpNetworkFetcherFactory(callFactory = { haHttp }))
        }.build()
    }
    init { appScope.launch { haAuth.restore() } }
}
