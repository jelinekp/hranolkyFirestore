package eu.jelinek.hranolky.domain

import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.ui.manageitem.ActionType

class AddSlotActionUseCase(
    private val slotRepository: SlotRepository,
    private val inputValidator: InputValidator
) {
    suspend operator fun invoke(
        slotId: String,
        actionType: ActionType,
        quantity: String,
        currentQuantity: Long,
        deviceId: String
    ): Result<Unit> {
        val validationResult = inputValidator.validateQuantity(quantity, currentQuantity, actionType)

        return if (validationResult.isSuccess) {
            try {
                slotRepository.addSlotAction(slotId, actionType, validationResult.getOrThrow(), currentQuantity, deviceId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(validationResult.exceptionOrNull()!!)
        }
    }
}