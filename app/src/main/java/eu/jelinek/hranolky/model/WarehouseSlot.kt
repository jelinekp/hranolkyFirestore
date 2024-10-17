package eu.jelinek.hranolky.model

data class WarehouseSlot(
    val productId: String,
    val quantity: Int,
    val slotActions: List<SlotAction> = emptyList(),
    val quality: String? = null,
    val width: Int? = null,
    val thickness: Float? = null,
    val length: Int? = null,
) {
    fun parsePropertiesFromProductId(): WarehouseSlot {
        val quality = this.productId.take(5)
        val parts = this.productId.split("-")
        val rawThickness = parts[2].toFloat()

        val thickness = when (rawThickness) {
            20.0f -> 20.0f
            27.0f -> 27.4f
            42.0f -> 42.4f
            else -> rawThickness
        }

        val width = parts[3].toInt()
        val length = parts[4].toInt()

        return this.copy(
            quality = quality,
            thickness = thickness,
            width = width,
            length = length,
            )
    }
}

data class Quantity(
    val quantity: Int = 0,
)
