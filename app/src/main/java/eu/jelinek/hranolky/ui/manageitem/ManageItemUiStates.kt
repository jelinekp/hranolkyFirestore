package eu.jelinek.hranolky.ui.manageitem

import eu.jelinek.hranolky.model.WarehouseSlot

/**
 * Isolated UI state classes for ManageItemScreen, following Separation of States (SoS) principle.
 * Each state class focuses on a single concern.
 */

/**
 * State for the slot data being displayed/edited.
 */
data class SlotDataState(
    val slot: WarehouseSlot? = null,
    val screenTitle: String = ""
)

/**
 * State for the network/loading status.
 */
sealed class DataLoadingState {
    data object Loading : DataLoadingState()
    data object Success : DataLoadingState()
    data class NetworkError(val message: String? = null) : DataLoadingState()
    data class DataError(val message: String? = null) : DataLoadingState()
    data class OtherError(val message: String? = null) : DataLoadingState()

    fun toResultStatus(): ResultStatus = when (this) {
        is Loading -> ResultStatus.LOADING
        is Success -> ResultStatus.SUCCESS
        is NetworkError -> ResultStatus.NETWORK_ERROR
        is DataError -> ResultStatus.DATA_ERROR
        is OtherError -> ResultStatus.OTHER_ERROR
    }

    companion object {
        fun fromResultStatus(status: ResultStatus, error: String? = null): DataLoadingState = when (status) {
            ResultStatus.LOADING -> Loading
            ResultStatus.SUCCESS -> Success
            ResultStatus.NETWORK_ERROR -> NetworkError(error)
            ResultStatus.DATA_ERROR -> DataError(error)
            ResultStatus.OTHER_ERROR -> OtherError(error)
        }
    }
}

/**
 * State for inventory check feature in ManageItemScreen.
 */
data class ManageItemInventoryState(
    val isEnabled: Boolean = false,
    val isDone: Boolean = false,
    val showConfirmPopup: Boolean = false,
    val popupMessage: InventoryCheckPopupMessage = InventoryCheckPopupMessage()
)

/**
 * State for connectivity status.
 */
data class ConnectivityState(
    val isOnline: Boolean = true
)

/**
 * State for form validation in action input.
 */
data class ActionInputValidationState(
    val isQuantityError: Boolean = false,
    val isRemovedError: Boolean = false,
    val errorMessage: String? = null
) {
    fun toAddActionValidationState(): AddActionValidationState = AddActionValidationState(
        isQuantityError = isQuantityError,
        isRemovedError = isRemovedError,
        errorMessage = errorMessage
    )

    companion object {
        fun fromAddActionValidationState(state: AddActionValidationState): ActionInputValidationState =
            ActionInputValidationState(
                isQuantityError = state.isQuantityError,
                isRemovedError = state.isRemovedError,
                errorMessage = state.errorMessage
            )
    }
}

/**
 * Composite UI state that combines all ManageItemScreen-related states.
 * This provides better state isolation while maintaining compatibility with legacy code.
 */
data class CompositeManageItemState(
    val slotData: SlotDataState = SlotDataState(),
    val loading: DataLoadingState = DataLoadingState.Loading,
    val inventory: ManageItemInventoryState = ManageItemInventoryState(),
    val connectivity: ConnectivityState = ConnectivityState()
) {
    /**
     * Convert to legacy ManageItemScreenState for backward compatibility.
     */
    fun toLegacyState(): ManageItemScreenState = ManageItemScreenState(
        slot = slotData.slot,
        screenTitle = slotData.screenTitle,
        resultStatus = loading.toResultStatus(),
        error = when (loading) {
            is DataLoadingState.NetworkError -> loading.message
            is DataLoadingState.DataError -> loading.message
            is DataLoadingState.OtherError -> loading.message
            else -> null
        },
        isOnline = connectivity.isOnline,
        isInventoryCheckEnabled = inventory.isEnabled,
        showConfirmSettingPopup = inventory.showConfirmPopup,
        inventoryCheckPopupMessage = inventory.popupMessage,
        isInventoryCheckDone = inventory.isDone
    )

    companion object {
        /**
         * Create from legacy ManageItemScreenState for migration purposes.
         */
        fun fromLegacyState(legacy: ManageItemScreenState): CompositeManageItemState = CompositeManageItemState(
            slotData = SlotDataState(
                slot = legacy.slot,
                screenTitle = legacy.screenTitle
            ),
            loading = DataLoadingState.fromResultStatus(legacy.resultStatus, legacy.error),
            inventory = ManageItemInventoryState(
                isEnabled = legacy.isInventoryCheckEnabled,
                isDone = legacy.isInventoryCheckDone,
                showConfirmPopup = legacy.showConfirmSettingPopup,
                popupMessage = legacy.inventoryCheckPopupMessage
            ),
            connectivity = ConnectivityState(
                isOnline = legacy.isOnline
            )
        )
    }
}
