package eu.jelinek.hranolky.domain

/**
 * Parses and validates quantity input strings.
 *
 * This follows the Action Version Transparency (AVT) principle by:
 * 1. Isolating the quantity parsing algorithm into a dedicated class
 * 2. Making the parsing rules explicit and testable
 * 3. Allowing the algorithm to evolve independently of the ViewModel
 *
 * Supported formats:
 * - Simple numbers: "123"
 * - Sums: "50+30+20" → 100
 */
class QuantityParser {

    /**
     * Result of parsing a quantity string
     */
    sealed class ParseResult {
        data class Success(val quantity: Long) : ParseResult()
        data class Error(val message: String) : ParseResult()
        data class ItemCodeDetected(val itemCode: String) : ParseResult()
    }

    /**
     * Error messages for quantity validation
     */
    object ErrorMessages {
        const val EMPTY = "Množství nemůže být prázdné."
        const val NEGATIVE = "Množství nemůže být záporné."
        const val EMPTY_PART = "Neplatný formát: prázdná část v součtu."
        const val INVALID_PART = "Neplatný formát čísla v části"
        const val NEGATIVE_PART = "Část součtu nemůže být záporná"
        const val INVALID_FORMAT = "Neplatný formát množství."
    }

    /**
     * Parses a quantity string to a Long value.
     *
     * @param quantityStr The input string to parse
     * @param itemCodeValidator Optional function to validate if the string is an item code
     * @return Result containing either the parsed value or an error
     */
    fun parse(
        quantityStr: String,
        itemCodeValidator: ((String) -> String?)? = null
    ): ParseResult {
        if (quantityStr.isBlank()) {
            return ParseResult.Error(ErrorMessages.EMPTY)
        }

        // Handle sums like "X+Y" or "X+Y+Z" or leading "+" first
        // This must be before toLongOrNull because "+50".toLongOrNull() returns 50L
        if (quantityStr.contains('+')) {
            return parseSum(quantityStr)
        }

        // Handle simple numbers (no + sign at this point)
        quantityStr.toLongOrNull()?.let { value ->
            if (value < 0) {
                return ParseResult.Error(ErrorMessages.NEGATIVE)
            }
            return ParseResult.Success(value)
        }


        // If it's not a number, check if it's a different item code
        if (itemCodeValidator != null) {
            val itemCode = quantityStr.trim().uppercase()
            val validatedItemCode = itemCodeValidator(itemCode)
            if (validatedItemCode != null) {
                return ParseResult.ItemCodeDetected(validatedItemCode)
            }
        }

        // If it's not a simple number, sum, or item code, it's an invalid format
        return ParseResult.Error(ErrorMessages.INVALID_FORMAT)
    }

    /**
     * Parses a sum expression like "50+30+20"
     */
    private fun parseSum(quantityStr: String): ParseResult {
        val parts = quantityStr.split('+')

        // Check for empty parts like "5++5" or "5+"
        if (parts.any { it.trim().isEmpty() }) {
            return ParseResult.Error(ErrorMessages.EMPTY_PART)
        }

        var sum = 0L
        for (part in parts) {
            val trimmedPart = part.trim()
            if (trimmedPart.isEmpty()) {
                return ParseResult.Error(ErrorMessages.EMPTY_PART)
            }

            val number = trimmedPart.toLongOrNull()
            if (number == null) {
                return ParseResult.Error("${ErrorMessages.INVALID_PART}: '$trimmedPart'")
            }

            if (number < 0) {
                return ParseResult.Error("${ErrorMessages.NEGATIVE_PART}: '$trimmedPart'")
            }

            sum += number
        }

        return ParseResult.Success(sum)
    }

    /**
     * Legacy method for backward compatibility
     */
    fun parseToResult(quantityStr: String): Result<Long> {
        return when (val result = parse(quantityStr)) {
            is ParseResult.Success -> Result.success(result.quantity)
            is ParseResult.Error -> Result.failure(NumberFormatException(result.message))
            is ParseResult.ItemCodeDetected -> Result.failure(NumberFormatException("Item code detected"))
        }
    }
}
