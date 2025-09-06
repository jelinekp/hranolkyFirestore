package eu.jelinek.hranolky.ui.overview

import eu.jelinek.hranolky.model.SlotType
import kotlin.math.floor

fun nextType(type: SlotType) = when (type) {
    SlotType.Beam -> SlotType.Jointer
    SlotType.Jointer -> SlotType.Beam
}

// Helper function to format the number according to your rules
fun formatSlotDimension(value: Float): String {
    val integerPart = floor(value).toLong()
    val isWholeNumber = value == integerPart.toFloat()

    return if (isWholeNumber) {
        val integerDigits = integerPart.toString().length
        if (integerDigits > 3) {
            integerPart.toString() // More than 3 integer digits, show as whole number string
        } else {
            value.toString()
        }
    } else {
        value.toString()
    }
}