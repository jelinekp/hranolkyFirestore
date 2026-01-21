package eu.jelinek.hranolky.config

import eu.jelinek.hranolky.data.config.LocalConfigDefaults

/**
 * Application-wide configuration following Data Version Transparency (DVT) principle.
 *
 * Contains business rules and configuration that may change over time.
 * Centralizing these values enables:
 * 1. Easy modification of business rules
 * 2. Potential future externalization to remote config
 * 3. Clear documentation of system parameters
 *
 * Supports loading from remote configuration with fallback to local defaults.
 */
object AppConfig {

    /**
     * Remote inventory check period loaded from Firestore.
     * Null until loaded from remote config.
     */
    private var remoteInventoryCheckPeriodDays: Int? = null

    /**
     * Inventory check configuration
     */
    object InventoryCheck {
        /** Number of days after which a slot is considered "due for inventory check" */
        val staleDays: Int
            get() = remoteInventoryCheckPeriodDays ?: LocalConfigDefaults.inventoryCheckPeriodDays

        /** Legacy constant for backward compatibility */
        @Deprecated("Use staleDays property instead", ReplaceWith("staleDays"))
        const val STALE_DAYS = 75

        /** Prefix shown on slots that need inventory check */
        const val INDICATOR_PREFIX = "📋 "
    }

    /**
     * Update inventory check period from remote configuration.
     * Called during app initialization when remote config is loaded.
     */
    fun updateInventoryCheckPeriod(days: Int) {
        if (days > 0) {
            remoteInventoryCheckPeriodDays = days
        }
    }

    /**
     * Check if remote config is loaded.
     */
    fun isRemoteConfigLoaded(): Boolean = remoteInventoryCheckPeriodDays != null

    /**
     * Reset to local defaults (for testing).
     */
    fun resetToDefaults() {
        remoteInventoryCheckPeriodDays = null
    }

    /**
     * Update manager configuration
     */
    object Updates {
        /** FTP server URL for APK downloads */
        const val FTP_SERVER = "ftp.jelinek.eu"

        /** FTP username */
        const val FTP_USERNAME = "d076652"

        /** APK filename on FTP server */
        const val APK_FILENAME = "app-release.apk"

        /** Local download filename */
        const val LOCAL_APK_NAME = "hranolky_update.apk"
    }

    /**
     * Slot display configuration
     */
    object SlotDisplay {
        /** Number of last modified slots to show in history */
        const val HISTORY_LIMIT = 50

        /** Format for displaying dates in the app */
        const val DATE_FORMAT = "dd.MM.yyyy HH:mm"
    }

    /**
     * Slot ID configuration
     */
    object SlotId {
        /** Prefix for beam product IDs */
        const val BEAM_PREFIX = "H-"

        /** Prefix for jointer product IDs */
        const val JOINTER_PREFIX = "S-"

        /** Length of valid beam codes without prefix */
        const val BEAM_CODE_LENGTH_NO_PREFIX = 16

        /** Length of valid beam codes with H- prefix */
        const val BEAM_CODE_LENGTH_WITH_PREFIX = 18

        /** Length of valid jointer codes with S- prefix */
        const val JOINTER_CODE_LENGTH = 22
    }
}
