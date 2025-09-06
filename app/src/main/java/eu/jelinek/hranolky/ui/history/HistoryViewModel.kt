package eu.jelinek.hranolky.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.model.WarehouseSlot
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

    init {
        viewModelScope.launch {
            slotRepository.getLastModifiedSlots().collect { lastModifiedSlots ->
                _historyScreenState.update {
                    it.copy(
                        lastModifiedBeamSlots = lastModifiedSlots.beamSlots,
                        lastModifiedJointerSlots = lastModifiedSlots.jointerSlots
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