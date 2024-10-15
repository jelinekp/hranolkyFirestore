package eu.jelinek.hranolky.model

data class WarehouseSlot(
    val productId: String,
    val quantity: Int,
    val quality: String? = null,
    val width: Int? = null,
    val thickness: Int? = null,
    val length: Int? = null,
)
