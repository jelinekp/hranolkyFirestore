package eu.jelinek.hranolky.domain

import android.util.Log
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.model.ActionType

/**
 * Data class representing an action that can be undone.
 *
 * This follows the Separation of Concerns (SoC) principle by:
 * 1. Encapsulating undo action data in a dedicated class
 * 2. Providing clear documentation of what's needed to undo an action
 */
data class UndoableAction(
    val fullSlotId: String,
    val actionDocumentId: String,
    val quantityChange: Long,
    val actionType: ActionType
)

/**
 * Coordinator for handling undo operations.
 *
 * This follows the Separation of Concerns (SoC) principle by:
 * 1. Isolating undo logic from the ViewModel
 * 2. Providing a clean interface for undo operations
 * 3. Handling error cases consistently
 */
interface UndoActionCoordinator {
    /**
     * Undo a previously performed action.
     *
     * @param undoableAction The action to undo
     * @return Result indicating success or failure with error message
     */
    suspend fun undoAction(undoableAction: UndoableAction): Result<Unit>
}

/**
 * Default implementation using SlotRepository
 */
class UndoActionCoordinatorImpl(
    private val slotRepository: SlotRepository
) : UndoActionCoordinator {

    companion object {
        private const val TAG = "UndoActionCoordinator"
    }

    override suspend fun undoAction(undoableAction: UndoableAction): Result<Unit> {
        return try {
            Log.d(TAG, "undoAction: Undoing action ${undoableAction.actionDocumentId}")
            slotRepository.undoSlotAction(
                fullSlotId = undoableAction.fullSlotId,
                actionDocumentId = undoableAction.actionDocumentId,
                quantityChange = undoableAction.quantityChange
            )
            Log.d(TAG, "undoAction: Successfully undid action ${undoableAction.actionDocumentId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "undoAction: Failed to undo action", e)
            Result.failure(e)
        }
    }
}
