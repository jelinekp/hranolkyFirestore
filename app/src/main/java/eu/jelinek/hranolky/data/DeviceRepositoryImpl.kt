package eu.jelinek.hranolky.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import eu.jelinek.hranolky.model.DeviceInfo
import kotlinx.coroutines.tasks.await

class DeviceRepositoryImpl(private val firestoreDb: FirebaseFirestore) : DeviceRepository {

    private val TAG = "DeviceRepository"

    override suspend fun updateDeviceInfo(deviceId: String, appVersion: String) {
        val deviceData = hashMapOf(
            "lastSeen" to FieldValue.serverTimestamp(),
            "appVersion" to appVersion
        )

        try {
            firestoreDb.collection("Devices")
                .document(deviceId)
                .set(deviceData, SetOptions.merge())
                .await()
            Log.d(TAG, "Device document created/updated for ID: $deviceId")
        } catch (e: Exception) {
            Log.w(TAG, "Error writing device document for ID: $deviceId", e)
            throw e
        }
    }

    override suspend fun getDeviceInfo(deviceId: String): DeviceInfo? {
        return try {
            val documentSnapshot = firestoreDb.collection("Devices")
                .document(deviceId)
                .get()
                .await()

            if (documentSnapshot.exists()) {
                val deviceName = documentSnapshot.getString("deviceName")
                val isInventoryCheckPermitted = documentSnapshot.getBoolean("isInventoryCheckPermitted") ?: false
                val appVersion = documentSnapshot.getString("appVersion")
                val lastSeen = documentSnapshot.getTimestamp("lastSeen")

                DeviceInfo(
                    deviceName = deviceName,
                    isInventoryCheckPermitted = isInventoryCheckPermitted,
                    appVersion = appVersion,
                    lastSeen = lastSeen
                )
            } else {
                Log.d(TAG, "Device document not found for ID: $deviceId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching device info for ID: $deviceId", e)
            throw e
        }
    }
}

