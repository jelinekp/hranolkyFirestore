package eu.jelinek.hranolky.domain

import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.ui.showlast.ActionType

class AddSlotActionUseCase(private val slotRepository: SlotRepository) {
    suspend operator fun invoke(
        slotId: String,
        actionType: ActionType,
        quantity: String,
        currentQuantity: Int,
        deviceId: String
    ): Result<Unit> {
        val quantityLong = quantity.toLongOrNull()
        if (quantityLong == null || quantityLong <= 0 || quantityLong > 9999) {
            return Result.failure(IllegalArgumentException("Invalid quantity"))
        }
        if (actionType == ActionType.REMOVE && quantityLong > currentQuantity) {
            return Result.failure(IllegalStateException("Cannot remove more than available"))
        }

        return try {
            slotRepository.addSlotAction(slotId, actionType, quantityLong, currentQuantity, deviceId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}