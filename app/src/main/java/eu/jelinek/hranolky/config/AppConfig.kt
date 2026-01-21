package eu.jelinek.hranolky.config

/**
 * Application-wide configuration following Data Version Transparency (DVT) principle.
 *
 * Contains business rules and configuration that may change over time.
 * Centralizing these values enables:
 * 1. Easy modification of business rules
 * 2. Potential future externalization to remote config
 * 3. Clear documentation of system parameters
 */
object AppConfig {

    /**
     * Inventory check configuration
     */
    object InventoryCheck {
        /** Number of days after which a slot is considered "due for inventory check" */
        const val STALE_DAYS = 75

        /** Prefix shown on slots that need inventory check */
        const val INDICATOR_PREFIX = "📋 "
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
