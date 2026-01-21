package eu.jelinek.hranolky.ui.manageitem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.jelinek.hranolky.domain.QuantityParser

/**
 * Dedicated state holder for quantity input with validation.
 * Follows Separation of States (SoS) by isolating input state from other concerns.
 */
class QuantityInputState(
    private val quantityParser: QuantityParser
) {
    /**
     * Current quantity input as string.
     */
    var quantity by mutableStateOf("")
        private set

    /**
     * Parsed quantity value, or null if parsing fails.
     */
    val parsedQuantity: Long?
        get() {
            val result = quantityParser.parse(quantity)
            return when (result) {
                is QuantityParser.ParseResult.Success -> result.quantity
                else -> null
            }
        }

    /**
     * Whether the current input is valid.
     */
    val isValid: Boolean
        get() = quantity.isEmpty() || parsedQuantity != null

    /**
     * Whether the input is empty.
     */
    val isEmpty: Boolean
        get() = quantity.isEmpty()

    /**
     * Whether there's a non-empty input that failed to parse.
     */
    val hasParseError: Boolean
        get() = quantity.isNotEmpty() && parsedQuantity == null

    /**
     * Update the quantity input.
     */
    fun onQuantityChanged(newQuantity: String) {
        quantity = newQuantity
    }

    /**
     * Clear the quantity input.
     */
    fun clear() {
        quantity = ""
    }

    /**
     * Get the parsed quantity or throw if invalid.
     */
    fun requireParsedQuantity(): Long {
        return parsedQuantity
            ?: throw IllegalStateException("Quantity is not valid: $quantity")
    }
}
