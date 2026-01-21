package eu.jelinek.hranolky.domain

import android.util.Log
import eu.jelinek.hranolky.data.SlotRepository

/**
 * Use case for undoing a previously performed slot action.
 *
 * This follows the Action Version Transparency (AVT) principle by:
 * 1. Encapsulating the undo algorithm in a dedicated class
 * 2. Implementing the UndoSlotActionOperation interface
 * 3. Allowing the implementation to evolve independently
 */
class UndoSlotActionUseCase(
    private val slotRepository: SlotRepository
) : UndoSlotActionOperation {

    companion object {
        private const val TAG = "UndoSlotActionUseCase"
    }

    override suspend fun execute(
        fullSlotId: String,
        actionDocumentId: String,
        quantityChange: Long
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Undoing action $actionDocumentId for slot $fullSlotId, reversing $quantityChange")

            slotRepository.undoSlotAction(
                fullSlotId = fullSlotId,
                actionDocumentId = actionDocumentId,
                quantityChange = quantityChange
            )

            Log.d(TAG, "Successfully undid action $actionDocumentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to undo action $actionDocumentId", e)
            Result.failure(e)
        }
    }
}
