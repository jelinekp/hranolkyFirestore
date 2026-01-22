package eu.jelinek.hranolky.data.config

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import eu.jelinek.hranolky.domain.config.ConfigCache
import eu.jelinek.hranolky.domain.config.ConfigChangeEvent
import eu.jelinek.hranolky.domain.config.ConfigProvider
import eu.jelinek.hranolky.domain.config.FirestoreCollectionConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ConfigProvider implementation that loads configuration from Firestore.
 * Falls back to local defaults when remote config is unavailable.
 *
 * Features:
 * - Real-time updates via Firestore listener (differential updates)
 * - 24-hour cache TTL with stale-while-revalidate pattern
 * - Background refresh without blocking UI
 * - Cache warming on startup
 *
 * Firestore document structure:
 * ```
 * /AppConfig/settings
 * {
 *   "qualityMappings": {
 *     "DUB-A|A": "DUB A/A",
 *     ...
 *   },
 *   "dimensionAdjustments": {
 *     "27.0": 27.4,
 *     ...
 *   },
 *   "inventoryCheckPeriodDays": 75,
 *   "collections": {
 *     "beam": "Hranolky",
 *     "jointer": "Sparovky",
 *     "slotActions": "SlotActions",
 *     "weeklyReport": "SlotWeeklyReport"
 *   },
 *   "version": 1,
 *   "lastUpdated": Timestamp
 * }
 * ```
 */
