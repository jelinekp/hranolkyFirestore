package eu.jelinek.hranolky.data

import eu.jelinek.hranolky.model.ActionType
import eu.jelinek.hranolky.model.SlotAction
import eu.jelinek.hranolky.model.SlotType
import eu.jelinek.hranolky.model.WarehouseSlot
import kotlinx.coroutines.flow.Flow

interface SlotRepository {
    fun getSlot(fullSlotId: String): Flow<WarehouseSlot?>
    fun getSlotActions(fullSlotId: String): Flow<List<SlotAction>>
    suspend fun addSlotAction(fullSlotId: String, actionType: ActionType, quantity: Long, currentQuantity: Long, deviceId: String): String // Returns action document ID
    suspend fun undoSlotAction(fullSlotId: String, actionDocumentId: String, quantityChange: Long)
    suspend fun createNewSlot(fullSlotId: String, quantity: Long)
    fun getLastModifiedSlots(slotType: SlotType): Flow<List<WarehouseSlot>>
    fun getAllSlots(slotType: SlotType): Flow<List<WarehouseSlot>>
}