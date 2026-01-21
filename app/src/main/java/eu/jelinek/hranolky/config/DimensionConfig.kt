package eu.jelinek.hranolky.config

import eu.jelinek.hranolky.data.config.LocalConfigDefaults

/**
 * Dimension adjustment mappings following Data Version Transparency (DVT) principle.
 *
 * Maps nominal dimensions (as stored in product IDs) to actual dimensions (with tolerances).
 * These mappings account for manufacturing tolerances and drying shrinkage.
 *
 * Supports loading from remote configuration with fallback to local defaults.
 */
object DimensionConfig {

    /**
     * Remote dimension adjustments loaded from Firestore.
     * Null until loaded from remote config.
     */
    private var remoteDimensionAdjustments: Map<Float, Float>? = null

    /**
     * Maps nominal thickness values to actual thickness with tolerances.
     * Uses remote config if available, otherwise falls back to local defaults.
     */
    val thicknessAdjustments: Map<Float, Float>
        get() = remoteDimensionAdjustments ?: LocalConfigDefaults.dimensionAdjustments

    /**
     * Maps nominal width values to actual width with tolerances.
     * Note: Width adjustments share the same values as thickness adjustments.
     */
    val widthAdjustments: Map<Float, Float>
        get() = remoteDimensionAdjustments ?: LocalConfigDefaults.dimensionAdjustments

    /**
     * Update dimension adjustments from remote configuration.
     * Called during app initialization when remote config is loaded.
     */
    fun updateFromRemote(adjustments: Map<Float, Float>) {
        if (adjustments.isNotEmpty()) {
            remoteDimensionAdjustments = adjustments
        }
    }

    /**
     * Check if remote config is loaded.
     */
    fun isRemoteConfigLoaded(): Boolean = remoteDimensionAdjustments != null

    /**
     * Reset to local defaults (for testing).
     */
    fun resetToDefaults() {
        remoteDimensionAdjustments = null
    }

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
