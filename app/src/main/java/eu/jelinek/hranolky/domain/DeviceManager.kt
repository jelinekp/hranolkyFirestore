package eu.jelinek.hranolky.domain

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import eu.jelinek.hranolky.ui.start.StartUiState
import kotlinx.coroutines.tasks.await

class DeviceManager(private val firestoreDb: FirebaseFirestore) {
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    fun logDeviceId(context: Context, state: StartUiState): StartUiState {
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

        val deviceData = hashMapOf(
            "lastSeen" to FieldValue.serverTimestamp(),
            "appVersion" to appVersion
        )

        firestoreDb.collection("devices").document(deviceId)
            .set(deviceData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(
                    "Firestore",
                    "Device document created/updated for ID: $deviceId"
                )
            }
            .addOnFailureListener { e -> Log.w("Firestore", "Error writing document", e) }

        return state.copy(
            shortenedDeviceId = deviceId.substring(0..2),
            appVersion = appVersion ?: "Neznámá verze",
            appVersionCode = currentVersionCode
        )
    }

    suspend fun fetchDeviceNameAndInventoryPermit(context: Context, state: StartUiState): StartUiState {
        val deviceId = getDeviceId(context)
        Log.d("StartViewModel", "Fetching device name for ID: $deviceId")
        try {
            val documentSnapshot = firestoreDb.collection("devices").document(deviceId).get().await()

            if (documentSnapshot.exists()) {
                val deviceName = documentSnapshot.getString("deviceName")
                val isInventoryCheckPermitted = documentSnapshot.getBoolean("isInventoryCheckPermitted") ?: false

                return state.copy(
                    deviceName = deviceName,
                    isInventoryCheckPermitted = isInventoryCheckPermitted
                )

            } else {
                Log.d("StartViewModel", "Device document not found for ID: $deviceId. Cannot fetch device name.")
                return state.copy(
                    deviceName = null,
                    isInventoryCheckPermitted = false,
                )
            }
        } catch (e: Exception) {
            Log.e("StartViewModel", "Error fetching device name for ID: $deviceId", e)
            return state.copy(deviceName = null)
        }
    }
}
