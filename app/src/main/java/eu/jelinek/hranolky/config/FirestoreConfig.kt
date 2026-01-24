package eu.jelinek.hranolky.config

/**
 * Centralized Firestore configuration following Data Version Transparency (DVT) principle.
 *
 * All Firestore-related configuration values are defined here to:
 * 1. Eliminate hardcoded strings scattered across the codebase
 * 2. Enable easy modification without hunting through multiple files
 * 3. Support future versioning of collection/document names
 * 4. Provide single source of truth for Firestore structure
 */
object FirestoreConfig {

    /**
     * Root collection names in Firestore
     */
    object Collections {
        /** Collection for beam (hranolky) products */
        const val BEAMS = "Hranolky"

        /** Collection for jointer (spárovky) products */
        const val JOINTERS = "Sparovky"

        /** Collection for app configuration */
        const val APP_CONFIG = "AppConfig"

        /** Collection for device information */
        const val DEVICES = "Devices"
    }

    /**
     * Subcollection names within slot documents
     */
    object Subcollections {
        /** Actions performed on a slot (add, remove, check) */
        const val SLOT_ACTIONS = "SlotActions"

        /** Weekly reports for a slot */
        const val WEEKLY_REPORT = "SlotWeeklyReport"
    }

    /**
     * Document names within collections
     */
    object Documents {
        /** Latest app version configuration document */
        const val LATEST = "latest"
    }

    /**
     * Field names used in Firestore documents
     */
    object Fields {
        // Common slot fields
        const val QUANTITY = "quantity"
        const val LAST_MODIFIED = "lastModified"

        // SlotAction fields
        const val ACTION_TYPE = "actionType"
        const val TIMESTAMP = "timestamp"
        const val DEVICE_ID = "deviceId"
        const val CURRENT_QUANTITY = "currentQuantity"
        const val IS_UNDONE = "isUndone"

        // AppConfig fields
        const val VERSION = "version"
        const val VERSION_CODE = "versionCode"
        const val RELEASE_NOTES = "releaseNotes"
        const val APK_URL = "apkUrl"
    }
}
