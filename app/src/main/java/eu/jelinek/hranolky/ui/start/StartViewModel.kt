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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import eu.jelinek.hranolky.model.FirestoreSlot
import eu.jelinek.hranolky.model.WarehouseSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StartViewModel(
    application: Application,
    private val firestoreDb: FirebaseFirestore,
) : AndroidViewModel(application) {
    private val _startScreenState = MutableStateFlow(StartUiState())
    val startScreenState get() = _startScreenState.asStateFlow()

    private var lastSlotListener: ListenerRegistration? = null

    fun updateScannedCode(scannedCode: String) {
        _startScreenState.value = _startScreenState.value.copy(scannedCode = scannedCode)
    }

    fun updateLastSlots(lastModifiedSlots: List<WarehouseSlot>) {
        _startScreenState.value = _startScreenState.value.copy(lastModifiedSlots = lastModifiedSlots)
    }

    init {
        viewModelScope.launch {
            logDeviceId()
        }

        viewModelScope.launch {
            fetchLastModifiedSlots()
        }
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        return Settings.Secure.getString(getApplication<Application>().contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun logDeviceId() {
        // In your app's startup logic
        val context = getApplication<Application>().applicationContext
        val deviceId = getDeviceId()

// Get app version name dynamically
        val appVersion = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AppVersion", "Could not get package info", e)
            "Unknown" // Fallback version name
        }

        val deviceData = hashMapOf(
            "lastSeen" to FieldValue.serverTimestamp(),
            "appVersion" to appVersion // Use the dynamic version here
        )

        firestoreDb.collection("devices").document(deviceId)
            .set(deviceData, SetOptions.merge()) // Use merge to avoid overwriting existing data
            .addOnSuccessListener { Log.d("Firestore", "Device document created/updated for ID: $deviceId") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error writing document", e) }
    }

    private fun fetchLastModifiedSlots() {
        lastSlotListener = firestoreDb.collection("WarehouseSlots")
            .orderBy("lastModified", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error receiving last slots from Firestore", error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val lastModifiedSlots = querySnapshot.documents.map { document ->
                        val firestoreSlot = document.toObject(FirestoreSlot::class.java)
                        // Build WarehouseSlot using FirestoreSlot and document.id
                        firestoreSlot?.toWarehouseSlot(document.id) // Pass document.id here
                    }

                    // Update the UI state with the new actions
                    _startScreenState.update {
                        it.copy(lastModifiedSlots = lastModifiedSlots.filterNotNull())
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        lastSlotListener?.remove()
    }
}

data class StartUiState(
    val scannedCode: String = "",
    val lastModifiedSlots: List<WarehouseSlot> = emptyList(),
)