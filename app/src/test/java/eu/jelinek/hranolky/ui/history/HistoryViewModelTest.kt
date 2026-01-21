package eu.jelinek.hranolky.ui.history

import android.app.Application
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.model.ActionType
import eu.jelinek.hranolky.model.SlotAction
import eu.jelinek.hranolky.model.SlotType
import eu.jelinek.hranolky.model.WarehouseSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeRepo : SlotRepository {
        override fun getSlot(fullSlotId: String): Flow<WarehouseSlot?> = flowOf(null)
        override fun getSlotActions(fullSlotId: String): Flow<List<SlotAction>> = flowOf(emptyList())

        override suspend fun addSlotAction(
            fullSlotId: String,
            actionType: ActionType,
            quantity: Long,
            currentQuantity: Long,
            deviceId: String
        ): String = "test-action-id"

        override suspend fun undoSlotAction(fullSlotId: String, actionDocumentId: String, quantityChange: Long) { }

        override suspend fun createNewSlot(fullSlotId: String, quantity: Long) { }

        override fun getLastModifiedSlots(slotType: SlotType): Flow<List<WarehouseSlot>> =
            when (slotType) {
                SlotType.Beam -> flowOf(listOf(WarehouseSlot(fullProductId = "H-X-20-40-1000", quantity = 1)))
                SlotType.Jointer -> flowOf(listOf(WarehouseSlot(fullProductId = "S-Y-27-42-2000", quantity = 2)))
            }

        override fun getAllSlots(slotType: SlotType): Flow<List<WarehouseSlot>> = flowOf(emptyList())
    }

    @Test
    fun `collects last modified slots into state`() {
        val vm = HistoryViewModel(Application(), FakeRepo())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.historyScreenState.value
        assertEquals(1, state.lastModifiedBeamSlots.size)
        assertEquals(0, state.lastModifiedJointerSlots.size) // Jointer is only loaded when loadJointerLastModifiedSlots() is called

        // Test jointer loading
        vm.loadJointerLastModifiedSlots()
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfterLoad = vm.historyScreenState.value
        assertEquals(1, stateAfterLoad.lastModifiedJointerSlots.size)
    }
}
