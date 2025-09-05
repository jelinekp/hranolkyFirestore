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
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.model.WarehouseSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StartViewModel(
    application: Application,
    private val firestoreDb: FirebaseFirestore,
    private val slotRepository: SlotRepository
) : AndroidViewModel(application) {
    private val _startScreenState = MutableStateFlow(StartUiState())
    val startScreenState get() = _startScreenState.asStateFlow()

    init {
        viewModelScope.launch {
            logDeviceId()
        }

        viewModelScope.launch {
            slotRepository.getLastModifiedSlots().collect { lastModifiedSlots ->
                _startScreenState.update { it.copy(lastModifiedBeamSlots = lastModifiedSlots.beamSlots, lastModifiedJointerSlots = lastModifiedSlots.jointerSlots) }
            }
        }
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        return Settings.Secure.getString(getApplication<Application>().contentResolver, Settings.Secure.ANDROID_ID)
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

        val deviceData = hashMapOf(
            "lastSeen" to FieldValue.serverTimestamp(),
            "appVersion" to appVersion
        )

        firestoreDb.collection("devices").document(deviceId)
            .set(deviceData, SetOptions.merge())
            .addOnSuccessListener { Log.d("Firestore", "Device document created/updated for ID: $deviceId") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error writing document", e) }
    }
}

data class StartUiState(
    val scannedCode: String = "",
    val lastModifiedBeamSlots: List<WarehouseSlot> = emptyList(),
    val lastModifiedJointerSlots: List<WarehouseSlot> = emptyList(),
)

fun isValidScannedTextFormat(text: String): Boolean {
    val textLength = text.length

    if (textLength == 16) {
        if (text[1] != '-'
            && text[textLength - 5] == '-'
            && text.substring(textLength - 4).all(Char::isDigit)
            ) {
            return true // Valid universal beam
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