package com.example.whereuat.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.net.wifi.WifiManager
import com.example.whereuat.model.FriendSignal
import com.example.whereuat.model.ScanProfile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BleScanner(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val leScanner: BluetoothLeScanner?
        get() = adapter?.bluetoothLeScanner

    @SuppressLint("MissingPermission")
    fun scanFriends(profileFlow: Flow<ScanProfile>): Flow<FriendSignal> = callbackFlow {
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.device.name ?: result.scanRecord?.deviceName ?: return
                if (!name.startsWith("WhereUAt-")) return
                val angle = ((System.currentTimeMillis() / 50L) % 360).toFloat()
                trySend(
                    FriendSignal(
                        friendId = result.device.address,
                        displayName = name.removePrefix("WhereUAt-"),
                        rssi = result.rssi,
                        azimuthDeg = angle
                    )
                )
            }
        }

        launch(dispatcher) {
            profileFlow.collect { profile ->
                val intervalMs = when (profile) {
                    ScanProfile.FAST -> 1_500L
                    ScanProfile.BALANCED -> 3_500L
                    ScanProfile.BATTERY_SAVER -> 7_000L
                }
                val mode = when (profile) {
                    ScanProfile.FAST -> ScanSettings.SCAN_MODE_LOW_LATENCY
                    ScanProfile.BALANCED -> ScanSettings.SCAN_MODE_BALANCED
                    ScanProfile.BATTERY_SAVER -> ScanSettings.SCAN_MODE_LOW_POWER
                }

                leScanner?.stopScan(callback)
                val settings = ScanSettings.Builder().setScanMode(mode).build()

                while (isActive) {
                    leScanner?.startScan(null, settings, callback)
                    delay(1_200)
                    leScanner?.stopScan(callback)
                    delay(intervalMs)
                }
            }
        }

        awaitClose { leScanner?.stopScan(callback) }
    }

    fun wifiSignalBoost(address: String): Int {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val level = wifi.connectionInfo?.rssi ?: -90
        return (level / 10) + address.hashCode() % 4
    }
}
