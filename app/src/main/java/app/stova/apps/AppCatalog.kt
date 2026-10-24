package app.stova.apps

import kotlinx.coroutines.flow.Flow

/** What is installed, when that changes, and how to open it. */
interface AppCatalog {
    suspend fun installed(): List<InstalledApp>
    fun changes(): Flow<Unit>
    fun launch(app: InstalledApp): Boolean
    fun openAppInfo(packageName: String)
}
