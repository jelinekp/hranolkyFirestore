package eu.jelinek.hranolky.domain.update

/**
 * State machine for the app update flow.
 *
 * This implements the Separation of States (SoS) principle by defining
 * clear state transitions and ensuring only valid transitions can occur.
 *
 * State Flow:
 * ```
 * ┌─────────┐     check()     ┌──────────┐
 * │  Idle   │ ──────────────> │ Checking │
 * └─────────┘                 └──────────┘
 *      │                           │
 *      │                    ┌──────┴──────┐
 *      │                    │             │
 *      │             noUpdate()      updateFound()
 *      │                    │             │
 *      │                    v             v
 *      │               ┌─────────┐  ┌───────────┐
 *      │               │UpToDate │  │ Available │
 *      │               └─────────┘  └───────────┘
 *      │                                  │
 *      │                           startDownload()
 *      │                                  │
 *      │                                  v
 *      │                          ┌─────────────┐
 *      │                          │ Downloading │ <─────────┐
 *      │                          └─────────────┘           │
 *      │                                  │          progress()
 *      │                    ┌─────────────┼─────────────┘
 *      │                    │             │
 *      │             error()         complete()
 *      │                    │             │
 *      │                    v             v
 *      │               ┌─────────┐  ┌─────────────┐
 *      └<──────────────│  Error  │  │  Downloaded │
 *                      └─────────┘  └─────────────┘
 *                                         │
 *                                   startInstall()
 *                                         │
 *                                         v
 *                                  ┌────────────┐
 *                                  │ Installing │
 *                                  └────────────┘
 *                                         │
 *                          ┌──────────────┼──────────────┐
 *                          │              │              │
 *                    error()        success()    userAction()
 *                          │              │              │
 *                          v              v              v
 *                     ┌─────────┐  ┌───────────┐  ┌─────────────┐
 *                     │  Error  │  │ Installed │  │PendingAction│
 *                     └─────────┘  └───────────┘  └─────────────┘
 * ```
 */
sealed class UpdateFlowState {

    /** Initial state - no update check performed */
    data object Idle : UpdateFlowState()

    /** Checking for available updates */
    data object Checking : UpdateFlowState()

    /** App is up to date */
    data object UpToDate : UpdateFlowState()

    /** Update is available */
    data class Available(
        val version: String,
        val versionCode: Int,
        val releaseNotes: String,
        val downloadUrl: String
    ) : UpdateFlowState()

    /** Downloading update */
    data class Downloading(
        val version: String,
        val versionCode: Int,
        val progress: Int
    ) : UpdateFlowState()

    /** Download completed, ready to install */
    data class Downloaded(
        val version: String,
        val versionCode: Int,
        val filePath: String
    ) : UpdateFlowState()

    /** Installing update */
    data class Installing(
        val version: String,
        val versionCode: Int
    ) : UpdateFlowState()

    /** Waiting for user action (system install prompt) */
    data class PendingUserAction(
        val version: String,
        val versionCode: Int
    ) : UpdateFlowState()

    /** Installation completed (app will restart) */
    data class Installed(
        val version: String,
        val versionCode: Int
    ) : UpdateFlowState()

    /** Error occurred */
    data class Error(
        val phase: UpdatePhase,
        val message: String,
        val recoverable: Boolean = true
    ) : UpdateFlowState()
}

/**
 * Phases of the update process (for error reporting)
 */
enum class UpdatePhase {
    CHECK,
    DOWNLOAD,
    INSTALL
}

/**
 * Update flow state machine that validates transitions.
 */
