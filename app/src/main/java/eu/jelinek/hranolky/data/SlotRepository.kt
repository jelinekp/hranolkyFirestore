package eu.jelinek.hranolky.data

import eu.jelinek.hranolky.model.SlotAction
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.ui.showlast.ActionType
import kotlinx.coroutines.flow.Flow

interface SlotRepository {
    fun getSlot(slotId: String): Flow<WarehouseSlot?>
    fun getSlotActions(slotId: String): Flow<List<SlotAction>>
    suspend fun addSlotAction(slotId: String, actionType: ActionType, quantity: Long, currentQuantity: Int, deviceId: String)
    suspend fun createNewSlot(slotId: String, quantity: Int)
    fun getLastModifiedSlots(): Flow<LastModifiedSlots>
    fun getAllSlots(): Flow<List<WarehouseSlot>>
}

data class LastModifiedSlots(
    val beamSlots: List<WarehouseSlot>,
    val jointerSlots: List<WarehouseSlot>
) {
    companion object {
        val EMPTY = LastModifiedSlots(emptyList(), emptyList())
    }
}