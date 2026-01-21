package eu.jelinek.hranolky.config

/**
 * Dimension adjustment mappings following Data Version Transparency (DVT) principle.
 *
 * Maps nominal dimensions (as stored in product IDs) to actual dimensions (with tolerances).
 * These mappings account for manufacturing tolerances and drying shrinkage.
 */
object DimensionConfig {

    /**
     * Maps nominal thickness values to actual thickness with tolerances.
     * Key: Nominal thickness from product ID
     * Value: Actual thickness for volume calculations
     */
    val thicknessAdjustments: Map<Float, Float> = mapOf(
        20.0f to 20.0f,   // 20mm stays 20mm
        27.0f to 27.4f,   // 27mm nominal is actually 27.4mm
        42.0f to 42.4f    // 42mm nominal is actually 42.4mm
    )

    /**
     * Maps nominal width values to actual width with tolerances.
     * Key: Nominal width from product ID
     * Value: Actual width for volume calculations
     */
    val widthAdjustments: Map<Float, Float> = mapOf(
        42.0f to 42.4f    // 42mm nominal is actually 42.4mm
    )

    /**
     * Adjusts a nominal thickness value to actual thickness.
     * Returns the adjustment if found, otherwise returns the original value.
     */
    fun adjustThickness(nominal: Float): Float {
        return thicknessAdjustments[nominal] ?: nominal
    }

    /**
     * Adjusts a nominal width value to actual width.
     * Returns the adjustment if found, otherwise returns the original value.
     */
    fun adjustWidth(nominal: Float): Float {
        return widthAdjustments[nominal] ?: nominal
    }
}
