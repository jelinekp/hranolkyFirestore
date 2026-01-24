package eu.jelinek.hranolky.ui.manageitem

import eu.jelinek.hranolky.model.WarehouseSlot
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for ManageItemScreen UI state classes following SoS principle.
 */
class ManageItemUiStatesTest {

    private fun createTestSlot() = WarehouseSlot(
        fullProductId = "H-DUB-A-20-100-2000",
        quantity = 50
    )

    @Test
    fun `SlotDataState default values are correct`() {
        val state = SlotDataState()
        assertNull(state.slot)
        assertEquals("", state.screenTitle)
    }

    @Test
    fun `DataLoadingState sealed class variants work correctly`() {
        assertEquals(ResultStatus.LOADING, DataLoadingState.Loading.toResultStatus())
        assertEquals(ResultStatus.SUCCESS, DataLoadingState.Success.toResultStatus())
        assertEquals(ResultStatus.NETWORK_ERROR, DataLoadingState.NetworkError("err").toResultStatus())
        assertEquals(ResultStatus.DATA_ERROR, DataLoadingState.DataError("err").toResultStatus())
        assertEquals(ResultStatus.OTHER_ERROR, DataLoadingState.OtherError("err").toResultStatus())
    }

    @Test
    fun `DataLoadingState fromResultStatus converts correctly`() {
        assertEquals(DataLoadingState.Loading, DataLoadingState.fromResultStatus(ResultStatus.LOADING))
        assertEquals(DataLoadingState.Success, DataLoadingState.fromResultStatus(ResultStatus.SUCCESS))
        assertTrue(DataLoadingState.fromResultStatus(ResultStatus.NETWORK_ERROR) is DataLoadingState.NetworkError)
        assertTrue(DataLoadingState.fromResultStatus(ResultStatus.DATA_ERROR) is DataLoadingState.DataError)
        assertTrue(DataLoadingState.fromResultStatus(ResultStatus.OTHER_ERROR) is DataLoadingState.OtherError)
    }

    @Test
    fun `DataLoadingState error messages are preserved`() {
        val networkError = DataLoadingState.NetworkError("No internet")
        assertEquals("No internet", networkError.message)

        val dataError = DataLoadingState.DataError("Invalid data")
        assertEquals("Invalid data", dataError.message)

        val otherError = DataLoadingState.OtherError("Something went wrong")
        assertEquals("Something went wrong", otherError.message)
    }

    @Test
    fun `ManageItemInventoryState default values are correct`() {
        val state = ManageItemInventoryState()
        assertFalse(state.isEnabled)
        assertFalse(state.isDone)
        assertFalse(state.showConfirmPopup)
        assertEquals(InventoryCheckPopupMessage(), state.popupMessage)
    }

    @Test
    fun `ConnectivityState default is online`() {
        val state = ConnectivityState()
        assertTrue(state.isOnline)
    }

    @Test
    fun `ActionInputValidationState default has no errors`() {
        val state = ActionInputValidationState()
        assertFalse(state.isQuantityError)
        assertFalse(state.isRemovedError)
        assertNull(state.errorMessage)
    }

    @Test
    fun `ActionInputValidationState toAddActionValidationState converts correctly`() {
        val input = ActionInputValidationState(
            isQuantityError = true,
            isRemovedError = false,
            errorMessage = "Invalid quantity"
        )

        val result = input.toAddActionValidationState()

        assertTrue(result.isQuantityError)
        assertFalse(result.isRemovedError)
        assertEquals("Invalid quantity", result.errorMessage)
    }

    @Test
    fun `ActionInputValidationState fromAddActionValidationState converts correctly`() {
        val add = AddActionValidationState(
            isQuantityError = false,
            isRemovedError = true,
            errorMessage = "Already removed"
        )

        val result = ActionInputValidationState.fromAddActionValidationState(add)

        assertFalse(result.isQuantityError)
        assertTrue(result.isRemovedError)
        assertEquals("Already removed", result.errorMessage)
    }

    @Test
    fun `CompositeManageItemState default values are correct`() {
        val state = CompositeManageItemState()
        assertEquals(SlotDataState(), state.slotData)
        assertEquals(DataLoadingState.Loading, state.loading)
        assertEquals(ManageItemInventoryState(), state.inventory)
        assertEquals(ConnectivityState(), state.connectivity)
    }

