package eu.jelinek.hranolky.domain.config

/**
 * Provider interface for application configuration.
 * Follows Data Version Transparency (DVT) by abstracting configuration sources.
 * Implementations can load from remote config, local storage, or compile-time defaults.
 */
interface ConfigProvider {
    /**
     * Get quality code to display name mappings.
     * Example: "DUB-A|A" -> "DUB A/A"
     */
    suspend fun getQualityMappings(): Map<String, String>

    /**
     * Get dimension adjustment mappings.
     * These adjust stored dimensions for display purposes.
     * Example: 27.0f -> 27.4f
     */
    suspend fun getDimensionAdjustments(): Map<Float, Float>

    /**
     * Get inventory check period in days.
     */
    suspend fun getInventoryCheckPeriodDays(): Int

    /**
     * Get Firestore collection configuration.
     */
    suspend fun getCollectionConfig(): FirestoreCollectionConfig

    /**
     * Invalidate cached configuration and force reload.
     */
    fun invalidateCache()

    /**
     * Check if configuration is loaded from remote source.
     */
    fun isRemoteConfigLoaded(): Boolean
}

/**
 * Configuration for Firestore collections.
 */
data class FirestoreCollectionConfig(
    val beamCollection: String,
    val jointerCollection: String,
    val slotActionsSubcollection: String,
    val weeklyReportSubcollection: String
)
