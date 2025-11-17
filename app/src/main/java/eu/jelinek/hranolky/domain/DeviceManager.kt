package eu.jelinek.hranolky.domain

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import eu.jelinek.hranolky.data.DeviceRepository
import eu.jelinek.hranolky.ui.start.StartUiState

class DeviceManager(private val deviceRepository: DeviceRepository) {
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    suspend fun logDeviceId(context: Context, state: StartUiState): StartUiState {
        val deviceId = getDeviceId(context)

        val appVersion = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AppVersion", "Could not get package info", e)
            "Unknown"
        }

        val currentVersionCode = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AppVersion", "Could not get package info", e)
            -1
        }

        try {
            deviceRepository.updateDeviceInfo(deviceId, appVersion ?: "Unknown")
        } catch (e: Exception) {
            Log.w("DeviceManager", "Error updating device info", e)
        }

        return state.copy(
            shortenedDeviceId = deviceId.substring(0..2),
            appVersion = appVersion ?: "Neznámá verze",
            appVersionCode = currentVersionCode
        )
    }

    suspend fun fetchDeviceNameAndInventoryPermit(context: Context, state: StartUiState): StartUiState {
        val deviceId = getDeviceId(context)
        Log.d("DeviceManager", "Fetching device info for ID: $deviceId")

        return try {
            val deviceInfo = deviceRepository.getDeviceInfo(deviceId)

            if (deviceInfo != null) {
                state.copy(
                    deviceName = deviceInfo.deviceName,
                    isInventoryCheckPermitted = deviceInfo.isInventoryCheckPermitted
                )
            } else {
                Log.d("DeviceManager", "Device document not found for ID: $deviceId")
                state.copy(
                    deviceName = null,
                    isInventoryCheckPermitted = false
                )
            }
        } catch (e: Exception) {
            Log.e("DeviceManager", "Error fetching device info for ID: $deviceId", e)
            state.copy(
                deviceName = null,
                isInventoryCheckPermitted = false
            )
        }
    }
}
