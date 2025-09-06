package eu.jelinek.hranolky.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.model.SlotType
import eu.jelinek.hranolky.model.WarehouseSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class OverViewModel(
    private val slotRepository: SlotRepository
) : ViewModel() {
    private val _overviewScreenState = MutableStateFlow(OverviewUiState())
    val overviewScreenState get() = _overviewScreenState.asStateFlow()

    init {
        viewModelScope.launch {
            slotRepository.getAllSlots(slotType = _overviewScreenState.value.slotType)
                .collect { slots ->
                    updateSlotsAndLoadAvailableFilters(slots, _overviewScreenState.value.slotType)
                }
        }
        viewModelScope.launch {
            overviewScreenState.first { it.allSlots.isNotEmpty() }
                .also {
                    onFilterClear()
                }
        }
        viewModelScope.launch {
            applyAllFilters()
        }
        viewModelScope.launch {
            applySorting()
        }
    }

    private fun updateSlotsAndLoadAvailableFilters(slots: List<WarehouseSlot>, slotType: SlotType) {
        _overviewScreenState.update { uiState ->
            // When allSlots changes, also recalculate allFilters
            val distinctQualityFilters = slots
                .mapNotNull { it.quality }
                .filter { it.isNotBlank() }
                .toSet().sorted().toList()
            val distinctThicknessFilters = slots
                .mapNotNull { it.thickness }
                .filter { it != 0f }
                .toSet().sorted().toList()
            val distinctWidthFilters = slots
                .mapNotNull { it.width }
                .filter { it != 0f }
                .toSet().sorted().toList()

            uiState.copy(
                slotType = slotType,
                allSlots = slots,
                allFilters = SlotFilters(
                    qualityFilters = distinctQualityFilters,
                    thicknessFilters = distinctThicknessFilters,
                    widthFilters = distinctWidthFilters,
                    lengthFilters = uiState.allFilters.lengthFilters
                )
            )
        }
    }

    private fun calculateSumSlotInline(slotsToSum: List<WarehouseSlot>): SlotSum {
        return slotsToSum
            .fold(SlotSum.EMPTY) { acc, slot ->
                SlotSum(
                    count = acc.count + 1,
                    quantitySum = acc.quantitySum + slot.quantity,
                    volumeSum = acc.volumeSum + (slot.getVolume() ?: 0.0)
                )
            }
    }

    fun onTypeChange(slotType: SlotType) {
        viewModelScope.launch {
            slotRepository.getAllSlots(slotType = slotType).collect { slots ->
                updateSlotsAndLoadAvailableFilters(slots, slotType)
            }
        }
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
            old.selectedFilters == new.selectedFilters && old.allSlots == new.allSlots
        }.collect { state ->
            val lsf =
                state.selectedFilters

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
            } else {
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
            old.sortingBy == new.sortingBy && old.selectedSlots == new.selectedSlots && old.sortingDirection == new.sortingDirection
        }.collect { state ->
            when (state.sortingBy) {
                "length" -> {
                    _overviewScreenState.update {
                        it.copy(
                            sortedSlots = when (state.sortingDirection) {
                                SortingDirection.ASC -> it.selectedSlots.sortedBy { it.length }
                                SortingDirection.DESC -> it.selectedSlots.sortedByDescending { it.length }
                                else -> it.selectedSlots
                            },
                        )
                    }
                }

                "width" -> {
                    _overviewScreenState.update {
                        it.copy(
                            sortedSlots = when (state.sortingDirection) {
                                SortingDirection.ASC -> it.selectedSlots.sortedBy { it.width }
                                SortingDirection.DESC -> it.selectedSlots.sortedByDescending { it.width }
                                else -> it.selectedSlots
                            },
                        )
                    }
                }

                "thickness" -> {
                    _overviewScreenState.update {
                        it.copy(
                            sortedSlots = when (state.sortingDirection) {
                                SortingDirection.ASC -> it.selectedSlots.sortedBy { it.thickness }
                                SortingDirection.DESC -> it.selectedSlots.sortedByDescending { it.thickness }
                                else -> it.selectedSlots
                            },
                        )
                    }
                }

                "quantity" -> {
                    _overviewScreenState.update {
                        it.copy(
                            sortedSlots = when (state.sortingDirection) {
                                SortingDirection.ASC -> it.selectedSlots.sortedBy { it.quantity }
                                SortingDirection.DESC -> it.selectedSlots.sortedByDescending { it.quantity }
                                else -> it.selectedSlots
                            },
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
    val allFilters: SlotFilters = SlotFilters.LENGTH_INTERVALS,
    val selectedFilters: SlotFilters = SlotFilters.EMPTY,
    val sortingBy: String = "quantity",
    val sortingDirection: SortingDirection = SortingDirection.DESC,
    val slotType: SlotType = SlotType.Beam,
)

data class SlotSum(
    val count: Int,
    val quantitySum: Long,
    val volumeSum: Double,
) {
    companion object {
        val EMPTY = SlotSum(0, 0L, 0.0)
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
        /*val ALL = SlotFilters(
            qualityFilters = listOf("DUB-A", "DUB-R"),
            thicknessFilters = listOf(20f, 27.4f, 42.4f),
            widthFilters = listOf(38f, 40f, 42.4f, 50f, 70f),
            lengthFilters = listOf(
                IntervalMm(0, 999),
                IntervalMm(1000, 1999),
                IntervalMm(2000, 2999)
            )
        )*/

        val LENGTH_INTERVALS = SlotFilters(
                qualityFilters = emptyList(),
                thicknessFilters = emptyList(),
                widthFilters = emptyList(),
                lengthFilters = listOf(
                    IntervalMm(0, 599),
                    IntervalMm(600, 1199),
                    IntervalMm(1200, 1799),
                    IntervalMm(1800, 2399),
                    IntervalMm(2400, 2999)
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

    fun getNumberOfActiveFilters(): Int {
        return qualityFilters.size + thicknessFilters.size + widthFilters.size + lengthFilters.size
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