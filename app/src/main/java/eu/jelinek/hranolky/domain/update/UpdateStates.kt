package eu.jelinek.hranolky.domain.update

/**
 * Isolated states for the app update flow, following Separation of States (SoS) principle.
 * Each state class focuses on a single concern within the update process.
 */

/**
 * Represents the availability status of an app update.
 * This state is independent of download/installation progress.
 */
sealed class UpdateAvailability {
    /** No update check has been performed yet */
    data object Unknown : UpdateAvailability()

    /** Currently checking for updates */
    data object Checking : UpdateAvailability()

    /** App is up to date */
    data object UpToDate : UpdateAvailability()

    /** A new version is available */
    data class Available(
        val version: String,
        val versionCode: Int,
        val releaseNotes: String
    ) : UpdateAvailability()

    /** Failed to check for updates */
    data class CheckFailed(val error: String) : UpdateAvailability()
}

/**
 * Represents the download state of an update.
 * Only relevant when an update is available.
 */
sealed class DownloadState {
    /** Download has not started */
    data object Idle : DownloadState()

    /** Download is in progress */
    data class Downloading(val progress: Int) : DownloadState()

    /** Download is paused (e.g., waiting for network) */
    data class Paused(val reason: PauseReason) : DownloadState()

    /** Download completed successfully */
    data object Completed : DownloadState()

    /** Download failed */
    data class Failed(val error: DownloadError) : DownloadState()
}

/**
 * Reasons why a download might be paused.
 */
enum class PauseReason {
    WAITING_FOR_WIFI,
    WAITING_FOR_NETWORK,
    WAITING_TO_RETRY,
    UNKNOWN
}

/**
 * Types of download errors.
 */
enum class DownloadError {
    CANNOT_RESUME,
    DEVICE_NOT_FOUND,
    FILE_ALREADY_EXISTS,
    FILE_ERROR,
    HTTP_DATA_ERROR,
    INSUFFICIENT_SPACE,
    TOO_MANY_REDIRECTS,
    UNHANDLED_HTTP_CODE,
    UNKNOWN
}

/**
 * Represents the installation state of an update.
 * Only relevant when download is completed.
 */
sealed class InstallationState {
    /** Installation has not started */
    data object Idle : InstallationState()

    /** Installation is preparing (setting up session) */
    data object Preparing : InstallationState()

    /** Installation is in progress */
    data object Installing : InstallationState()

    /** Waiting for user confirmation */
    data object PendingUserAction : InstallationState()

    /** Installation completed successfully */
    data object Completed : InstallationState()

    /** Installation failed */
    data class Failed(val error: InstallationError) : InstallationState()
}

/**
 * Types of installation errors.
 */
enum class InstallationError {
    ABORTED,
    BLOCKED,
    CONFLICT,
    INCOMPATIBLE,
    INVALID,
    STORAGE,
    UNKNOWN
}

/**
 * Represents post-update state.
 * Used to show user feedback after a successful update.
 */
data class PostUpdateState(
    val justUpdated: Boolean = false,
    val previousVersion: String? = null,
    val newVersion: String? = null
)

/**
 * Composite state that combines all update-related states.
 * This maintains backward compatibility while providing better state isolation.
 */
data class CompositeUpdateState(
    val availability: UpdateAvailability = UpdateAvailability.Unknown,
    val download: DownloadState = DownloadState.Idle,
    val installation: InstallationState = InstallationState.Idle,
    val postUpdate: PostUpdateState = PostUpdateState()
) {
    /**
     * Convert to legacy UpdateState for backward compatibility.
     */
    fun toLegacyState(): UpdateState {
        val isUpdateAvailable = availability is UpdateAvailability.Available
        val (version, versionCode, releaseNotes) = when (val avail = availability) {
            is UpdateAvailability.Available -> Triple(avail.version, avail.versionCode, avail.releaseNotes)
            else -> Triple("", 0, "")
        }

        val isDownloading = download is DownloadState.Downloading
        val downloadProgress = when (val dl = download) {
            is DownloadState.Downloading -> dl.progress
            is DownloadState.Completed -> 100
            else -> 0
        }

        val isInstalling = installation is InstallationState.Installing ||
                          installation is InstallationState.Preparing ||
                          installation is InstallationState.PendingUserAction

        val error = when {
            availability is UpdateAvailability.CheckFailed -> availability.error
            download is DownloadState.Failed -> download.error.name
            installation is InstallationState.Failed -> installation.error.name
            else -> null
        }

        return UpdateState(
            isUpdateAvailable = isUpdateAvailable,
            isDownloading = isDownloading,
            isInstalling = isInstalling,
            downloadProgress = downloadProgress,
            latestVersion = version,
            latestVersionCode = versionCode,
            releaseNotes = releaseNotes,
            error = error,
            justUpdated = postUpdate.justUpdated
        )
    }

    companion object {
        /**
         * Create from legacy UpdateState for migration purposes.
         */
        fun fromLegacyState(legacy: UpdateState): CompositeUpdateState {
            val availability = when {
                legacy.error != null -> UpdateAvailability.CheckFailed(legacy.error)
                legacy.isUpdateAvailable -> UpdateAvailability.Available(
                    version = legacy.latestVersion,
                    versionCode = legacy.latestVersionCode,
                    releaseNotes = legacy.releaseNotes
                )
                legacy.latestVersionCode > 0 -> UpdateAvailability.UpToDate
                else -> UpdateAvailability.Unknown
            }

            val download = when {
                legacy.isDownloading -> DownloadState.Downloading(legacy.downloadProgress)
                legacy.downloadProgress == 100 -> DownloadState.Completed
                else -> DownloadState.Idle
            }

            val installation = when {
                legacy.isInstalling -> InstallationState.Installing
                else -> InstallationState.Idle
            }

            return CompositeUpdateState(
                availability = availability,
                download = download,
                installation = installation,
                postUpdate = PostUpdateState(justUpdated = legacy.justUpdated)
            )
        }
    }
}

/**
 * Legacy UpdateState kept for backward compatibility.
 * New code should prefer using CompositeUpdateState with isolated states.
 */
data class UpdateState(
    val isUpdateAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val isInstalling: Boolean = false,
    val downloadProgress: Int = 0,
    val latestVersion: String = "",
    val latestVersionCode: Int = 0,
    val releaseNotes: String = "",
    val error: String? = null,
    val justUpdated: Boolean = false
)
