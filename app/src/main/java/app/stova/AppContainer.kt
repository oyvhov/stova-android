package app.stova

import android.content.Context
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
import app.stova.core.security.KeystoreSecretStore
import app.stova.ha.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Hand-wired dependencies for the whole app. Later tasks add their repositories here. */
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

    val appCatalog = PackageManagerAppCatalog(
        appContext,
        PackageManagerAppCatalog.STOCK_LAUNCHERS + appContext.packageName,
    )

    val apps = AppRepository(appCatalog, profiles, appScope)

    val appArt = AppArtCache(appContext)

    val homeSources: List<HomeRowSource> = listOf(AppsRowSource(apps, session))

    val household = JsonFileStore(File(appContext.filesDir, "household.json"), HouseholdSettings.serializer(), HouseholdSettings())
    val haHttp = OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()
    suspend fun haAddress(): HaAddress? = household.current().haBaseUrl?.let { (checkHaAddress(it) as? AddressCheck.Ok)?.address }
    val haAuth = HaAuth(haHttp, secrets, ::haAddress)
    private val haScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val haSocket = HaSocket(haHttp, haAuth, ::haAddress, haScope)
    val haRepository = HaRepository(haSocket, session, profiles, appScope)
    val haImages by lazy {
        coil3.ImageLoader.Builder(appContext).components {
            add(coil3.network.okhttp.OkHttpNetworkFetcherFactory(callFactory = { haHttp }))
        }.build()
    }
    init { appScope.launch { haAuth.restore() } }
}
