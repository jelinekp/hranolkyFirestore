package eu.jelinek.hranolky.config

import eu.jelinek.hranolky.data.config.LocalConfigDefaults

/**
 * Product quality code mappings following Data Version Transparency (DVT) principle.
 *
 * Maps internal quality codes to human-readable display names.
 * These mappings are business data that may need updates as new products are added.
 *
 * Supports loading from remote configuration with fallback to local defaults.
 */
object QualityConfig {

    /**
     * Remote quality mappings loaded from Firestore.
     * Null until loaded from remote config.
     */
    private var remoteQualityMappings: Map<String, String>? = null

    /**
     * Maps internal quality codes to full display names.
     * Uses remote config if available, otherwise falls back to local defaults.
     */
    val fullQualityNames: Map<String, String>
        get() = remoteQualityMappings ?: LocalConfigDefaults.qualityMappings

    /**
     * Update quality mappings from remote configuration.
     * Called during app initialization when remote config is loaded.
     */
    fun updateFromRemote(mappings: Map<String, String>) {
        if (mappings.isNotEmpty()) {
            remoteQualityMappings = mappings
        }
    }

    /**
     * Check if remote config is loaded.
     */
    fun isRemoteConfigLoaded(): Boolean = remoteQualityMappings != null

    /**
     * Reset to local defaults (for testing).
     */
    fun resetToDefaults() {
        remoteQualityMappings = null
    }

    /**
     * Gets the full quality name for a given code, or returns the code if not found.
     */
    fun getFullQualityName(code: String?): String {
        if (code == null) return ""
        return fullQualityNames[code] ?: code
    }
}
