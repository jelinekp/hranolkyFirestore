package eu.jelinek.hranolky.model

data class WarehouseSlot(
    val productId: String,
    val quantity: Int,
    val slotActions: List<SlotAction> = emptyList(),
    val quality: String? = null,
    val width: Int? = null,
    val thickness: Float? = null,
    val length: Int? = null,
)

data class Quantity(
    val quantity: Int = 0,
)
