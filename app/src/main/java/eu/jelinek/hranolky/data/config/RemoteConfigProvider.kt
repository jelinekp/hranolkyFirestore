package eu.jelinek.hranolky.data.config

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import eu.jelinek.hranolky.domain.config.ConfigCache
import eu.jelinek.hranolky.domain.config.ConfigProvider
import eu.jelinek.hranolky.domain.config.FirestoreCollectionConfig
import kotlinx.coroutines.tasks.await

/**
 * ConfigProvider implementation that loads configuration from Firestore.
 * Falls back to local defaults when remote config is unavailable.
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
    private val cache: ConfigCache = ConfigCache()
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