class UpdateStateMachine(
    private val onStateChange: (UpdateFlowState) -> Unit
) {
    private var currentState: UpdateFlowState = UpdateFlowState.Idle

    val state: UpdateFlowState get() = currentState

    /**
     * Transition to checking state
     */
    fun startCheck(): Boolean {
        if (currentState !is UpdateFlowState.Idle &&
            currentState !is UpdateFlowState.UpToDate &&
            currentState !is UpdateFlowState.Error) {
            return false
        }
        transition(UpdateFlowState.Checking)
        return true
    }

    /**
     * Transition to up-to-date state
     */
    fun setUpToDate(): Boolean {
        if (currentState !is UpdateFlowState.Checking) {
            return false
        }
        transition(UpdateFlowState.UpToDate)
        return true
    }

    /**
     * Transition to update available state
     */
    fun setUpdateAvailable(version: String, versionCode: Int, releaseNotes: String, downloadUrl: String): Boolean {
        if (currentState !is UpdateFlowState.Checking) {
            return false
        }
        transition(UpdateFlowState.Available(version, versionCode, releaseNotes, downloadUrl))
        return true
    }

    /**
     * Start downloading the update
     */
    fun startDownload(): Boolean {
        val available = currentState as? UpdateFlowState.Available ?: return false
        transition(UpdateFlowState.Downloading(available.version, available.versionCode, 0))
        return true
    }

    /**
     * Update download progress
     */
    fun updateProgress(progress: Int): Boolean {
        val downloading = currentState as? UpdateFlowState.Downloading ?: return false
        transition(downloading.copy(progress = progress.coerceIn(0, 100)))
        return true
    }

    /**
     * Mark download as complete
     */
    fun downloadComplete(filePath: String): Boolean {
        val downloading = currentState as? UpdateFlowState.Downloading ?: return false
        transition(UpdateFlowState.Downloaded(downloading.version, downloading.versionCode, filePath))
        return true
    }

    /**
     * Start installation
     */
    fun startInstall(): Boolean {
        val downloaded = currentState as? UpdateFlowState.Downloaded ?: return false
        transition(UpdateFlowState.Installing(downloaded.version, downloaded.versionCode))
        return true
    }

    /**
     * Mark installation as pending user action
     */
    fun pendingUserAction(): Boolean {
        val installing = currentState as? UpdateFlowState.Installing ?: return false
        transition(UpdateFlowState.PendingUserAction(installing.version, installing.versionCode))
        return true
    }

    /**
     * Mark installation as complete
     */
    fun installComplete(): Boolean {
        val state = currentState
        if (state !is UpdateFlowState.Installing && state !is UpdateFlowState.PendingUserAction) {
            return false
        }
        val (version, versionCode) = when (state) {
            is UpdateFlowState.Installing -> state.version to state.versionCode
            is UpdateFlowState.PendingUserAction -> state.version to state.versionCode
            else -> return false
        }
        transition(UpdateFlowState.Installed(version, versionCode))
        return true
    }

    /**
     * Set error state
     */
    fun setError(phase: UpdatePhase, message: String, recoverable: Boolean = true): Boolean {
        // Error can occur from most states
        when (currentState) {
            is UpdateFlowState.Idle,
            is UpdateFlowState.UpToDate,
            is UpdateFlowState.Installed -> return false
            else -> {
                transition(UpdateFlowState.Error(phase, message, recoverable))
                return true
            }
        }
    }

    /**
     * Reset to idle state (e.g., for retry)
     */
    fun reset(): Boolean {
        val error = currentState as? UpdateFlowState.Error ?: return false
        if (!error.recoverable) return false
        transition(UpdateFlowState.Idle)
        return true
    }

    private fun transition(newState: UpdateFlowState) {
        currentState = newState
        onStateChange(newState)
    }
}

/**
 * Extension to convert UpdateFlowState to legacy UpdateState
 */
fun UpdateFlowState.toLegacyUpdateState(): UpdateState = when (this) {
    is UpdateFlowState.Idle -> UpdateState()
    is UpdateFlowState.Checking -> UpdateState()
    is UpdateFlowState.UpToDate -> UpdateState()
    is UpdateFlowState.Available -> UpdateState(
        isUpdateAvailable = true,
        latestVersion = version,
        latestVersionCode = versionCode,
        releaseNotes = releaseNotes
    )
    is UpdateFlowState.Downloading -> UpdateState(
        isUpdateAvailable = true,
        isDownloading = true,
        downloadProgress = progress,
        latestVersion = version,
        latestVersionCode = versionCode
    )
    is UpdateFlowState.Downloaded -> UpdateState(
        isUpdateAvailable = true,
        downloadProgress = 100,
        latestVersion = version,
        latestVersionCode = versionCode
    )
    is UpdateFlowState.Installing -> UpdateState(
        isUpdateAvailable = true,
        isInstalling = true,
        downloadProgress = 100,
        latestVersion = version,
        latestVersionCode = versionCode
    )
    is UpdateFlowState.PendingUserAction -> UpdateState(
        isUpdateAvailable = true,
        isInstalling = true,
        downloadProgress = 100,
        latestVersion = version,
        latestVersionCode = versionCode
    )
    is UpdateFlowState.Installed -> UpdateState(
        justUpdated = true,
        latestVersion = version,
        latestVersionCode = versionCode
    )
    is UpdateFlowState.Error -> UpdateState(
        error = message
    )
}
