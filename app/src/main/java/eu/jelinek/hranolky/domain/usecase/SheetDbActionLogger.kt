package eu.jelinek.hranolky.domain.usecase

import eu.jelinek.hranolky.data.SheetDbRepository
import eu.jelinek.hranolky.data.network.JointerReportingRow
import eu.jelinek.hranolky.model.SlotAction
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * SheetDB implementation of ExternalActionLogger.
 *
 * Logs jointer-related actions (ADD, INVENTORY_CHECK) to SheetDB
 * for external reporting purposes.
 *
 * This follows the Strategy pattern per AVT principles - the logging
 * algorithm can be replaced without modifying AddSlotActionUseCase.
 */
class SheetDbActionLogger(
    private val sheetDbRepository: SheetDbRepository
) : ExternalActionLogger {

    private val dateFormatter = DateTimeFormatter
        .ofPattern("dd.MM.yyyy")
        .withZone(ZoneId.systemDefault())

    override suspend fun logAction(
        action: SlotAction,
        slotId: String,
        deviceId: String
    ): Result<LoggingResult> {
        // Only log ADD actions (jointer production reporting)
        if (action.action != "ADD") {
            return Result.success(LoggingResult.Skipped)
        }

        return try {
            // Parse slot info from slotId to get dimensions
            val slot = parseSlotInfo(slotId)
            val row = createReportingRow(action, slot, deviceId)
            val result = sheetDbRepository.addLogRow(row)

            result.fold(
                onSuccess = { response ->
                    Result.success(LoggingResult.Success(response.created?.toString()))
                },
                onFailure = { error ->
                    // SheetDB failures should not block the main operation
                    Result.success(LoggingResult.NonBlockingFailure(error.message ?: "Unknown error"))
                }
            )
        } catch (e: Exception) {
            // Catch any unexpected errors - don't block main operation
            Result.success(LoggingResult.NonBlockingFailure(e.message ?: "Unknown error"))
        }
    }

    /**
     * Parses slot info from slotId to extract dimensions.
     * SlotId format: [S-|H-]QUALITY-THICKNESS-WIDTH-LENGTH
     * Example: S-DUB-A-27-42-3000 -> Jointer (S-), DUB quality A, 27mm thick, 42mm wide, 3000mm long
     */
    private fun parseSlotInfo(slotId: String): SlotInfo {
        // Remove S- or H- prefix if present
        val normalizedId = when {
            slotId.startsWith("S-") -> slotId.removePrefix("S-")
            slotId.startsWith("H-") -> slotId.removePrefix("H-")
            else -> slotId
        }

        val parts = normalizedId.split("-")
        return if (parts.size >= 5) {
            SlotInfo(
                quality = "${parts[0]}-${parts[1]}", // e.g., DUB-A
                thickness = parts[2],
                width = parts[3],
                length = parts[4]
            )
        } else {
            SlotInfo(
                quality = parts.getOrElse(0) { "?" },
                thickness = parts.getOrElse(1) { "?" },
                width = parts.getOrElse(2) { "?" },
                length = parts.getOrElse(3) { "?" }
            )
        }
    }

    private fun createReportingRow(
        action: SlotAction,
        slot: SlotInfo,
        deviceId: String
    ): JointerReportingRow {
        val formattedDate = action.timestamp?.let {
            dateFormatter.format(it.toDate().toInstant())
        } ?: dateFormatter.format(java.time.Instant.now())

        return JointerReportingRow(
            date = formattedDate,
            quality = slot.quality,
            thickness = slot.thickness,
            width = slot.width,
            length = slot.length,
            quantityChange = action.quantityChange.toString(),
            madeBy = deviceId
        )
    }

    private data class SlotInfo(
        val quality: String,
        val thickness: String,
        val width: String,
        val length: String
    )
}
