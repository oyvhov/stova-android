package app.stova.system

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock

/** What the device reports right now, read once each time the Settings icon's menu opens. */
data class SystemStatus(
    val device: String,
    val android: String,
    val uptimeMinutes: Long,
    val network: NetworkStatus,
    /** Null when Android does not let Stova read it. */
    val bluetooth: BluetoothStatus?,
    /** Stova's Home-button service is on, so it can open Android's power menu. */
    val powerMenu: Boolean,
)

/** Only whether and how the device is connected; the menu shows it with an icon. */
sealed interface NetworkStatus {
    data object Offline : NetworkStatus
    /** [level] 0..4. */
    data class Wifi(val level: Int) : NetworkStatus
    data object Ethernet : NetworkStatus
}

data class BluetoothStatus(val on: Boolean, val paired: Int?)

/** Five steps between -100 and -55 dBm, as Android's own Wi-Fi bars. */
internal fun signalLevel(rssi: Int, levels: Int = 5): Int = when {
    rssi <= -100 -> 0
    rssi >= -55 -> levels - 1
    else -> ((rssi + 100) * (levels - 1) / 45f).toInt()
}

/** How many of the Wi-Fi icon's three arcs a [signalLevel] lights; a connection always lights one. */
internal fun wifiArcs(level: Int): Int = when {
    level >= 3 -> 3
    level == 2 -> 2
    else -> 1
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
        powerMenu = app.stova.homebutton.HomeButtonStatus.serviceEnabled(device),
    )
}

@Suppress("DEPRECATION") // connectionInfo is the only way to read the signal on Android 11.
private fun network(context: Context): NetworkStatus {
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val active = connectivity.activeNetwork ?: return NetworkStatus.Offline
    val caps = connectivity.getNetworkCapabilities(active) ?: return NetworkStatus.Offline
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkStatus.Ethernet
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
            NetworkStatus.Wifi(context.getSystemService(WifiManager::class.java)?.connectionInfo?.rssi?.let { signalLevel(it) } ?: 0)
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