class RemoteConfigProvider(
    private val firestore: FirebaseFirestore,
    private val cache: ConfigCache = ConfigCache(),
    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : ConfigProvider {

    private companion object {
        const val TAG = "RemoteConfigProvider"
        const val CONFIG_COLLECTION = "AppConfig"
        const val SETTINGS_DOCUMENT = "settings"

        // Field names in Firestore
        const val FIELD_QUALITY_MAPPINGS = "qualityMappings"
        const val FIELD_DIMENSION_ADJUSTMENTS = "dimensionAdjustments"
        const val FIELD_INVENTORY_CHECK_PERIOD = "inventoryCheckPeriodDays"
        const val FIELD_COLLECTIONS = "collections"

        // Collection config field names
        const val FIELD_BEAM_COLLECTION = "beam"
        const val FIELD_JOINTER_COLLECTION = "jointer"
        const val FIELD_SLOT_ACTIONS = "slotActions"
        const val FIELD_WEEKLY_REPORT = "weeklyReport"
    }

    private var remoteConfigLoaded = false
    private var listenerRegistration: ListenerRegistration? = null

    override suspend fun getQualityMappings(): Map<String, String> {
        return cache.getOrFetch(ConfigCache.KEY_QUALITY_MAPPINGS) {
            fetchQualityMappings()
        } ?: LocalConfigDefaults.qualityMappings
    }

    override suspend fun getDimensionAdjustments(): Map<Float, Float> {
        return cache.getOrFetch(ConfigCache.KEY_DIMENSION_ADJUSTMENTS) {
            fetchDimensionAdjustments()
        } ?: LocalConfigDefaults.dimensionAdjustments
    }

    override suspend fun getInventoryCheckPeriodDays(): Int {
        return cache.getOrFetch(ConfigCache.KEY_INVENTORY_CHECK_PERIOD) {
            fetchInventoryCheckPeriod()
        } ?: LocalConfigDefaults.inventoryCheckPeriodDays
    }

    override suspend fun getCollectionConfig(): FirestoreCollectionConfig {
        return cache.getOrFetch(ConfigCache.KEY_COLLECTION_CONFIG) {
            fetchCollectionConfig()
        } ?: LocalConfigDefaults.collectionConfig
    }

    override fun invalidateCache() {
        cache.invalidate()
        remoteConfigLoaded = false
    }

    override fun isRemoteConfigLoaded(): Boolean = remoteConfigLoaded

    override suspend fun warmUp() {
        Log.d(TAG, "Warming up config cache...")
        // Fetch all config in parallel (coroutines will suspend independently)
        try {
            // These will fetch from remote and populate cache
            getQualityMappings()
            getDimensionAdjustments()
            getInventoryCheckPeriodDays()
            getCollectionConfig()
            Log.d(TAG, "Cache warm-up complete. Stats: ${cache.getStats()}")
        } catch (e: Exception) {
            Log.w(TAG, "Cache warm-up failed (using defaults): ${e.message}")
        }
    }

    override fun observeConfigChanges(): Flow<ConfigChangeEvent> = callbackFlow {
        Log.d(TAG, "Starting real-time config listener")

        listenerRegistration = firestore
            .collection(CONFIG_COLLECTION)
            .document(SETTINGS_DOCUMENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Config listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    Log.d(TAG, "Config document doesn't exist")
                    return@addSnapshotListener
                }

                Log.d(TAG, "Config document changed, applying differential updates")

                // Process changes in background to not block the listener
                backgroundScope.launch {
                    applyDifferentialUpdates(snapshot) { event ->
                        trySend(event)
                    }
                }
            }

        awaitClose {
            Log.d(TAG, "Stopping real-time config listener")
            listenerRegistration?.remove()
            listenerRegistration = null
        }
    }

    override fun stopObservingChanges() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    override fun getCacheStats(): ConfigCache.CacheStats = cache.getStats()

    /**
     * Apply only the changed values from a Firestore snapshot.
     * Compares with cached values and only updates what changed.
     */
    private suspend fun applyDifferentialUpdates(
        snapshot: DocumentSnapshot,
        onChanged: (ConfigChangeEvent) -> Unit
    ) {
        // Check quality mappings
        @Suppress("UNCHECKED_CAST")
        val newQualityMappings = snapshot.get(FIELD_QUALITY_MAPPINGS) as? Map<String, String>
        if (newQualityMappings != null) {
            val cached = cache.getIfCached<Map<String, String>>(ConfigCache.KEY_QUALITY_MAPPINGS)
            if (cached != newQualityMappings) {
                cache.put(ConfigCache.KEY_QUALITY_MAPPINGS, newQualityMappings)
                remoteConfigLoaded = true
                Log.d(TAG, "Quality mappings updated: ${newQualityMappings.size} entries")
                onChanged(ConfigChangeEvent.QualityMappingsChanged(newQualityMappings))
            }
        }

        // Check dimension adjustments
        @Suppress("UNCHECKED_CAST")
        val rawDimensions = snapshot.get(FIELD_DIMENSION_ADJUSTMENTS) as? Map<String, Any>
        val newDimensionAdjustments = rawDimensions?.mapNotNull { (key, value) ->
            try {
                val floatKey = key.toFloat()
                val floatValue = when (value) {
                    is Number -> value.toFloat()
                    is String -> value.toFloat()
                    else -> null
                }
                if (floatValue != null) floatKey to floatValue else null
            } catch (e: NumberFormatException) {
                null
            }
        }?.toMap()

        if (newDimensionAdjustments != null) {
            val cached = cache.getIfCached<Map<Float, Float>>(ConfigCache.KEY_DIMENSION_ADJUSTMENTS)
            if (cached != newDimensionAdjustments) {
                cache.put(ConfigCache.KEY_DIMENSION_ADJUSTMENTS, newDimensionAdjustments)
                remoteConfigLoaded = true
                Log.d(TAG, "Dimension adjustments updated: ${newDimensionAdjustments.size} entries")
                onChanged(ConfigChangeEvent.DimensionAdjustmentsChanged(newDimensionAdjustments))
            }
        }

        // Check inventory period
        val newPeriod = snapshot.getLong(FIELD_INVENTORY_CHECK_PERIOD)?.toInt()
        if (newPeriod != null) {
            val cached = cache.getIfCached<Int>(ConfigCache.KEY_INVENTORY_CHECK_PERIOD)
            if (cached != newPeriod) {
                cache.put(ConfigCache.KEY_INVENTORY_CHECK_PERIOD, newPeriod)
                remoteConfigLoaded = true
                Log.d(TAG, "Inventory period updated: $newPeriod days")
                onChanged(ConfigChangeEvent.InventoryPeriodChanged(newPeriod))
            }
        }

        // Check collection config
        @Suppress("UNCHECKED_CAST")
        val collectionsMap = snapshot.get(FIELD_COLLECTIONS) as? Map<String, String>
        if (collectionsMap != null) {
            val newConfig = FirestoreCollectionConfig(
                beamCollection = collectionsMap[FIELD_BEAM_COLLECTION]
                    ?: LocalConfigDefaults.collectionConfig.beamCollection,
                jointerCollection = collectionsMap[FIELD_JOINTER_COLLECTION]
                    ?: LocalConfigDefaults.collectionConfig.jointerCollection,
                slotActionsSubcollection = collectionsMap[FIELD_SLOT_ACTIONS]
                    ?: LocalConfigDefaults.collectionConfig.slotActionsSubcollection,
                weeklyReportSubcollection = collectionsMap[FIELD_WEEKLY_REPORT]
                    ?: LocalConfigDefaults.collectionConfig.weeklyReportSubcollection
            )

            val cached = cache.getIfCached<FirestoreCollectionConfig>(ConfigCache.KEY_COLLECTION_CONFIG)
            if (cached != newConfig) {
                cache.put(ConfigCache.KEY_COLLECTION_CONFIG, newConfig)
                remoteConfigLoaded = true
                Log.d(TAG, "Collection config updated")
                onChanged(ConfigChangeEvent.CollectionConfigChanged(newConfig))
            }
        }
    }

    private suspend fun fetchQualityMappings(): Map<String, String>? {
        return try {
            val document = firestore
                .collection(CONFIG_COLLECTION)
                .document(SETTINGS_DOCUMENT)
                .get()
                .await()

            @Suppress("UNCHECKED_CAST")
            val mappings = document.get(FIELD_QUALITY_MAPPINGS) as? Map<String, String>

            if (mappings != null && mappings.isNotEmpty()) {
                remoteConfigLoaded = true
                Log.d(TAG, "Loaded ${mappings.size} quality mappings from remote config")
            }

            mappings
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch quality mappings from Firestore", e)
            null
        }
    }

    private suspend fun fetchDimensionAdjustments(): Map<Float, Float>? {
        return try {
            val document = firestore
                .collection(CONFIG_COLLECTION)
                .document(SETTINGS_DOCUMENT)
                .get()
                .await()

            @Suppress("UNCHECKED_CAST")
            val rawMap = document.get(FIELD_DIMENSION_ADJUSTMENTS) as? Map<String, Any>

            // Convert string keys to float (Firestore doesn't support numeric keys in maps)
            val adjustments = rawMap?.mapNotNull { (key, value) ->
                try {
                    val floatKey = key.toFloat()
                    val floatValue = when (value) {
                        is Number -> value.toFloat()
                        is String -> value.toFloat()
                        else -> null
                    }
                    if (floatValue != null) floatKey to floatValue else null
                } catch (e: NumberFormatException) {
                    null
                }
            }?.toMap()

            if (adjustments != null && adjustments.isNotEmpty()) {
                remoteConfigLoaded = true
                Log.d(TAG, "Loaded ${adjustments.size} dimension adjustments from remote config")
            }

            adjustments
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch dimension adjustments from Firestore", e)
            null
        }
    }

    private suspend fun fetchInventoryCheckPeriod(): Int? {
        return try {
            val document = firestore
                .collection(CONFIG_COLLECTION)
                .document(SETTINGS_DOCUMENT)
                .get()
                .await()

            val period = document.getLong(FIELD_INVENTORY_CHECK_PERIOD)?.toInt()

            if (period != null) {
                remoteConfigLoaded = true
                Log.d(TAG, "Loaded inventory check period from remote config: $period days")
            }

            period
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch inventory check period from Firestore", e)
            null
        }
    }

    private suspend fun fetchCollectionConfig(): FirestoreCollectionConfig? {
        return try {
            val document = firestore
                .collection(CONFIG_COLLECTION)
                .document(SETTINGS_DOCUMENT)
                .get()
                .await()

            @Suppress("UNCHECKED_CAST")
            val collectionsMap = document.get(FIELD_COLLECTIONS) as? Map<String, String>

            if (collectionsMap == null) {
                return null
            }

            val config = FirestoreCollectionConfig(
                beamCollection = collectionsMap[FIELD_BEAM_COLLECTION]
                    ?: LocalConfigDefaults.collectionConfig.beamCollection,
                jointerCollection = collectionsMap[FIELD_JOINTER_COLLECTION]
                    ?: LocalConfigDefaults.collectionConfig.jointerCollection,
                slotActionsSubcollection = collectionsMap[FIELD_SLOT_ACTIONS]
                    ?: LocalConfigDefaults.collectionConfig.slotActionsSubcollection,
                weeklyReportSubcollection = collectionsMap[FIELD_WEEKLY_REPORT]
                    ?: LocalConfigDefaults.collectionConfig.weeklyReportSubcollection
            )

            remoteConfigLoaded = true
            Log.d(TAG, "Loaded collection config from remote config")

            config
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch collection config from Firestore", e)
            null
        }
    }
}
