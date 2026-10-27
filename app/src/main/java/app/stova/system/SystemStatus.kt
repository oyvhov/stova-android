package app.stova.system

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.view.Display
import java.net.Inet4Address

/** What the device reports right now, read once each time the Settings icon's menu opens. */
data class SystemStatus(
    val device: String,
    val android: String,
    val uptimeMinutes: Long,
    val network: NetworkStatus,
    /** Null when Android does not let Stova read it. */
    val bluetooth: BluetoothStatus?,
    val display: DisplayStatus?,
    val storage: StorageStatus?,
    /** Stova's Home-button service is on, so it can open Android's power menu. */
    val powerMenu: Boolean,
)

sealed interface NetworkStatus {
    data object Offline : NetworkStatus
    /** [level] 0..4. */
    data class Wifi(val level: Int, val band: WifiBand?, val ip: String?) : NetworkStatus
    data class Ethernet(val ip: String?) : NetworkStatus
}

enum class WifiBand { GHZ_24, GHZ_5, GHZ_6;
    companion object {
        fun of(mhz: Int): WifiBand? = when (mhz) {
            in 2400..2500 -> GHZ_24
            in 4900..5899 -> GHZ_5
            in 5900..7125 -> GHZ_6
            else -> null
        }
    }
}

data class BluetoothStatus(val on: Boolean, val paired: Int?)
data class DisplayStatus(val width: Int, val height: Int, val hz: Int)
data class StorageStatus(val free: Long, val total: Long)

/** Five steps between -100 and -55 dBm, as Android's own Wi-Fi bars. */
internal fun signalLevel(rssi: Int, levels: Int = 5): Int = when {
    rssi <= -100 -> 0
    rssi >= -55 -> levels - 1
    else -> ((rssi + 100) * (levels - 1) / 45f).toInt()
}

/** Reads only Android's own reports; call it off the main thread. Nothing here asks the network. */
fun readSystemStatus(context: Context): SystemStatus {
    val device = context.applicationContext
    return SystemStatus(
        device = listOf(Build.MANUFACTURER, Build.MODEL).let { (maker, model) ->
            if (model.startsWith(maker, ignoreCase = true)) model else "$maker $model"
        },
        android = Build.VERSION.RELEASE,
        uptimeMinutes = SystemClock.elapsedRealtime() / 60_000,
        network = runCatching { network(device) }.getOrDefault(NetworkStatus.Offline),
        bluetooth = runCatching { bluetooth(device) }.getOrNull(),
        display = runCatching { display(device) }.getOrNull(),
        storage = runCatching { StatFs(Environment.getDataDirectory().path).let { StorageStatus(it.availableBytes, it.totalBytes) } }.getOrNull(),
        powerMenu = app.stova.homebutton.HomeButtonStatus.serviceEnabled(device),
    )
}

@Suppress("DEPRECATION") // connectionInfo is the only way to read signal and band on Android 11.
private fun network(context: Context): NetworkStatus {
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val active = connectivity.activeNetwork ?: return NetworkStatus.Offline
    val caps = connectivity.getNetworkCapabilities(active) ?: return NetworkStatus.Offline
    val ip = connectivity.getLinkProperties(active)?.linkAddresses?.map { it.address }
        ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkStatus.Ethernet(ip)
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
            val info = context.getSystemService(WifiManager::class.java)?.connectionInfo
            NetworkStatus.Wifi(info?.rssi?.let { signalLevel(it) } ?: 0, info?.frequency?.let(WifiBand::of), ip)
        }
        else -> NetworkStatus.Offline
    }
}

// Guarded below. On Android 12+ Stova never asks for BLUETOOTH_CONNECT, so the check fails and nothing is read.
@SuppressLint("MissingPermission")
private fun bluetooth(context: Context): BluetoothStatus? {
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return null
    // Android 12 and later want a runtime permission that Stova does not ask for; then nothing is read.
    val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        else context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
    if (!allowed) return null
    val on = adapter.isEnabled
    return BluetoothStatus(on, if (on) adapter.bondedDevices.size else null)
}

private fun display(context: Context): DisplayStatus? {
    val mode = context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)?.mode ?: return null
    return DisplayStatus(mode.physicalWidth, mode.physicalHeight, kotlin.math.round(mode.refreshRate).toInt())
}
