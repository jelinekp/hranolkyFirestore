package eu.jelinek.hranolky.ui.manageitem

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.data.preferences.InventoryCheckPreferencesRepository
import eu.jelinek.hranolky.domain.AddSlotActionUseCase
import eu.jelinek.hranolky.domain.CheckInventoryStatusUseCase
import eu.jelinek.hranolky.domain.InputValidator
import eu.jelinek.hranolky.domain.QuantityParser
import eu.jelinek.hranolky.domain.UndoSlotActionUseCase
import eu.jelinek.hranolky.domain.navigation.ManageItemNavigationCoordinator
import eu.jelinek.hranolky.domain.navigation.NavigationResult
import eu.jelinek.hranolky.model.ActionType
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.navigation.Screen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Data class representing an action that can be undone
 */
data class UndoableAction(
    val fullSlotId: String,
    val actionDocumentId: String,
    val quantityChange: Long,
    val actionType: ActionType
)

/**
 * ViewModel for managing individual warehouse slot items.
 *
 * This ViewModel has been refactored following the Separation of Concerns (SoC) principle:
 * - QuantityParser: Handles quantity string parsing and validation
 * - CheckInventoryStatusUseCase: Determines if inventory check is recent
 * - InventoryCheckPreferencesRepository: Manages inventory check toggle persistence
 * - UndoSlotActionUseCase: Handles undo operations
 * - ManageItemNavigationCoordinator: Coordinates navigation to other items
 * - AddSlotActionUseCase: Handles adding actions to slots
 */
class ManageItemViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val slotRepository: SlotRepository,
    private val addSlotActionUseCase: AddSlotActionUseCase,
    private val inputValidator: InputValidator,
    private val quantityParser: QuantityParser,
    private val checkInventoryStatusUseCase: CheckInventoryStatusUseCase,
    private val inventoryCheckPreferences: InventoryCheckPreferencesRepository,
    private val undoSlotActionUseCase: UndoSlotActionUseCase,
    private val navigationCoordinator: ManageItemNavigationCoordinator
) : AndroidViewModel(application) {

    private val TAG = "ManageItemViewModel"

    val fullSlotId: String? = savedStateHandle[Screen.ManageItemScreen.ID]
    private val _screenStateStream =
        MutableStateFlow<ManageItemScreenState>(
            ManageItemScreenState(
                screenTitle = this.fullSlotId ?: ""
            )
        )
    val screenStateStream get() = _screenStateStream.asStateFlow()

    private val _validationSharedFlowStream = MutableSharedFlow<AddActionValidationState>()
    val validationSharedFlowStream get() = _validationSharedFlowStream.asSharedFlow()

    // Navigation events delegated to coordinator
    val navigateToAnotherItem = navigationCoordinator.navigateToAnotherItem

    // Undo snackbar events
    private val _undoSnackbarEvent = MutableSharedFlow<UndoableAction>()
    val undoSnackbarEvent = _undoSnackbarEvent.asSharedFlow()

    // Error snackbar events (for undo failures)
    private val _errorSnackbarEvent = MutableSharedFlow<String>()
    val errorSnackbarEvent = _errorSnackbarEvent.asSharedFlow()

    var quantityState = mutableStateOf("")
        private set

    fun onQuantityChanged(newQuantity: String) {
        quantityState.value = newQuantity
    }

    init {
        viewModelScope.launch {
            loadInventoryCheckSetting()
            fetchSlotData()
        }
    }

    /**
     * Load inventory check setting from preferences repository (SoC extraction)
     */
    private fun loadInventoryCheckSetting() {
        val inventoryCheckEnabled = inventoryCheckPreferences.isInventoryCheckEnabled()
        _screenStateStream.update {
            it.copy(isInventoryCheckEnabled = inventoryCheckEnabled)
        }
    }

    /**
     * Check if inventory has been verified recently using extracted use case (SoC extraction)
     */
    private fun checkInventoryDone() {
        val slotActions = _screenStateStream.value.slot?.slotActions
        val isInventoryCheckDone = checkInventoryStatusUseCase.isInventoryCheckDone(slotActions)

        if (_screenStateStream.value.isInventoryCheckDone != isInventoryCheckDone) {
            _screenStateStream.update { it.copy(isInventoryCheckDone = isInventoryCheckDone) }
            Log.d(TAG, "checkInventoryDone: Updated isInventoryCheckDone to $isInventoryCheckDone")
        }
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    private fun updateSlotAndTitleScreen(slot: WarehouseSlot) {

        val slotTitle = slot.getScreenTitle()

        Log.d(
            TAG,
            "updateSlotAndTitleScreen: Updating screen state - slot: ${slot.fullProductId}, actions count: ${slot.slotActions.size}"
        )

        _screenStateStream.update {
            it.copy(
                slot = slot,
                screenTitle = slotTitle,
                resultStatus = ResultStatus.SUCCESS
            )
        }

        Log.d(
            TAG,
            "updateSlotAndTitleScreen: Screen state updated - current actions count: ${_screenStateStream.value.slot?.slotActions?.size}"
        )
    }

    private fun fetchSlotData() {
        fullSlotId?.let { id ->
            viewModelScope.launch {
                try {
                    combine(
                        slotRepository.getSlot(id),
                        slotRepository.getSlotActions(id) // Ensure this flow also emits when actions change
                    ) { slot, actions ->
                        // This lambda will be called whenever slot or actions change
                        Log.d(
                            TAG,
                            "fetchSlotData: combine triggered - slot: ${slot?.fullProductId}, actions count: ${actions.size}"
                        )
                        if (slot != null) {
                            val parsedSlot = slot.copy(slotActions = actions)
                            updateSlotAndTitleScreen(parsedSlot)

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
                            Log.d(
                                TAG,
                                "fetchSlotData: Slot with ID $id not found. Attempting to create."
                            )
                            slotRepository.createNewSlot(
                                id,
                                0
                            ) // This might then trigger another emission
                            _screenStateStream.update {
                                it.copy(
                                    slot = null,
                                    resultStatus = ResultStatus.DATA_ERROR,
                                    error = "Slot not found"
                                )
                            }
                        }
                    }
                        .distinctUntilChanged() // Prevent duplicate emissions when slot data hasn't actually changed
                        .collect {
                            // Collection is handled by the combine operator.
                            // The block inside combine is the actual processing per emission.
                            Log.d(TAG, "fetchSlotData: combine block collected.")
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "fetchSlotData: Error in flow collection", e)
                    _screenStateStream.update {
                        it.copy(
                            resultStatus = ResultStatus.OTHER_ERROR,
                            error = "Error fetching data: ${e.message}"
                        )
                    }
                }
            }
        } ?: run {
            _screenStateStream.update {
                it.copy(
                    resultStatus = ResultStatus.DATA_ERROR,
                    error = "Slot ID is null"
                )
            }
        }
    }

    fun addActionToTheSlot(actionType: ActionType) {
        viewModelScope.launch {
            val currentSlot = screenStateStream.value.slot
            if (fullSlotId == null || currentSlot == null) {
                Log.e(TAG, "addActionToTheSlot: slotId or currentSlot is null.")
                _validationSharedFlowStream.emit(
                    AddActionValidationState(
                        isQuantityError = true,
                        errorMessage = "Položka nenalezena."
                    )
                )
                return@launch
            }

            val parsedQuantityResult = parseQuantityStringToLong(quantityState.value)
            val quantityLong: Long

            if (parsedQuantityResult.isSuccess) {
                quantityLong = parsedQuantityResult.getOrThrow()
                // Quantity must be positive or zero for INVENTORY_CHECK
                if (quantityLong < 0 && actionType == ActionType.INVENTORY_CHECK || quantityLong <= 0 && actionType != ActionType.INVENTORY_CHECK) {
                    _validationSharedFlowStream.emit(
                        AddActionValidationState(
                            isQuantityError = true,
                            errorMessage = "Zadané množství musí být kladné."
                        )
                    )
                    return@launch
                }
            } else {
                val errorMessage =
                    parsedQuantityResult.exceptionOrNull()?.message ?: "Neplatný formát množství."
                Log.e(
                    TAG,
                    "addActionToTheSlot: Invalid quantity format: ${quantityState.value}",
                    parsedQuantityResult.exceptionOrNull()
                )
                _validationSharedFlowStream.emit(
                    AddActionValidationState(
                        isQuantityError = true,
                        errorMessage = errorMessage
                    )
                )
                return@launch
            }

            // For INVENTORY_CHECK, quantityLong is the *new total* quantity.
            // For ADD/REMOVE, quantityLong is the *change* in quantity.
            val result = addSlotActionUseCase(
                fullSlotId = fullSlotId,
                slot = currentSlot,
                actionType = actionType,
                quantity = quantityLong.toString(), // Use the parsed Long
                currentQuantity = currentSlot.quantity,
                deviceId = getDeviceId()
            )

            if (result.isSuccess) {
                val actionResult = result.getOrThrow()
                resetFields()

                // Emit undo event for snackbar
                _undoSnackbarEvent.emit(
                    UndoableAction(
                        fullSlotId = fullSlotId,
                        actionDocumentId = actionResult.actionDocumentId,
                        quantityChange = actionResult.quantityChange,
                        actionType = actionType
                    )
                )
            } else {
                val exception = result.exceptionOrNull()
                Log.e(TAG, "addActionToTheSlot: Error adding action.", exception)
                val validationState = when (exception) {
                    is IllegalArgumentException -> AddActionValidationState(
                        isQuantityError = true,
                        errorMessage = exception.message
                    )

                    is IllegalStateException -> AddActionValidationState(
                        isRemovedError = true,
                        errorMessage = exception.message
                    )

                    else -> AddActionValidationState(
                        errorMessage = exception?.message ?: "Neznámá chyba."
                    )
                }
                _validationSharedFlowStream.emit(validationState)
            }
        }
    }

    /**
     * Undo a previously performed action using extracted use case (SoC extraction)
     */
    fun undoLastAction(undoableAction: UndoableAction) {
        viewModelScope.launch {
            val result = undoSlotActionUseCase.execute(
                fullSlotId = undoableAction.fullSlotId,
                actionDocumentId = undoableAction.actionDocumentId,
                quantityChange = undoableAction.quantityChange
            )

            if (result.isFailure) {
                val error = result.exceptionOrNull()
                Log.e(TAG, "undoLastAction: Failed to undo action", error)
                _errorSnackbarEvent.emit("Nepodařilo se vrátit změnu: ${error?.message ?: "Chyba sítě"}")
            }
        }
    }

    fun showSettingPopup() {
        viewModelScope.launch {
            val currentSlotQuantity = screenStateStream.value.slot?.quantity ?: 0L
            val parsedQuantityResult = parseQuantityStringToLong(quantityState.value)
            val enteredQuantityLong: Long

            if (parsedQuantityResult.isSuccess) {
                enteredQuantityLong = parsedQuantityResult.getOrThrow()
                if (enteredQuantityLong < 0) { // For inventory check, new quantity cannot be negative
                    _validationSharedFlowStream.emit(
                        AddActionValidationState(
                            isQuantityError = true,
                            errorMessage = "Nové množství nemůže být záporné."
                        )
                    )
                    return@launch
                }
            } else {
                val errorMessage =
                    parsedQuantityResult.exceptionOrNull()?.message ?: "Neplatný formát množství."
                Log.e(
                    TAG,
                    "showSettingPopup: Invalid quantity format: ${quantityState.value}",
                    parsedQuantityResult.exceptionOrNull()
                )
                _validationSharedFlowStream.emit(
                    AddActionValidationState(
                        isQuantityError = true,
                        errorMessage = errorMessage
                    )
                )
                return@launch
            }

            if (enteredQuantityLong != currentSlotQuantity) {
                val diff = enteredQuantityLong - currentSlotQuantity
                val compareString = if (diff > 0) "více" else "méně"
                quantityState.value = enteredQuantityLong.toString()
                _screenStateStream.update {
                    it.copy(
                        showConfirmSettingPopup = true,
                        inventoryCheckPopupMessage = InventoryCheckPopupMessage(
                            diff = abs(diff),
                            compareString = compareString
                        )
                    )
                }
            } else {
                // If quantities are the same, still perform INVENTORY_CHECK to record the timestamp
                performInventoryQuantitySet()
            }
        }
    }

    /**
     * Parses a quantity string using the extracted QuantityParser (SoC extraction).
     * Handles item code detection and navigation via NavigationCoordinator.
     */
    private fun parseQuantityStringToLong(quantityStr: String): Result<Long> {
        val parseResult = quantityParser.parse(
            quantityStr = quantityStr,
            itemCodeValidator = { itemCode ->
                // Check if it's a valid item code for navigation
                val result = navigationCoordinator.checkForItemCodeNavigation(
                    scannedCode = itemCode,
                    currentSlotId = fullSlotId
                )
                when (result) {
                    is NavigationResult.NavigateToItem -> result.itemCode
                    is NavigationResult.ContinueWithInput -> null
                }
            }
        )

        return when (parseResult) {
            is QuantityParser.ParseResult.Success -> Result.success(parseResult.quantity)
            is QuantityParser.ParseResult.Error -> Result.failure(NumberFormatException(parseResult.message))
            is QuantityParser.ParseResult.ItemCodeDetected -> {
                // Trigger navigation via coordinator
                viewModelScope.launch {
                    Log.d(TAG, "parseQuantityStringToLong: Item code detected: ${parseResult.itemCode}. Triggering navigation.")
                    navigationCoordinator.navigateToItem(parseResult.itemCode)
                    resetFields()
                }
                // Return a special error to indicate navigation is happening
                Result.failure(NumberFormatException("Přesměrování na jinou položku..."))
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
    val screenTitle: String = "",
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
    val errorMessage: String? = null
)

enum class ResultStatus {
    LOADING, SUCCESS, NETWORK_ERROR, DATA_ERROR, OTHER_ERROR
}

data class InventoryCheckPopupMessage(
    val diff: Long = 0,
    val compareString: String = "stejný",
)