package eu.jelinek.hranolky.ui.history

import android.app.Application
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.model.ActionType
import eu.jelinek.hranolky.model.SlotAction
import eu.jelinek.hranolky.model.SlotType
import eu.jelinek.hranolky.model.WarehouseSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryViewModelTest {

    private class FakeRepo(private val flow: Flow<LastModifiedSlots>) : SlotRepository {
        override fun getSlot(fullSlotId: String): Flow<WarehouseSlot?> = flowOf(null)
        override fun getSlotActions(fullSlotId: String): Flow<List<SlotAction>> = flowOf(emptyList())
        override suspend fun addSlotAction(fullSlotId: String, actionType: ActionType, quantity: Long, currentQuantity: Long, deviceId: String) { }
        override suspend fun createNewSlot(fullSlotId: String, quantity: Long) { }
        override fun getLastModifiedSlots(): Flow<LastModifiedSlots> = flow
        override fun getAllSlots(slotType: SlotType): Flow<List<WarehouseSlot>> = flowOf(emptyList())
    }

    @Test
    fun `collects last modified slots into state`() {
        val slots = LastModifiedSlots(
            beamSlots = listOf(WarehouseSlot(productId = "H-X-20-40-1000", quantity = 1)),
            jointerSlots = listOf(WarehouseSlot(productId = "S-Y-27-42-2000", quantity = 2))
        )
        val vm = HistoryViewModel(Application(), FakeRepo(flowOf(slots)))
        val state = vm.historyScreenState.value
        assertEquals(1, state.lastModifiedBeamSlots.size)
        assertEquals(1, state.lastModifiedJointerSlots.size)
    }
}
