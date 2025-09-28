package eu.jelinek.hranolky.domain

import eu.jelinek.hranolky.model.ActionType

class InputValidator {
    fun validateQuantity(quantity: String, currentQuantity: Long, actionType: ActionType): Result<Long> {
        val quantityLong = quantity.toLongOrNull()
        if (quantityLong == null || quantityLong <= 0 || quantityLong > 99_999) {
            return Result.failure(IllegalArgumentException("Invalid quantity"))
        }
        if (actionType == ActionType.REMOVE && quantityLong > currentQuantity) {
            return Result.failure(IllegalStateException("Cannot remove more than available"))
        }
        return Result.success(quantityLong)
    }
}