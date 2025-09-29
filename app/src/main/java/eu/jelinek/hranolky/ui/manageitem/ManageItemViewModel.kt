package eu.jelinek.hranolky.ui.manageitem

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import android.icu.util.Calendar
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
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

@RequiresApi(Build.VERSION_CODES.N)
class ManageItemViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val slotRepository: SlotRepository,
    private val addSlotActionUseCase: AddSlotActionUseCase
) : AndroidViewModel(application) {

    val TAG = "ManageItemViewModel"

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
            loadInventoryCheckSetting()
            fetchSlotData()
        }
    }

    private fun loadInventoryCheckSetting() {
        val inventoryCheckEnabled =
            sharedPreferences.getBoolean(PREF_INVENTORY_CHECK_ENABLED, false)
        _screenStateStream.update {
            it.copy(isInventoryCheckEnabled = inventoryCheckEnabled)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun checkInventoryDone() {
        val slotActions = _screenStateStream.value.slot?.slotActions
        if (slotActions.isNullOrEmpty()) {
            _screenStateStream.update { it.copy(isInventoryCheckDone = false) }
            Log.d(TAG, "checkInventoryDone: No slot actions found.")
            return
        }

        // Calculate the date 80 days ago
        val calendar80DaysAgo = Calendar.getInstance()
        calendar80DaysAgo.add(Calendar.DAY_OF_YEAR, -80)
        val limitDate = calendar80DaysAgo.time // This is a java.util.Date

        Log.d(TAG, "checkInventoryDone: Checking for INVENTORY_CHECK actions newer than $limitDate")

        val recentInventoryCheckExists = slotActions.any { slotAction ->
            // Assuming slotAction.action is a String that matches ActionType enum names
            // And slotAction.timestamp is a com.google.firebase.Timestamp
            if (slotAction.action == ActionType.INVENTORY_CHECK.toString() && slotAction.timestamp != null) {
                val actionDate = slotAction.timestamp.toDate() // Convert Firebase Timestamp to java.util.Date
                val isRecent = actionDate.after(limitDate)
                if (isRecent) {
                    Log.d(TAG, "checkInventoryDone: Found recent INVENTORY_CHECK action: $slotAction at $actionDate")
                }
                isRecent
            } else {
                false
            }
        }

        if (_screenStateStream.value.isInventoryCheckDone != recentInventoryCheckExists) {
            _screenStateStream.update { it.copy(isInventoryCheckDone = recentInventoryCheckExists) }
            Log.d(TAG, "checkInventoryDone: Updated isInventoryCheckDone to $recentInventoryCheckExists")
        } else {
            Log.d(TAG, "checkInventoryDone: isInventoryCheckDone state is already $recentInventoryCheckExists, no update.")
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
                    slotRepository.getSlotActions(id) // Ensure this flow also emits when actions change
                ) { slot, actions ->
                    // This lambda will be called whenever slot or actions change
                    if (slot != null) {
                        val parsedSlot = slot.parsePropertiesFromProductId().copy(slotActions = actions)
                        _screenStateStream.update {
                            it.copy(slot = parsedSlot, resultStatus = ResultStatus.SUCCESS)
                        }
                        // After updating the slot with actions, if inventory check is enabled,
                        // the collector in init will pick up the change and call checkInventoryDone.
                        // Or, you could call it explicitly here too if preferred,
                        // but the collector pattern is more robust for ongoing updates.
                        if (_screenStateStream.value.isInventoryCheckEnabled) {
                            checkInventoryDone()
                        }
                    } else {
                        // Slot not found, potentially create it or handle error
                        // If creating new, slotActions will be empty initially.
                        Log.d(TAG, "fetchSlotData: Slot with ID $id not found. Attempting to create.")
                        slotRepository.createNewSlot(id, 0) // This might then trigger another emission
                        _screenStateStream.update {
                            it.copy(slot = null, resultStatus = ResultStatus.DATA_ERROR, error = "Slot not found")
                        }
                    }
                }.collect {
                    // Collection is handled by the combine operator.
                    // The block inside combine is the actual processing per emission.
                    Log.d(TAG, "fetchSlotData: combine block executed.")
                }
            }
        } ?: run {
            _screenStateStream.update { it.copy(resultStatus = ResultStatus.DATA_ERROR, error = "Slot ID is null") }
        }
    }

    fun addActionToTheSlot(actionType: ActionType) {
        viewModelScope.launch {
            val currentSlot = screenStateStream.value.slot
            if (slotId == null || currentSlot == null) {
                Log.e(TAG, "addActionToTheSlot: slotId or currentSlot is null. Cannot add action.")
                _validationSharedFlowStream.emit(AddActionValidationState(isQuantityError = true)) // Generic error
                return@launch
            }

            val result = addSlotActionUseCase(
                slotId = slotId,
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
    val inventoryCheckPopupMessage: InventoryCheckPopupMessage = InventoryCheckPopupMessage(),
    val isInventoryCheckDone: Boolean = false,
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