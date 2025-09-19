package eu.jelinek.hranolky.ui.start

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StartViewModel(
    application: Application,
    private val firestoreDb: FirebaseFirestore,
) : AndroidViewModel(application) {
    private val _startScreenState = MutableStateFlow(StartUiState())
    val startScreenState get() = _startScreenState.asStateFlow()

    init {
        viewModelScope.launch {
            fetchDeviceName()
            logDeviceId()
        }
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    private fun logDeviceId() {
        val context = getApplication<Application>().applicationContext
        val deviceId = getDeviceId()

        val appVersion = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AppVersion", "Could not get package info", e)
            "Unknown"
        }

        _startScreenState.value = _startScreenState.value.copy(
            shortenedDeviceId = deviceId.substring(0..2),
            appVersion = appVersion ?: "Neznámá verze"
        )

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
    }

    private suspend fun fetchDeviceName() { // Make this a suspend function
        val deviceId = getDeviceId()
        Log.d("StartViewModel", "Fetching device name for ID: $deviceId")
        try {
            val documentSnapshot = firestoreDb.collection("devices").document(deviceId).get().await()

            if (documentSnapshot.exists()) {
                val deviceName = documentSnapshot.getString("deviceName") // Get the "deviceName" field
                if (deviceName != null) {
                    Log.d("StartViewModel", "Device name found: $deviceName")
                    _startScreenState.value = _startScreenState.value.copy(deviceName = deviceName)
                } else {
                    Log.d("StartViewModel", "deviceName field is null or not found in document for $deviceId.")
                    // Optionally set a default or leave it as null in the state
                    _startScreenState.value = _startScreenState.value.copy(deviceName = null) // Or some default like "Unknown Device"
                }
            } else {
                Log.d("StartViewModel", "Device document not found for ID: $deviceId. Cannot fetch device name.")
                _startScreenState.value = _startScreenState.value.copy(deviceName = null) // Or some default
            }
        } catch (e: Exception) {
            Log.e("StartViewModel", "Error fetching device name for ID: $deviceId", e)
            _startScreenState.value = _startScreenState.value.copy(deviceName = null) // Or some default in case of error
        }
    }

    fun isValidScannedTextFormat(text: String): Boolean { // TODO move to input validator class
        val textLength = text.length

        if (textLength == 16) {
            if (text[1] != '-'
                && text[textLength - 5] == '-'
                && text.substring(textLength - 4).all(Char::isDigit)
            ) {
               return true // Valid original universal beam
            }
        }

        if (textLength == 18) {
            if (text[0] == 'H'
                && text[1] == '-'
                && text[textLength - 5] == '-'
                && text.substring(textLength - 4).all(Char::isDigit)
            ) {
                return true // Valid specified beam
            }
        }

        if (textLength == 20) {
            if (text[0] == 'S'
                && text[1] == '-'
                && text[textLength - 10] == '-'
                && text.substring(textLength - 9, textLength - 5).all(Char::isDigit)
                && text[textLength - 5] == '-'
                && text.substring(textLength - 4).all(Char::isDigit)
            ) {
                return true // Valid specified jointer
            }
        }

        // If neither rule matched, it's not a valid format
        return false
    }

}

data class StartUiState(
    val scannedCode: String = "",
    val shortenedDeviceId: String = "",
    val deviceName: String? = null,
    val appVersion: String = "",
)

