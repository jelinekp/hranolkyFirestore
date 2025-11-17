package eu.jelinek.hranolky.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.model.SlotType
import eu.jelinek.hranolky.model.WarehouseSlot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    application: Application,
    private val slotRepository: SlotRepository
) : AndroidViewModel(application) {

    private val _historyScreenState = MutableStateFlow(HistoryScreenState())
    val historyScreenState get() = _historyScreenState.asStateFlow()

    private var jointerLoadJob: Job? = null

    init {
        viewModelScope.launch {
            slotRepository.getLastModifiedSlots(SlotType.Beam).collect { lastModifiedBeamSlots ->
                _historyScreenState.update {
                    it.copy(
                        lastModifiedBeamSlots = lastModifiedBeamSlots,
                    )
                }
            }
        }
    }

    fun loadJointerLastModifiedSlots() {
        // Prevent creating multiple collectors on repeated tab selections
        if (jointerLoadJob != null) return
        jointerLoadJob = viewModelScope.launch {
            slotRepository.getLastModifiedSlots(SlotType.Jointer)
                .collect { lastModifiedJointerSlots ->
                    _historyScreenState.update {
                        it.copy(
                            lastModifiedJointerSlots = lastModifiedJointerSlots
                        )
                    }
                }
        }
    }
}

data class HistoryScreenState(
    val lastModifiedBeamSlots: List<WarehouseSlot> = emptyList(),
    val lastModifiedJointerSlots: List<WarehouseSlot> = emptyList(),
)