package eu.jelinek.hranolky.model

import android.util.Log
import eu.jelinek.hranolky.config.DimensionConfig
import eu.jelinek.hranolky.config.QualityConfig

data class WarehouseSlot(
    val fullProductId: String,
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

        val initialProductId =
            this.fullProductId // Use a consistent name for the original product ID

        // Determine the type and update productId using a 'let' block for scope
        val (type, processedProductId) = if (initialProductId.startsWith("H") || initialProductId.startsWith(
                "S"
            )
        ) {
            initialProductId.take(1) to initialProductId.substring(2)
        } else {
            "" to initialProductId // No type prefix, so type is empty and product ID remains unchanged
        }

        // Extract quality and split parts from the potentially modified product ID
        val parts = processedProductId.split("-")
        val quality = parts.getOrNull(0)?.let { firstPart ->
            parts.getOrNull(1)?.let { secondPart -> "$firstPart-$secondPart" }
        }

        if (parts.size < 5 || quality == null) {
            Log.d("Parsed parts", "Parsed parts: ${parts.size}")
        } else {

            val rawThickness = parts[2].toFloat()
            val thickness = DimensionConfig.adjustThickness(rawThickness)

            val rawWidth = parts[3].toFloat()
            val width = DimensionConfig.adjustWidth(rawWidth)

            val length = parts[4].toInt()

            return this.copy(
                slotType = when (type) {
                    "H" -> SlotType.Beam
                    "S" -> SlotType.Jointer
                    else -> {
                        SlotType.Beam
                    }
                },
                quality = QualityConfig.getFullQualityName(quality.replace('/', '|')),
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
        return QualityConfig.getFullQualityName(this.quality)
    }

    fun getShortQualityName(): String {
        return when (slotType) {
            SlotType.Beam -> this.fullProductId[2] + this.fullProductId.substring(5, 7)
            SlotType.Jointer -> this.fullProductId[2] + this.fullProductId.substring(5, 9).replace('|', '/')
            null -> this.quality ?: ""
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
            return "$type ${this.fullProductId}"

        val widthFormatted = this.width?.formatDimension() ?: ""
        val thicknessFormatted = this.thickness?.formatDimension() ?: ""

        return "$type ${this.quality}\n$thicknessFormatted x $widthFormatted x ${this.length} mm"
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
    val quantity: Long = 0L,
    val lastModified: com.google.firebase.Timestamp? = null,
    val quality: String? = null,
    val width: Double? = null,
    val thickness: Double? = null,
    val length: Long? = null,
) {
    fun toWarehouseSlot(slotType: SlotType, productId: String): WarehouseSlot {
        return WarehouseSlot(
            fullProductId = getFullProductId(slotType = slotType, productId = productId),
            slotType = slotType,
            quantity = this.quantity,
            lastModified = this.lastModified,
            quality = this.quality,
            width = this.width?.toFloat(),
            thickness = this.thickness?.toFloat(),
            length = this.length?.toInt(),
        )
    }
}

private fun getFullProductId(slotType: SlotType, productId: String): String {
    return when (slotType) {
        SlotType.Beam -> "H-$productId"
        SlotType.Jointer -> "S-$productId"
    }
}