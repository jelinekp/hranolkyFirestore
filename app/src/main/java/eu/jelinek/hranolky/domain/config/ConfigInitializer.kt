package eu.jelinek.hranolky.domain.config

import android.util.Log
import eu.jelinek.hranolky.config.AppConfig
import eu.jelinek.hranolky.config.DimensionConfig
import eu.jelinek.hranolky.config.QualityConfig

/**
 * Initializes application configuration from remote source.
 * Updates static config objects with values from ConfigProvider.
 */
class ConfigInitializer(
    private val configProvider: ConfigProvider
) {
    companion object {
        private const val TAG = "ConfigInitializer"
    }

    /**
     * Load all configuration from remote source and update static config objects.
     * Falls back to local defaults if remote loading fails.
     */
    suspend fun initialize() {
        Log.d(TAG, "Initializing configuration...")

        try {
            // Load quality mappings
            val qualityMappings = configProvider.getQualityMappings()
            QualityConfig.updateFromRemote(qualityMappings)
            Log.d(TAG, "Loaded ${qualityMappings.size} quality mappings")

            // Load dimension adjustments
            val dimensionAdjustments = configProvider.getDimensionAdjustments()
            DimensionConfig.updateFromRemote(dimensionAdjustments)
            Log.d(TAG, "Loaded ${dimensionAdjustments.size} dimension adjustments")

            // Load inventory check period
            val inventoryPeriod = configProvider.getInventoryCheckPeriodDays()
            AppConfig.updateInventoryCheckPeriod(inventoryPeriod)
            Log.d(TAG, "Loaded inventory check period: $inventoryPeriod days")

            if (configProvider.isRemoteConfigLoaded()) {
                Log.i(TAG, "Remote configuration loaded successfully")
            } else {
                Log.i(TAG, "Using local default configuration")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load remote configuration, using defaults", e)
        }
    }

    /**
     * Invalidate cached config and reload from remote.
     */
    suspend fun refresh() {
        configProvider.invalidateCache()
        initialize()
    }
}
