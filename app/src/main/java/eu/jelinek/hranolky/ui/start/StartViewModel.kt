package eu.jelinek.hranolky.ui.start

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import eu.jelinek.hranolky.model.FirestoreSlot
import eu.jelinek.hranolky.model.WarehouseSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StartViewModel(
    private val firestoreDb: FirebaseFirestore,
) : ViewModel() {
    private val _startScreenState = MutableStateFlow(StartUiState())
    val startScreenState get() = _startScreenState.asStateFlow()

    private var lastSlotListener: ListenerRegistration? = null

    fun updateScannedCode(scannedCode: String) {
        _startScreenState.value = _startScreenState.value.copy(scannedCode = scannedCode)
    }

    fun updateLastSlots(lastModifiedSlots: List<WarehouseSlot?>) {
        _startScreenState.value = _startScreenState.value.copy(lastModifiedSlots = lastModifiedSlots)
    }

    init {
        fetchLastModifiedSlots()
    }

    fun fetchLastModifiedSlots() {
        firestoreDb.collection("WarehouseSlots")
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
                        it.copy(lastModifiedSlots = lastModifiedSlots)
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
    val lastModifiedSlots: List<WarehouseSlot?> = emptyList(),
)