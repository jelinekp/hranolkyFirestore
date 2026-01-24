package eu.jelinek.hranolky.domain.config

import kotlinx.coroutines.flow.Flow

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

    /**
     * Warm up the cache by pre-loading all configuration.
     * Non-blocking - returns immediately, loads in background.
     */
    suspend fun warmUp()

    /**
     * Start listening for real-time configuration changes.
     * Returns a Flow that emits when config changes are detected.
     */
    fun observeConfigChanges(): Flow<ConfigChangeEvent>

    /**
     * Stop listening for configuration changes.
     * Should be called when the app goes to background.
     */
    fun stopObservingChanges()

    /**
     * Get cache statistics for monitoring/debugging.
     */
    fun getCacheStats(): ConfigCache.CacheStats
}

/**
 * Event emitted when configuration changes.
 */
sealed class ConfigChangeEvent {
    data class QualityMappingsChanged(val mappings: Map<String, String>) : ConfigChangeEvent()
    data class DimensionAdjustmentsChanged(val adjustments: Map<Float, Float>) : ConfigChangeEvent()
    data class InventoryPeriodChanged(val days: Int) : ConfigChangeEvent()
    data class CollectionConfigChanged(val config: FirestoreCollectionConfig) : ConfigChangeEvent()
    data object AllConfigReloaded : ConfigChangeEvent()
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
