package eu.jelinek.hranolky.data.config

import eu.jelinek.hranolky.domain.config.ConfigProvider
import eu.jelinek.hranolky.domain.config.FirestoreCollectionConfig

/**
 * Local (compile-time) configuration defaults.
 * Used as fallback when remote configuration is unavailable.
 *
 * Following Data Version Transparency (DVT) principle, these values are
 * centralized here and can be overridden by remote configuration.
 */
object LocalConfigDefaults {

    /**
     * Default quality code to display name mappings.
     */
    val qualityMappings: Map<String, String> = mapOf(
        // Oak (DUB) grades
        "DUB-A|A" to "DUB A/A",
        "DUB-A|B" to "DUB A/B",
        "DUB-B|B" to "DUB B/B",
        "DUB-B|A" to "DUB B/A",
        "DUB-ABP" to "DUB A/B-P",
        "DUB-RST" to "DUB RUSTIK",
        "DUB-CNK" to "DUB CINK",
        "DUB-RSC" to "DUB RUSTIK CINK",

        // Stone pine (ZIRBE) grades
        "ZIR-ZIR" to "ZIRBE",
        "ZIR-BMS" to "ZIRBE MS",
        "ZIR-CNK" to "ZIRBE CINK",

        // Mixed grades
        "ZBD-BDC" to "ZIRBE+BUK/DUB/BUK CINK/DUB CINK",
        "ZBD-CNK" to "ZIRBE CINK+BUK/DUB/BUK CINK/DUB CINK",

        // Beech (BUK) grades
        "BUK-BUK" to "BUK",
        "BUK-CNK" to "BUK CINK",

        // Other wood types
        "JSN-JSN" to "JASAN",  // Ash
        "KŠT-KŠT" to "KAŠTAN" // Chestnut
    )

    /**
     * Default dimension adjustments.
     * Maps raw dimension values to adjusted display values.
     */
    val dimensionAdjustments: Map<Float, Float> = mapOf(
        27.0f to 27.4f,
        32.0f to 32.4f,
        42.0f to 42.4f,
        52.0f to 52.5f
    )

    /**
     * Default inventory check period in days.
     */
    const val inventoryCheckPeriodDays: Int = 75

    /**
     * Default Firestore collection configuration.
     */
    val collectionConfig = FirestoreCollectionConfig(
        beamCollection = "Hranolky",
        jointerCollection = "Sparovky",
        slotActionsSubcollection = "SlotActions",
        weeklyReportSubcollection = "SlotWeeklyReport"
    )
}

/**
 * ConfigProvider implementation that only uses local defaults.
 * Used when remote configuration is unavailable or as a fallback.
 */
class LocalConfigProvider : ConfigProvider {

    override suspend fun getQualityMappings(): Map<String, String> =
        LocalConfigDefaults.qualityMappings

    override suspend fun getDimensionAdjustments(): Map<Float, Float> =
        LocalConfigDefaults.dimensionAdjustments

    override suspend fun getInventoryCheckPeriodDays(): Int =
        LocalConfigDefaults.inventoryCheckPeriodDays

    override suspend fun getCollectionConfig(): FirestoreCollectionConfig =
        LocalConfigDefaults.collectionConfig

    override fun invalidateCache() {
        // No cache to invalidate for local defaults
    }

    override fun isRemoteConfigLoaded(): Boolean = false
}
