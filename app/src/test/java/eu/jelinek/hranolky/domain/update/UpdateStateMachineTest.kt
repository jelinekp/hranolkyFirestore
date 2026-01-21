package eu.jelinek.hranolky.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for UpdateStateMachine ensuring valid state transitions.
 */
class UpdateStateMachineTest {

    private lateinit var stateMachine: UpdateStateMachine
    private val stateChanges = mutableListOf<UpdateFlowState>()

    @Before
    fun setup() {
        stateChanges.clear()
        stateMachine = UpdateStateMachine { state ->
            stateChanges.add(state)
        }
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(UpdateFlowState.Idle, stateMachine.state)
    }

    @Test
    fun `can start check from Idle`() {
        assertTrue(stateMachine.startCheck())
        assertTrue(stateMachine.state is UpdateFlowState.Checking)
    }

    @Test
    fun `cannot start download from Idle`() {
        assertFalse(stateMachine.startDownload())
        assertEquals(UpdateFlowState.Idle, stateMachine.state)
    }

    @Test
    fun `cannot update progress from Idle`() {
        assertFalse(stateMachine.updateProgress(50))
    }

    @Test
    fun `can set up to date from Checking`() {
        stateMachine.startCheck()
        assertTrue(stateMachine.setUpToDate())
        assertTrue(stateMachine.state is UpdateFlowState.UpToDate)
    }

    @Test
    fun `can set update available from Checking`() {
        stateMachine.startCheck()
        assertTrue(stateMachine.setUpdateAvailable("1.2.0", 10, "New features", "http://example.com/app.apk"))

        val state = stateMachine.state as UpdateFlowState.Available
        assertEquals("1.2.0", state.version)
        assertEquals(10, state.versionCode)
        assertEquals("New features", state.releaseNotes)
        assertEquals("http://example.com/app.apk", state.downloadUrl)
    }

    @Test
    fun `cannot set up to date from Idle`() {
        assertFalse(stateMachine.setUpToDate())
    }

    @Test
    fun `can start download from Available`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://example.com")

        assertTrue(stateMachine.startDownload())

        val state = stateMachine.state as UpdateFlowState.Downloading
        assertEquals("1.2.0", state.version)
        assertEquals(10, state.versionCode)
        assertEquals(0, state.progress)
    }

    @Test
    fun `can update progress while Downloading`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://example.com")
        stateMachine.startDownload()

        assertTrue(stateMachine.updateProgress(50))

        val state = stateMachine.state as UpdateFlowState.Downloading
        assertEquals(50, state.progress)
    }

    @Test
    fun `progress is clamped to valid range`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://example.com")
        stateMachine.startDownload()

        stateMachine.updateProgress(150)
        assertEquals(100, (stateMachine.state as UpdateFlowState.Downloading).progress)

        stateMachine.updateProgress(-10)
        assertEquals(0, (stateMachine.state as UpdateFlowState.Downloading).progress)
    }

    @Test
    fun `can complete download`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://example.com")
        stateMachine.startDownload()
        stateMachine.updateProgress(100)

        assertTrue(stateMachine.downloadComplete("/path/to/file.apk"))

        val state = stateMachine.state as UpdateFlowState.Downloaded
        assertEquals("1.2.0", state.version)
        assertEquals("/path/to/file.apk", state.filePath)
    }

    @Test
    fun `can start install from Downloaded`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://example.com")
        stateMachine.startDownload()
        stateMachine.downloadComplete("/path/to/file.apk")

        assertTrue(stateMachine.startInstall())
        assertTrue(stateMachine.state is UpdateFlowState.Installing)
    }

    @Test
    fun `can mark pending user action from Installing`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://example.com")
        stateMachine.startDownload()
        stateMachine.downloadComplete("/path/to/file.apk")
        stateMachine.startInstall()

        assertTrue(stateMachine.pendingUserAction())
        assertTrue(stateMachine.state is UpdateFlowState.PendingUserAction)
    }

    @Test
    fun `can complete install from Installing`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://example.com")
        stateMachine.startDownload()
        stateMachine.downloadComplete("/path/to/file.apk")
        stateMachine.startInstall()

        assertTrue(stateMachine.installComplete())
        assertTrue(stateMachine.state is UpdateFlowState.Installed)
    }

    @Test
    fun `can complete install from PendingUserAction`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://example.com")
        stateMachine.startDownload()
        stateMachine.downloadComplete("/path/to/file.apk")
        stateMachine.startInstall()
        stateMachine.pendingUserAction()

        assertTrue(stateMachine.installComplete())
        assertTrue(stateMachine.state is UpdateFlowState.Installed)
    }

    @Test
    fun `can set error from Checking`() {
        stateMachine.startCheck()

        assertTrue(stateMachine.setError(UpdatePhase.CHECK, "Network error"))

        val error = stateMachine.state as UpdateFlowState.Error
        assertEquals(UpdatePhase.CHECK, error.phase)
        assertEquals("Network error", error.message)
        assertTrue(error.recoverable)
    }

    @Test
    fun `can set error from Downloading`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://example.com")
        stateMachine.startDownload()

        assertTrue(stateMachine.setError(UpdatePhase.DOWNLOAD, "Download failed"))
        assertTrue(stateMachine.state is UpdateFlowState.Error)
    }

    @Test
    fun `can set error from Installing`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://example.com")
        stateMachine.startDownload()
        stateMachine.downloadComplete("/path/to/file.apk")
        stateMachine.startInstall()

        assertTrue(stateMachine.setError(UpdatePhase.INSTALL, "Install failed"))
        assertTrue(stateMachine.state is UpdateFlowState.Error)
    }

    @Test
    fun `cannot set error from Idle`() {
        assertFalse(stateMachine.setError(UpdatePhase.CHECK, "Error"))
        assertEquals(UpdateFlowState.Idle, stateMachine.state)
    }

    @Test
    fun `cannot set error from UpToDate`() {
        stateMachine.startCheck()
        stateMachine.setUpToDate()

        assertFalse(stateMachine.setError(UpdatePhase.CHECK, "Error"))
        assertTrue(stateMachine.state is UpdateFlowState.UpToDate)
    }

    @Test
    fun `can reset from recoverable Error`() {
        stateMachine.startCheck()
        stateMachine.setError(UpdatePhase.CHECK, "Error", recoverable = true)

        assertTrue(stateMachine.reset())
        assertEquals(UpdateFlowState.Idle, stateMachine.state)
    }

    @Test
    fun `cannot reset from non-recoverable Error`() {
        stateMachine.startCheck()
        stateMachine.setError(UpdatePhase.CHECK, "Fatal error", recoverable = false)

        assertFalse(stateMachine.reset())
        assertTrue(stateMachine.state is UpdateFlowState.Error)
    }

    @Test
    fun `can restart check from UpToDate`() {
        stateMachine.startCheck()
        stateMachine.setUpToDate()

        assertTrue(stateMachine.startCheck())
        assertTrue(stateMachine.state is UpdateFlowState.Checking)
    }

    @Test
    fun `can restart check from Error`() {
        stateMachine.startCheck()
        stateMachine.setError(UpdatePhase.CHECK, "Error")

        assertTrue(stateMachine.startCheck())
        assertTrue(stateMachine.state is UpdateFlowState.Checking)
    }

    @Test
    fun `state changes are notified`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://example.com")
        stateMachine.startDownload()
        stateMachine.updateProgress(50)

        assertEquals(4, stateChanges.size)
        assertTrue(stateChanges[0] is UpdateFlowState.Checking)
        assertTrue(stateChanges[1] is UpdateFlowState.Available)
        assertTrue(stateChanges[2] is UpdateFlowState.Downloading)
        assertTrue(stateChanges[3] is UpdateFlowState.Downloading)
    }

    @Test
    fun `full happy path flow works correctly`() {
        // Start check
        assertTrue(stateMachine.startCheck())
        assertTrue(stateMachine.state is UpdateFlowState.Checking)

        // Find update
        assertTrue(stateMachine.setUpdateAvailable("2.0.0", 20, "Major update", "http://example.com/v2.apk"))
        assertTrue(stateMachine.state is UpdateFlowState.Available)

        // Download
        assertTrue(stateMachine.startDownload())
        assertTrue(stateMachine.updateProgress(25))
        assertTrue(stateMachine.updateProgress(50))
        assertTrue(stateMachine.updateProgress(75))
        assertTrue(stateMachine.updateProgress(100))
        assertTrue(stateMachine.downloadComplete("/storage/app-v2.apk"))
        assertTrue(stateMachine.state is UpdateFlowState.Downloaded)

        // Install
        assertTrue(stateMachine.startInstall())
        assertTrue(stateMachine.state is UpdateFlowState.Installing)

        // Complete
        assertTrue(stateMachine.installComplete())

        val installed = stateMachine.state as UpdateFlowState.Installed
        assertEquals("2.0.0", installed.version)
        assertEquals(20, installed.versionCode)
    }
}

