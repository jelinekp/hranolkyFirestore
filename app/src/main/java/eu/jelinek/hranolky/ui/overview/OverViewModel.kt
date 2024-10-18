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

    private fun fetchAllSlots() {
        firestoreDb.collection("WarehouseSlots")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error receiving all slots from Firestore", error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val allSlots = querySnapshot.documents.map {
                        val firestoreSlot = it.toObject(FirestoreSlot::class.java)
                        firestoreSlot?.toWarehouseSlot(it.id)
                    }

                    // Update the UI state with the new actions
                    _overviewScreenState.update {
                        it.copy(allSlots = allSlots)
                    }
                    calculateSumSlot()
                }
            }
    }

    private fun calculateSumSlot() {
        val allSlots = _overviewScreenState.value.allSlots
        val sumSlot = allSlots.filterNotNull() // Filter out null slots
            .fold(SlotSum.EMPTY) { acc, slot -> // Accumulate sums using fold
                SlotSum(
                    count = acc.count + 1,
                    quantitySum = acc.quantitySum + slot.quantity,
                    volumeSum = acc.volumeSum + (slot.getVolume() ?: 0.0)
                )
            }
        _overviewScreenState.update { it.copy(sum = sumSlot) }
    }

    fun onFilterChange(list: List<Any>) {
        _overviewScreenState.update { it.copy(selectedFilters = list) }
    }

    override fun onCleared() {
        super.onCleared()
        allSlotsListener?.remove()
    }
}

data class OverviewUiState(
    val allSlots: List<WarehouseSlot?> = emptyList(),
    val sum: SlotSum = SlotSum.EMPTY,
    val selectedFilters: List<Any> = emptyList(),
)

data class SlotSum(
    val count: Int,
    val quantitySum: Int,
    val volumeSum: Double,
) {
    companion object {
        val EMPTY = SlotSum(0, 0, 0.0)
    }
}

data class SlotFilters(
    val qualityFilters: List<String>,
    val thicknessFilters: List<Float>,
    val widthFilters: List<Float>,
    val lengthFilters: List<Pair<Int, Int>>,
) {
    companion object {
        val EMPTY = SlotFilters(
            qualityFilters = emptyList(),
            thicknessFilters = emptyList(),
            widthFilters = emptyList(),
            lengthFilters = emptyList(),
        )
        val ALL = SlotFilters(
            qualityFilters = listOf("DUB-A", "DUB-R"),
            thicknessFilters = listOf(20f, 27.4f, 42.4f),
            widthFilters = listOf(42.4f, 50f, 70f),
            lengthFilters = listOf(Pair(0, 999), Pair(1000, 1999), Pair(2000, 2999))
        )
    }
}