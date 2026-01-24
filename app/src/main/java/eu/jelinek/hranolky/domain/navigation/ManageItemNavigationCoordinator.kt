package eu.jelinek.hranolky.domain.navigation

import eu.jelinek.hranolky.domain.InputValidator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Coordinates navigation within the ManageItem feature.
 * Follows Separation of Concerns (SoC) by isolating navigation logic
 * from the ViewModel.
 */
class ManageItemNavigationCoordinator(
    private val inputValidator: InputValidator
) {
    private val _navigateToAnotherItem = MutableSharedFlow<String>()
    val navigateToAnotherItem = _navigateToAnotherItem.asSharedFlow()

    /**
     * Emit navigation event to a different item.
     */
    suspend fun navigateToItem(itemCode: String) {
        _navigateToAnotherItem.emit(itemCode)
    }

    /**
     * Check if scanned code is a different item code and should trigger navigation.
     *
     * @param scannedCode The code that was scanned
     * @param currentSlotId The current slot ID (without prefix)
     * @return NavigationResult indicating whether to navigate or continue with input
     */
    fun checkForItemCodeNavigation(
        scannedCode: String,
        currentSlotId: String?
    ): NavigationResult {
        val trimmedCode = scannedCode.trim()

        // Check if the input looks like an item code (not a number)
        if (trimmedCode.isEmpty()) {
            return NavigationResult.ContinueWithInput
        }

        // If it's a valid number, continue with input
        if (isNumericInput(trimmedCode)) {
            return NavigationResult.ContinueWithInput
        }

        // Try to manipulate and validate as item code
        val validatedCode = inputValidator.manipulateAndValidateItemCode(trimmedCode)
        if (validatedCode != null) {
            // It's a valid item code, check if it's different from current
            val normalizedScanned = normalizeSlotId(validatedCode)
            val normalizedCurrent = currentSlotId?.let { normalizeSlotId(it) }

            if (normalizedScanned != normalizedCurrent) {
                return NavigationResult.NavigateToItem(validatedCode)
            }
        }

        // Not a valid item code, continue with input (will show validation error)
        return NavigationResult.ContinueWithInput
    }

    private fun isNumericInput(input: String): Boolean {
        // Check for negative numbers
        val normalizedInput = if (input.startsWith("-")) input.substring(1) else input
        return normalizedInput.isNotEmpty() && normalizedInput.all { it.isDigit() }
    }

    private fun normalizeSlotId(slotId: String): String {
        // Remove H- or S- prefix for comparison
        return when {
            slotId.startsWith("H-") -> slotId.removePrefix("H-")
            slotId.startsWith("S-") -> slotId.removePrefix("S-")
            else -> slotId
        }
    }
}

/**
 * Result of navigation check.
 */
sealed class NavigationResult {
    /** Continue processing as quantity input */
    data object ContinueWithInput : NavigationResult()

    /** Navigate to a different item */
    data class NavigateToItem(val itemCode: String) : NavigationResult()
}
