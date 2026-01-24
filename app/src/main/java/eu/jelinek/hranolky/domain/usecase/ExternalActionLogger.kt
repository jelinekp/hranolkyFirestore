package eu.jelinek.hranolky.domain.usecase

import eu.jelinek.hranolky.model.SlotAction

/**
 * Strategy interface for logging slot actions to external services.
 *
 * This follows the Action Version Transparency (AVT) principle by
 * abstracting the logging algorithm behind an interface, allowing
 * different implementations to be swapped without affecting the caller.
 *
 * Implementations could include:
 * - SheetDB logging (current production)
 * - Google Sheets API
 * - Custom REST API
 * - No-op for testing
 */
interface ExternalActionLogger {
    /**
     * Logs a slot action to an external service.
     *
     * @param action The slot action to log
     * @param slotId The ID of the slot being modified
     * @param deviceId The device identifier performing the action
     * @return Result indicating success or failure with details
     */
    suspend fun logAction(
        action: SlotAction,
        slotId: String,
        deviceId: String
    ): Result<LoggingResult>
}

/**
 * Result of an external logging operation.
 */
sealed class LoggingResult {
    /** Logging succeeded */
    data class Success(val externalId: String? = null) : LoggingResult()

    /** Logging was skipped (e.g., action type not configured for logging) */
    data object Skipped : LoggingResult()

    /** Logging failed but should not block the main operation */
    data class NonBlockingFailure(val error: String) : LoggingResult()
}

/**
 * No-op implementation for testing or when external logging is disabled.
 */
class NoOpExternalActionLogger : ExternalActionLogger {
    override suspend fun logAction(
        action: SlotAction,
        slotId: String,
        deviceId: String
    ): Result<LoggingResult> = Result.success(LoggingResult.Skipped)
}
