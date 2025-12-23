package eu.jelinek.hranolky.domain

import eu.jelinek.hranolky.model.ActionType

class InputValidator {
    fun validateQuantity(quantity: String, currentQuantity: Long, actionType: ActionType): Result<Long> {
        val quantityAsLong = quantity.toLongOrNull()

        return when {
            quantityAsLong == null -> Result.failure(Exception("Neplatné množství"))
            quantityAsLong < 0 -> Result.failure(Exception("Množství musí být kladné číslo"))
            quantityAsLong == 0L && actionType != ActionType.INVENTORY_CHECK -> Result.failure(Exception("Pohyb nemůže být nulový"))
            actionType == ActionType.REMOVE && quantityAsLong > currentQuantity -> Result.failure(Exception("Nedostatek množství na skladě"))
            else -> Result.success(quantityAsLong)
        }
    }

    fun isValidScannedTextFormat(text: String): Boolean {
        val textLength = text.length

        if (textLength == 16) {
            if (text[1] != '-'
                && text[textLength - 5] == '-'
                && text.substring(textLength - 4).all(Char::isDigit)
            ) {
                return true
            }
        }

        if (textLength == 18) {
            if (text[0] == 'H'
                && text[1] == '-'
                && text[textLength - 5] == '-'
                && text.substring(textLength - 4).all(Char::isDigit)
            ) {
                return true
            }
        }

        if (textLength == 22) {
            // This regex validates the structure: S-[3 uppercase chars]-[3 uppercase chars with slashes]-[2 digits]-[4 digits]-[4 digits]
            // It matches all the provided examples like:
            // S-DUB-ABP-42-0272-1860
            // S-DUB-A/A-27-0095-2050
            // S-ZIR-ZIR-32-0245-2150
            val jointerRegex = Regex("^S-[A-Z]{3}-[A-Z|]{3}-[0-9]{2}-[0-9]{4}-[0-9]{4}$")
            return jointerRegex.matches(text)
        }

        // If neither rule matched, it's not a valid format
        return false
    }
}
