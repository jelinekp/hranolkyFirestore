package eu.jelinek.hranolky.ui.manageitem

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.domain.AddSlotActionUseCase
import eu.jelinek.hranolky.model.ActionType
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.navigation.Screen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

class ManageItemViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val slotRepository: SlotRepository,
    private val addSlotActionUseCase: AddSlotActionUseCase
) : AndroidViewModel(application) {

    val slotId: String? = savedStateHandle[Screen.ManageItemScreen.ID]
    private val _screenStateStream =
        MutableStateFlow<ManageItemScreenState>(ManageItemScreenState())
    val screenStateStream get() = _screenStateStream.asStateFlow()

    private val _validationSharedFlowStream = MutableSharedFlow<AddActionValidationState>()
    val validationSharedFlowStream get() = _validationSharedFlowStream.asSharedFlow()

    var quantityState = mutableStateOf("")
        private set

    private companion object {
        const val PREF_INVENTORY_CHECK_ENABLED = "inventory_check_enabled"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(getApplication())
    }

    fun onQuantityChanged(newQuantity: String) {
        quantityState.value = newQuantity
    }

    init {
        viewModelScope.launch {
            fetchSlotData()
        }
        loadInventoryCheckSetting()
    }

    private fun loadInventoryCheckSetting() {
        val inventoryCheckEnabled =
            sharedPreferences.getBoolean(PREF_INVENTORY_CHECK_ENABLED, false)
        _screenStateStream.update {
            it.copy(isInventoryCheckEnabled = inventoryCheckEnabled)
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
        viewModelScope.launch {
            val result = addSlotActionUseCase(
                slotId = slotId!!,
                slot = screenStateStream.value.slot!!,
                actionType = actionType,
                quantity = quantityState.value,
                currentQuantity = screenStateStream.value.slot?.quantity ?: 0,
                deviceId = getDeviceId()
            )
            if (result.isSuccess) {
                resetFields()
            } else {
                val validationState = when (result.exceptionOrNull()) {
                    is IllegalArgumentException -> AddActionValidationState(isQuantityError = true)
                    is IllegalStateException -> AddActionValidationState(isRemovedError = true)
                    else -> AddActionValidationState()
                }
                _validationSharedFlowStream.emit(validationState)
            }
        }
    }

    fun showSettingPopup() {
        viewModelScope.launch {
            try {
                if (quantityState.value.toLong() != (screenStateStream.value.slot?.quantity ?: 0)) {
                    val diff = quantityState.value.toLong() - (screenStateStream.value.slot?.quantity ?: 0)
                    val compareString = if (diff > 0) "více" else "méně"
                    _screenStateStream.update {
                        it.copy(
                            showConfirmSettingPopup = true,
                            inventoryCheckPopupMessage = InventoryCheckPopupMessage(
                                diff = abs(diff),
                                compareString = compareString
                            )
                        )
                    }
                }
                else
                    performInventoryQuantitySet()
            } catch (_: NumberFormatException) {
                _validationSharedFlowStream.emit(AddActionValidationState(isQuantityError = true))
            }
        }
    }

    fun onDismissSettingPopup() {
        _screenStateStream.update {
            it.copy(showConfirmSettingPopup = false)
        }
    }

    fun onConfirmSettingPopup() {
        performInventoryQuantitySet()

        _screenStateStream.update {
            it.copy(showConfirmSettingPopup = false)
        }
    }

    private fun performInventoryQuantitySet() {
        addActionToTheSlot(ActionType.INVENTORY_CHECK)
    }

    private fun resetFields() {
        quantityState.value = ""

        viewModelScope.launch {
            _validationSharedFlowStream.emit(AddActionValidationState())
        }
    }
}

data class ManageItemScreenState(
    val slot: WarehouseSlot? = null,
    val resultStatus: ResultStatus = ResultStatus.LOADING,
    val error: String? = null,
    val isOnline: Boolean = true,
    val isInventoryCheckEnabled: Boolean = false,
    val showConfirmSettingPopup: Boolean = false,
    val inventoryCheckPopupMessage: InventoryCheckPopupMessage = InventoryCheckPopupMessage()
)


data class AddActionValidationState(
    val isQuantityError: Boolean = false,
    val isRemovedError: Boolean = false,
)

enum class ResultStatus {
    LOADING, SUCCESS, NETWORK_ERROR, DATA_ERROR, OTHER_ERROR
}

data class InventoryCheckPopupMessage (
    val diff: Long = 0,
    val compareString: String = "stejný",
)