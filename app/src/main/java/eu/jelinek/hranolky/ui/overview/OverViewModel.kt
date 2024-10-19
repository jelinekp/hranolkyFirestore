package eu.jelinek.hranolky.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import eu.jelinek.hranolky.model.FirestoreSlot
import eu.jelinek.hranolky.model.WarehouseSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class OverViewModel(
    private val firestoreDb: FirebaseFirestore
) : ViewModel() {
    private val _overviewScreenState = MutableStateFlow(OverviewUiState())
    val overviewScreenState get() = _overviewScreenState.asStateFlow()

    private var allSlotsListener: ListenerRegistration? = null

    init {
        viewModelScope.launch {
            fetchAllSlots()
        }
        viewModelScope.launch {
            overviewScreenState.first { it.allSlots.isNotEmpty() }
                .also {
                    onFilterClear()
                } // Call onFilterClear after collecting the first non-empty value
        }
        viewModelScope.launch {
            applyAllFilters()
        }
        viewModelScope.launch {
            applySorting()
        }
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
                        it.copy(allSlots = allSlots.filterNotNull())
                    }
                }
            }
    }

    private fun calculateSumSlotInline(slotsToSum: List<WarehouseSlot>): SlotSum {
        return slotsToSum
            .fold(SlotSum.EMPTY) { acc, slot -> // Accumulate sums using fold
                SlotSum(
                    count = acc.count + 1,
                    quantitySum = acc.quantitySum + slot.quantity,
                    volumeSum = acc.volumeSum + (slot.getVolume() ?: 0.0)
                )
            }
    }

    override fun onCleared() {
        super.onCleared()
        allSlotsListener?.remove()
    }

    fun onQualityFilterChange(qualitiesFilters: List<String>) {
        _overviewScreenState.update { state ->
            state.copy(
                selectedFilters = state.selectedFilters.copy(qualityFilters = qualitiesFilters),
            )
        }
    }

    fun onThicknessFilterChange(thicknessFilters: List<Float>) {
        _overviewScreenState.update {
            it.copy(
                selectedFilters = it.selectedFilters.copy(thicknessFilters = thicknessFilters),
            )
        }
    }

    fun onWidthFilterChange(widthFilters: List<Float>) {
        _overviewScreenState.update {
            it.copy(
                selectedFilters = it.selectedFilters.copy(widthFilters = widthFilters),
            )
        }
    }

    fun onLengthFilterChange(mmsFilters: List<IntervalMm>) {
        _overviewScreenState.update {
            it.copy(
                selectedFilters = it.selectedFilters.copy(lengthFilters = mmsFilters),
            )
        }
    }

    private suspend fun applyAllFilters() {
        overviewScreenState.distinctUntilChanged { old, new ->
            old.selectedFilters == new.selectedFilters // every time selected filters changes
        }.collect { state ->
            val lsf =
                state.selectedFilters // here we have the latest screenState with latest selected filters (lsf)

            if (lsf.isEmpty()) {
                onFilterClear()
            } else {
                val selectedSlots = state.allSlots.filter { slot ->
                    (!lsf.hasQualityFilters()
                            || lsf.qualityFilters.contains(slot.quality))
                            &&
                            (!lsf.hasThicknessFilters() || lsf.thicknessFilters.contains(slot.thickness?.toFloat()))
                            &&
                            (!lsf.hasWidthFilters() || lsf.widthFilters.contains(slot.width?.toFloat()))
                            &&
                            (!lsf.hasLengthFilters() || lsf.lengthFilters.any {
                                it.contains(slot.length)
                            })
                }
                _overviewScreenState.update {
                    it.copy(
                        selectedSlots = selectedSlots,
                        sum = calculateSumSlotInline(selectedSlots)
                    )
                }
            }
        }
    }

    fun updateSorting(by: String) {
        var sortingBy = ""
        val currentDirection = _overviewScreenState.value.sortingDirection
        var sortingDirection = SortingDirection.NONE
        if (_overviewScreenState.value.sortingBy == by) {
            if (currentDirection == SortingDirection.DESC) {
                sortingDirection = SortingDirection.ASC
                sortingBy = by
            }
            else {
                sortingDirection = SortingDirection.NONE
                sortingBy = ""
            }
        } else {
            sortingBy = by
            sortingDirection = SortingDirection.DESC
        }

        Log.d("OverViewModel", "Sorting by: $sortingBy, Direction: $sortingDirection")

        _overviewScreenState.update {
            it.copy(
                sortingBy = sortingBy,
                sortingDirection = sortingDirection,
            )
        }
    }

    private suspend fun applySorting() {
        overviewScreenState.distinctUntilChanged { old, new ->
            old.sortingBy == new.sortingBy && old.selectedSlots == new.selectedSlots && old.sortingDirection == new.sortingDirection // every time sortingBy or selected changes
        }.collect { state ->
            when (state.sortingBy) {
                "length" -> {
                    _overviewScreenState.update {
                        it.copy(
                            sortedSlots = if (state.sortingDirection == SortingDirection.ASC) it.selectedSlots.sortedBy { it.length }
                            else if (state.sortingDirection == SortingDirection.DESC) it.selectedSlots.sortedByDescending { it.length }
                            else it.selectedSlots,
                        )
                    }
                }

                "width" -> {
                    _overviewScreenState.update {
                        it.copy(
                            sortedSlots = if (state.sortingDirection == SortingDirection.ASC) it.selectedSlots.sortedBy { it.width }
                            else if (state.sortingDirection == SortingDirection.DESC) it.selectedSlots.sortedByDescending { it.width }
                            else it.selectedSlots,
                        )
                    }
                }

                "thickness" -> {
                    _overviewScreenState.update {
                        it.copy(
                            sortedSlots = if (state.sortingDirection == SortingDirection.ASC) it.selectedSlots.sortedBy { it.thickness }
                            else if (state.sortingDirection == SortingDirection.DESC) it.selectedSlots.sortedByDescending { it.thickness }
                            else it.selectedSlots,
                        )
                    }
                }

                "quantity" -> {
                    _overviewScreenState.update {
                        it.copy(
                            sortedSlots = if (state.sortingDirection == SortingDirection.ASC) it.selectedSlots.sortedBy { it.quantity }
                            else if (state.sortingDirection == SortingDirection.DESC) it.selectedSlots.sortedByDescending { it.quantity }
                            else it.selectedSlots,
                        )
                    }
                }

                else -> {
                    _overviewScreenState.update {
                        it.copy(
                            sortedSlots = it.selectedSlots,
                        )
                    }
                }
            }
        }
    }

    fun onFilterClear() {
        _overviewScreenState.update {
            it.copy(
                selectedFilters = SlotFilters.EMPTY,
                selectedSlots = it.allSlots,
                sum = calculateSumSlotInline(it.allSlots)
            )
        }
    }
}