    @Test
    fun `CompositeManageItemState toLegacyState converts correctly`() {
        val slot = createTestSlot()
        val composite = CompositeManageItemState(
            slotData = SlotDataState(slot = slot, screenTitle = "DUB A 20×100×2000"),
            loading = DataLoadingState.Success,
            inventory = ManageItemInventoryState(isEnabled = true, isDone = true),
            connectivity = ConnectivityState(isOnline = true)
        )

        val legacy = composite.toLegacyState()

        assertEquals(slot, legacy.slot)
        assertEquals("DUB A 20×100×2000", legacy.screenTitle)
        assertEquals(ResultStatus.SUCCESS, legacy.resultStatus)
        assertNull(legacy.error)
        assertTrue(legacy.isOnline)
        assertTrue(legacy.isInventoryCheckEnabled)
        assertTrue(legacy.isInventoryCheckDone)
    }

    @Test
    fun `CompositeManageItemState fromLegacyState converts correctly`() {
        val slot = createTestSlot()
        val legacy = ManageItemScreenState(
            slot = slot,
            screenTitle = "Test Title",
            resultStatus = ResultStatus.NETWORK_ERROR,
            error = "Connection failed",
            isOnline = false,
            isInventoryCheckEnabled = false,
            showConfirmSettingPopup = true,
            isInventoryCheckDone = false
        )

        val composite = CompositeManageItemState.fromLegacyState(legacy)

        assertEquals(slot, composite.slotData.slot)
        assertEquals("Test Title", composite.slotData.screenTitle)
        assertTrue(composite.loading is DataLoadingState.NetworkError)
        assertEquals("Connection failed", (composite.loading as DataLoadingState.NetworkError).message)
        assertFalse(composite.connectivity.isOnline)
        assertFalse(composite.inventory.isEnabled)
        assertTrue(composite.inventory.showConfirmPopup)
        assertFalse(composite.inventory.isDone)
    }

    @Test
    fun `roundtrip conversion preserves state`() {
        val slot = createTestSlot()
        val original = ManageItemScreenState(
            slot = slot,
            screenTitle = "Original Title",
            resultStatus = ResultStatus.SUCCESS,
            error = null,
            isOnline = true,
            isInventoryCheckEnabled = true,
            showConfirmSettingPopup = false,
            inventoryCheckPopupMessage = InventoryCheckPopupMessage(diff = 5, compareString = "více"),
            isInventoryCheckDone = true
        )

        val composite = CompositeManageItemState.fromLegacyState(original)
        val roundTripped = composite.toLegacyState()

        assertEquals(original.slot, roundTripped.slot)
        assertEquals(original.screenTitle, roundTripped.screenTitle)
        assertEquals(original.resultStatus, roundTripped.resultStatus)
        assertEquals(original.error, roundTripped.error)
        assertEquals(original.isOnline, roundTripped.isOnline)
        assertEquals(original.isInventoryCheckEnabled, roundTripped.isInventoryCheckEnabled)
        assertEquals(original.showConfirmSettingPopup, roundTripped.showConfirmSettingPopup)
        assertEquals(original.isInventoryCheckDone, roundTripped.isInventoryCheckDone)
    }

    @Test
    fun `individual states can be updated independently`() {
        val state = CompositeManageItemState()

        // Update only loading state
        val withSuccess = state.copy(loading = DataLoadingState.Success)
        assertEquals(DataLoadingState.Success, withSuccess.loading)
        assertEquals(state.slotData, withSuccess.slotData)
        assertEquals(state.inventory, withSuccess.inventory)
        assertEquals(state.connectivity, withSuccess.connectivity)

        // Update only connectivity
        val withOffline = state.copy(
            connectivity = state.connectivity.copy(isOnline = false)
        )
        assertFalse(withOffline.connectivity.isOnline)
        assertEquals(state.slotData, withOffline.slotData)
        assertEquals(state.loading, withOffline.loading)
        assertEquals(state.inventory, withOffline.inventory)
    }

    @Test
    fun `error states preserve error messages in legacy conversion`() {
        val composite = CompositeManageItemState(
            loading = DataLoadingState.NetworkError("No network available")
        )

        val legacy = composite.toLegacyState()

        assertEquals(ResultStatus.NETWORK_ERROR, legacy.resultStatus)
        assertEquals("No network available", legacy.error)
    }
}
