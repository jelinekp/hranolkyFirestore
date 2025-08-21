package eu.jelinek.hranolky.ui.showlast

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.navigation.Screen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShowLastActionsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val slotRepository: SlotRepository,
) : AndroidViewModel(application) {

    val slotId: String? = savedStateHandle[Screen.ShowLastActionsScreen.ID]
    private val _screenStateStream =
        MutableStateFlow<ShowLastActionsScreenState>(ShowLastActionsScreenState())
    val screenStateStream get() = _screenStateStream.asStateFlow()

    private val _validationSharedFlowStream = MutableSharedFlow<AddActionValidationState>()
    val validationSharedFlowStream get() = _validationSharedFlowStream.asSharedFlow()

    var quantityState = mutableStateOf("")
        private set

    fun onQuantityChanged(newQuantity: String) {
        quantityState.value = newQuantity
    }

    val TAG = "Firestore"

    init {
        viewModelScope.launch {
            fetchSlotData()
        }
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        return Settings.Secure.getString(getApplication<Application>().contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun fetchSlotData() {
        slotId?.let { id ->
            viewModelScope.launch {
                combine(
                    slotRepository.getSlot(id),
                    slotRepository.getSlotActions(id)
                ) { slot, actions ->
                    if (slot != null) {
                        val parsedSlot = slot.parsePropertiesFromProductId().copy(slotActions = actions)
                        _screenStateStream.update {
                            it.copy(slot = parsedSlot, resultStatus = ResultStatus.SUCCESS)
                        }
                    } else {
                        slotRepository.createNewSlot(id, 0)
                    }
                }.collect { }
            }
        }
    }

    fun addActionToTheSlot(actionType: ActionType) {
        if (validateInputs(actionType)) {
            viewModelScope.launch {
                slotId?.let {
                    slotRepository.addSlotAction(
                        it,
                        actionType,
                        quantityState.value.toLong(),
                        screenStateStream.value.slot?.quantity ?: 0,
                        getDeviceId()
                    )
                }
                resetFields()
            }
        }
    }

    private fun resetFields() {
        quantityState.value = ""

        viewModelScope.launch {
            _validationSharedFlowStream.emit(AddActionValidationState())
        }
    }

    private fun validateInputs(actionType: ActionType): Boolean {

        val quantity = quantityState.value.toDoubleOrNull()
        if (quantity == null || quantity <= 0 || quantity > 9_999) {
            viewModelScope.launch {
                _validationSharedFlowStream.emit(AddActionValidationState(isQuantityError = true))
            }
            return false
        } else if (actionType == ActionType.REMOVE && quantity > screenStateStream.value.slot!!.quantity) {
            viewModelScope.launch {
                _validationSharedFlowStream.emit(AddActionValidationState(isRemovedError = true))
            }
            return false
        }

        return true
    }
}

data class ShowLastActionsScreenState(
    val slot: WarehouseSlot? = null,
    val resultStatus: ResultStatus = ResultStatus.LOADING,
    val error: String? = null,
    val isOnline: Boolean = true,
)


data class AddActionValidationState(
    val isQuantityError: Boolean = false,
    val isRemovedError: Boolean = false,
)

enum class ResultStatus {
    LOADING, SUCCESS, NETWORK_ERROR, DATA_ERROR, OTHER_ERROR
}

enum class ActionType {
    ADD, REMOVE;

    override fun toString(): String {
        return when (this) {
            ADD -> "prijem"
            REMOVE -> "vydej"
        }
    }
}