data class OverviewUiState(
    val allSlots: List<WarehouseSlot> = emptyList(),
    val selectedSlots: List<WarehouseSlot> = emptyList(),
    val sortedSlots: List<WarehouseSlot> = emptyList(),
    val sum: SlotSum = SlotSum.EMPTY,
    val selectedFilters: SlotFilters = SlotFilters.EMPTY,
    val sortingBy: String = "",
    val sortingDirection: SortingDirection = SortingDirection.NONE,
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
    val lengthFilters: List<IntervalMm>,
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
            lengthFilters = listOf(
                IntervalMm(0, 999),
                IntervalMm(1000, 1999),
                IntervalMm(2000, 2999)
            )
        )
    }

    fun isEmpty(): Boolean {
        return qualityFilters.isEmpty() && thicknessFilters.isEmpty() && widthFilters.isEmpty() && lengthFilters.isEmpty()
    }

    fun hasQualityFilters(): Boolean {
        return qualityFilters.isNotEmpty()
    }

    fun hasThicknessFilters(): Boolean {
        return thicknessFilters.isNotEmpty()
    }

    fun hasWidthFilters(): Boolean {
        return widthFilters.isNotEmpty()
    }

    fun hasLengthFilters(): Boolean {
        return lengthFilters.isNotEmpty()
    }
}

data class IntervalMm(
    val start: Int,
    val end: Int,
) {
    fun contains(value: Int?): Boolean {
        return value in start..end
    }

    override fun toString(): String {
        return "$start - $end"
    }
}

enum class SortingDirection {
    ASC, DESC, NONE
}
