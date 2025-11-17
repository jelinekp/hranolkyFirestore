package eu.jelinek.hranolky.domain

import android.util.Log
import eu.jelinek.hranolky.data.SheetDbRepository
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.data.helpers.toCustomCommaDecimalString
import eu.jelinek.hranolky.data.network.JointerReportingRow
import eu.jelinek.hranolky.model.ActionType
import eu.jelinek.hranolky.model.WarehouseSlot

class AddSlotActionUseCase(
    private val slotRepository: SlotRepository,
    private val sheetDbRepository: SheetDbRepository,
    private val inputValidator: InputValidator
) {
    suspend operator fun invoke(
        fullSlotId: String,
        slot: WarehouseSlot,
        actionType: ActionType,
        quantity: String,
        currentQuantity: Long,
        deviceId: String
    ): Result<Unit> {
        val validationResult = inputValidator.validateQuantity(quantity, currentQuantity, actionType)

        return if (validationResult.isSuccess) {
            try {
                slotRepository.addSlotAction(
                    fullSlotId = fullSlotId,
                    actionType = actionType,
                    quantity = validationResult.getOrThrow(),
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

                Result.success(Unit)
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