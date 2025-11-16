package eu.jelinek.hranolky.model

import android.util.Log

data class WarehouseSlot(
    val productId: String,
    val quantity: Long,
    val slotActions: List<SlotAction> = emptyList(),
    val slotType: SlotType? = null,
    val quality: String? = null,
    val width: Float? = null,
    val thickness: Float? = null,
    val length: Int? = null,
    val lastModified: com.google.firebase.Timestamp? = null
) {
    fun parsePropertiesFromProductId(): WarehouseSlot {

        val initialProductId = this.productId // Use a consistent name for the original product ID

        // Determine the type and update productId using a 'let' block for scope
        val (type, processedProductId) = if (initialProductId.startsWith("H") || initialProductId.startsWith("S")) {
            initialProductId.take(1) to initialProductId.substring(2)
        } else {
            "" to initialProductId // No type prefix, so type is empty and product ID remains unchanged
        }

        // Extract quality and split parts from the potentially modified product ID
        val parts = processedProductId.split("-")
        val quality = parts[0] + "-" + parts[1]

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
                slotType = when (type) {
                    "H" -> SlotType.Beam
                    "S" -> SlotType.Jointer
                    else -> {
                        SlotType.Beam}
                },
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

    fun getFullQualityName(): String {
        return when (this.quality) {
            null -> ""
            "DUB-A|A" -> "DUB A/A"
            "DUB-A|B" -> "DUB A/B"
            "DUB-B|B" -> "DUB B/B"
            "DUB-B|A" -> "DUB B/A"
            "DUB-ABP" -> "DUB A/B-P"
            "DUB-RST" -> "DUB RUSTIK"
            "DUB-CNK" -> "DUB CINK"
            "DUB-RSC" -> "DUB RUSTIK CINK"
            "ZIR-ZIR" -> "ZIRBE"
            "ZIR-BMS" -> "ZIRBE MS"
            "ZIR-CNK" -> "ZIRBE CINK"
            "ZBD-BDC" -> "ZIRBE+BUK/DUB/BUK CINK/DUB CINK"
            "ZBD-CNK" -> "ZIRBE CINK+BUK/DUB/BUK CINK/DUB CINK"
            "BUK-BUK" -> "BUK"
            "BUK-CNK" -> "BUK CINK"
            "JSN-JSN" -> "JASAN"
            "KŠT-KŠT" -> "KAŠTAN"
            else -> this.quality
        }
    }

    fun hasAllProperties(): Boolean {
        return this.quality != null && this.width != null && this.thickness != null && this.length != null
    }

    private fun Float.formatDimension(): String {
        return if (this % 1.0f == 0f) {
            // It's a whole number, remove the .0
            this.toInt().toString()
        } else {
            // It has decimals, keep them
            this.toString()
        }
    }

    fun getScreenTitle(): String {
        val type = when (this.slotType) {
            SlotType.Beam -> "Hranolek"
            SlotType.Jointer -> "Spárovka"
            else -> ""
        }

        if (!hasAllProperties())
            return "$type ${this.productId}"

        val widthFormatted = this.width?.formatDimension() ?: ""
        val thicknessFormatted = this.thickness?.formatDimension() ?: ""

        return "$type ${this.getFullQualityName()}\n$widthFormatted x $thicknessFormatted x ${this.length} mm"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WarehouseSlot
        return productId == other.productId
    }

    override fun hashCode(): Int {
        return productId.toInt()
    }
}

data class FirestoreSlot(
    val quantity: Long = 0L,
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
