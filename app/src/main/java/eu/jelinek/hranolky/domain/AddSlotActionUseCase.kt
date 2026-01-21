package eu.jelinek.hranolky.domain

import android.util.Log
import eu.jelinek.hranolky.data.SheetDbRepository
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.data.helpers.toCustomCommaDecimalString
import eu.jelinek.hranolky.data.network.JointerReportingRow
import eu.jelinek.hranolky.model.ActionType
import eu.jelinek.hranolky.model.WarehouseSlot

/**
 * Result of adding a slot action, containing the document ID for potential undo
 */
data class AddSlotActionResult(
    val actionDocumentId: String,
    val quantityChange: Long
)

class AddSlotActionUseCase(
    private val slotRepository: SlotRepository,
    private val sheetDbRepository: SheetDbRepository,
    private val inputValidator: InputValidator
) : SlotActionOperation {

    /**
     * Operator invoke for backward compatibility
     */
    suspend operator fun invoke(
        fullSlotId: String,
        slot: WarehouseSlot,
        actionType: ActionType,
        quantity: String,
        currentQuantity: Long,
        deviceId: String
    ): Result<AddSlotActionResult> = execute(fullSlotId, slot, actionType, quantity, currentQuantity, deviceId)

    override suspend fun execute(
        fullSlotId: String,
        slot: WarehouseSlot,
        actionType: ActionType,
        quantity: String,
        currentQuantity: Long,
        deviceId: String
    ): Result<AddSlotActionResult> {
        val validationResult = inputValidator.validateQuantity(quantity, currentQuantity, actionType)

        return if (validationResult.isSuccess) {
            try {
                val validatedQuantity = validationResult.getOrThrow()

                // Calculate the actual quantity change for undo
                val quantityChange = when (actionType) {
                    ActionType.ADD -> validatedQuantity
                    ActionType.REMOVE -> -validatedQuantity
                    ActionType.INVENTORY_CHECK -> validatedQuantity - currentQuantity
                }

                val actionDocumentId = slotRepository.addSlotAction(
                    fullSlotId = fullSlotId,
                    actionType = actionType,
                    quantity = validatedQuantity,
                    currentQuantity = currentQuantity,
                    deviceId = deviceId
                )

                if (fullSlotId.first() == 'S') {
                    val sampleRowData = JointerReportingRow(
                        quality = slot.getFullQualityName(),
                        thickness = slot.thickness.toCustomCommaDecimalString(),
                        width = slot.width?.toInt().toString(),
                        length = slot.length.toString(),
                        quantityChange = quantity,
                        madeBy = "Petr",
                    )

                    val sheetDbResult = sheetDbRepository.addLogRow(sampleRowData)

                    sheetDbResult.onSuccess {
                        // Log successful, proceed with overall success
                        Result.success(Unit)
                    }.onFailure { sheetDbError ->
                        // SheetDB logging failed.
                        // Decide on behavior:
                        // 1. Return overall success anyway, but log this specific error?
                        // 2. Return failure for the whole use case?
                        // For now, let's treat SheetDB failure as a non-critical error for the use case's success,
                        // but log it. The primary slot action succeeded.
                        Log.e(
                            "AddSlotActionUseCase",
                            "Slot action succeeded but SheetDB logging failed for slot $fullSlotId: ${sheetDbError.message}",
                            sheetDbError
                        )
                        Result.success(Unit) // Still consider the main operation successful
                        // OR if SheetDB logging is critical:
                        // return Result.failure(Exception("Failed to log to SheetDB after slot action: ${sheetDbError.message}", sheetDbError))
                    }
                }

                Result.success(AddSlotActionResult(actionDocumentId, quantityChange))
            } catch (e: Exception) {
                Log.e(
                    "AddSlotActionUseCase",
                    "Error during slot action or preparing SheetDB log for slot $fullSlotId: ${e.message}",
                    e
                )
                Result.failure(e)
            }
        } else {
            Result.failure(validationResult.exceptionOrNull()!!)
        }
    }
}