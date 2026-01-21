package eu.jelinek.hranolky.domain

import eu.jelinek.hranolky.model.ActionType
import eu.jelinek.hranolky.model.WarehouseSlot

/**
 * Interface for slot action operations following Action Version Transparency (AVT) principle.
 *
 * This allows different implementations of slot actions to be used interchangeably,
 * enabling the system to evolve without breaking existing functionality.
 */
interface SlotActionOperation {
    /**
     * Execute the slot action
     *
     * @param fullSlotId The complete slot identifier
     * @param slot The warehouse slot being modified
     * @param actionType The type of action to perform
     * @param quantity The quantity value as a string
     * @param currentQuantity The current quantity before the action
     * @param deviceId The device performing the action
     * @return Result containing action details or error
     */
    suspend fun execute(
        fullSlotId: String,
        slot: WarehouseSlot,
        actionType: ActionType,
        quantity: String,
        currentQuantity: Long,
        deviceId: String
    ): Result<AddSlotActionResult>
}

/**
 * Interface for undoing slot actions.
 */
interface UndoSlotActionOperation {
    /**
     * Undo a previously executed slot action
     *
     * @param fullSlotId The complete slot identifier
     * @param actionDocumentId The ID of the action document to undo
     * @param quantityChange The quantity change to reverse
     * @return Result indicating success or failure
     */
    suspend fun execute(
        fullSlotId: String,
        actionDocumentId: String,
        quantityChange: Long
    ): Result<Unit>
}