/**
 * Tests for UpdateFlowState to legacy UpdateState conversion
 */
class UpdateFlowStateLegacyConversionTest {

    @Test
    fun `Idle converts to empty UpdateState`() {
        val legacy = UpdateFlowState.Idle.toLegacyUpdateState()
        assertFalse(legacy.isUpdateAvailable)
        assertFalse(legacy.isDownloading)
        assertFalse(legacy.isInstalling)
    }

    @Test
    fun `Available converts correctly`() {
        val state = UpdateFlowState.Available("1.2.0", 10, "New features", "http://example.com")
        val legacy = state.toLegacyUpdateState()

        assertTrue(legacy.isUpdateAvailable)
        assertEquals("1.2.0", legacy.latestVersion)
        assertEquals(10, legacy.latestVersionCode)
        assertEquals("New features", legacy.releaseNotes)
        assertFalse(legacy.isDownloading)
    }

    @Test
    fun `Downloading converts correctly`() {
        val state = UpdateFlowState.Downloading("1.2.0", 10, 45)
        val legacy = state.toLegacyUpdateState()

        assertTrue(legacy.isUpdateAvailable)
        assertTrue(legacy.isDownloading)
        assertEquals(45, legacy.downloadProgress)
    }

    @Test
    fun `Installing converts correctly`() {
        val state = UpdateFlowState.Installing("1.2.0", 10)
        val legacy = state.toLegacyUpdateState()

        assertTrue(legacy.isUpdateAvailable)
        assertTrue(legacy.isInstalling)
        assertEquals(100, legacy.downloadProgress)
    }

    @Test
    fun `Installed converts correctly`() {
        val state = UpdateFlowState.Installed("1.2.0", 10)
        val legacy = state.toLegacyUpdateState()

        assertTrue(legacy.justUpdated)
        assertEquals("1.2.0", legacy.latestVersion)
    }

    @Test
    fun `Error converts correctly`() {
        val state = UpdateFlowState.Error(UpdatePhase.DOWNLOAD, "Connection lost", true)
        val legacy = state.toLegacyUpdateState()

        assertEquals("Connection lost", legacy.error)
    }
}
