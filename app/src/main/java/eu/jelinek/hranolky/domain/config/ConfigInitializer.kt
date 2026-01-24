package eu.jelinek.hranolky.domain.config

import android.util.Log
import eu.jelinek.hranolky.config.AppConfig
import eu.jelinek.hranolky.config.DimensionConfig
import eu.jelinek.hranolky.config.QualityConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Initializes application configuration from remote source.
 * Updates static config objects with values from ConfigProvider.
 *
 * Features:
 * - Cache warming on startup (non-blocking)
 * - Real-time updates via Firestore listener
 * - Automatic fallback to local defaults
 * - Lifecycle-aware observation management
 */
class ConfigInitializer(
    private val configProvider: ConfigProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "ConfigInitializer"
    }

    private var observationJob: Job? = null

    /**
     * Load all configuration from remote source and update static config objects.
     * Falls back to local defaults if remote loading fails.
     * This is non-blocking and returns quickly.
     */
    suspend fun initialize() {
        Log.d(TAG, "Initializing configuration...")

        try {
            // Warm up cache - fetches all config in parallel
            configProvider.warmUp()

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

            // Log cache stats
            val stats = configProvider.getCacheStats()
            Log.d(TAG, "Cache stats after init: hits=${stats.hitCount}, misses=${stats.missCount}, hitRate=${String.format("%.1f", stats.hitRate * 100)}%")

        } catch (e: Exception) {
            Log.w(TAG, "Failed to load remote configuration, using defaults", e)
        }
    }

    /**
     * Start observing real-time configuration changes.
     * Should be called when app comes to foreground.
     */
    fun startObserving() {
        if (observationJob?.isActive == true) {
            Log.d(TAG, "Already observing config changes")
            return
        }

        Log.d(TAG, "Starting config change observation")
        observationJob = configProvider.observeConfigChanges()
            .onEach { event ->
                Log.d(TAG, "Config change event: $event")
                handleConfigChange(event)
            }
            .catch { e ->
                Log.w(TAG, "Config observation error", e)
            }
            .launchIn(scope)
    }

    /**
     * Stop observing configuration changes.
     * Should be called when app goes to background.
     */
    fun stopObserving() {
        Log.d(TAG, "Stopping config change observation")
        observationJob?.cancel()
        observationJob = null
        configProvider.stopObservingChanges()
    }

    /**
     * Handle a config change event by updating the appropriate static config.
     */
    private fun handleConfigChange(event: ConfigChangeEvent) {
        when (event) {
            is ConfigChangeEvent.QualityMappingsChanged -> {
                QualityConfig.updateFromRemote(event.mappings)
                Log.i(TAG, "Quality mappings updated: ${event.mappings.size} entries")
            }
            is ConfigChangeEvent.DimensionAdjustmentsChanged -> {
                DimensionConfig.updateFromRemote(event.adjustments)
                Log.i(TAG, "Dimension adjustments updated: ${event.adjustments.size} entries")
            }
            is ConfigChangeEvent.InventoryPeriodChanged -> {
                AppConfig.updateInventoryCheckPeriod(event.days)
                Log.i(TAG, "Inventory period updated: ${event.days} days")
            }
            is ConfigChangeEvent.CollectionConfigChanged -> {
                // Collection config is typically only used at startup
                Log.i(TAG, "Collection config updated (requires app restart to take effect)")
            }
            ConfigChangeEvent.AllConfigReloaded -> {
                Log.i(TAG, "All config reloaded")
            }
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
