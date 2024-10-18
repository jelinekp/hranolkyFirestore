package eu.jelinek.hranolky.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import eu.jelinek.hranolky.model.FirestoreSlot
import eu.jelinek.hranolky.model.WarehouseSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OverViewModel(
    private val firestoreDb : FirebaseFirestore
) : ViewModel() {
    private val _overviewScreenState = MutableStateFlow(OverviewUiState())
    val overviewScreenState get() = _overviewScreenState.asStateFlow()

    private var allSlotsListener: ListenerRegistration? = null

    init {
        fetchAllSlots()
    }

    fun fetchAllSlots() {
        firestoreDb.collection("WarehouseSlots")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error receiving all slots from Firestore", error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val setOfSlots = mutableSetOf<WarehouseSlot>()

                    querySnapshot.documents.forEach {
                        val firestoreSlot = it.toObject(FirestoreSlot::class.java)
                        firestoreSlot?.toWarehouseSlot(it.id)

                        if (firestoreSlot != null) {

                        }
                    }

                    val allSlots = querySnapshot.documents.map { document ->
                        val firestoreSlot = document.toObject(FirestoreSlot::class.java)
                        // Build WarehouseSlot using FirestoreSlot and document.id
                        firestoreSlot?.toWarehouseSlot(document.id) // Pass document.id here
                    }

                    // Update the UI state with the new actions
                    _overviewScreenState.update {
                        it.copy(allSlots = allSlots)
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        allSlotsListener?.remove()
    }
}

data class OverviewUiState(
    val allSlots: Set<WarehouseSlot> = emptySet(),
)