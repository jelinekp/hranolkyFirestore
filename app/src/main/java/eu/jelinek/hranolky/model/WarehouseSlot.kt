package eu.jelinek.hranolky.model

import android.util.Log

data class WarehouseSlot(
    val productId: String,
    val quantity: Int,
    val slotActions: List<SlotAction> = emptyList(),
    val quality: String? = null,
    val width: Float? = null,
    val thickness: Float? = null,
    val length: Int? = null,
    val lastModified: com.google.firebase.Timestamp? = null
) {
    fun parsePropertiesFromProductId(): WarehouseSlot {
        val quality = this.productId.take(5)
        val parts = this.productId.split("-")

        if (parts.size < 5) {
            Log.d("Parsed parts", "Parsed parts: ${parts.size}")
        } else {

            val rawThickness = parts[2].toFloat()

            val thickness = when (rawThickness) {
                20.0f -> 20.0f
                27.0f -> 27.4f
                42.0f -> 42.4f
                else -> rawThickness
            }

            val rawWidth = parts[3].toFloat()

            val width = when (rawWidth) {
                42.0f -> 42.4f
                else -> rawWidth
            }

            val length = parts[4].toInt()

            return this.copy(
                quality = quality,
                thickness = thickness,
                width = width,
                length = length,
            )
        }
        return this
    }

    fun getVolume(): Double? {
        val quantity = this.quantity
        val width = this.width ?: return null
        val length = this.length ?: return null
        val thickness = this.thickness ?: return null
        val volume = ((quantity * length).toDouble() * thickness * width) / 1_000_000_000.0

        return volume
    }

    fun hasAllProperties(): Boolean {
        return this.quality != null && this.width != null && this.thickness != null && this.length != null
    }
/*
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WarehouseSlot
        return productId == other.productId
    }

    override fun hashCode(): Int {
        return productId.toInt()
    }*/
}

data class FirestoreSlot(
    val quantity: Int = 0,
    val lastModified: com.google.firebase.Timestamp? = null,
) {
    fun toWarehouseSlot(productId: String): WarehouseSlot {
        val slot = WarehouseSlot(
            productId = productId,
            quantity = this.quantity,
            lastModified = this.lastModified,
        )
        return slot.parsePropertiesFromProductId()
    }
}
