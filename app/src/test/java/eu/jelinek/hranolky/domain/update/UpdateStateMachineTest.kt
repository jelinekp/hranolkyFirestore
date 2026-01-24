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
        stateMachine = UpdateStateMachine(
            onStateChange = { state ->
                stateChanges.add(state)
            }
        )
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
        assertTrue(stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://url"))
        assertTrue(stateMachine.state is UpdateFlowState.Available)
    }

    @Test
    fun `cannot set up to date from Idle`() {
        assertFalse(stateMachine.setUpToDate())
    }

    @Test
    fun `can start download from Available`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://url")

        assertTrue(stateMachine.startDownload())
        val state = stateMachine.state as UpdateFlowState.Downloading
        assertEquals("1.2.0", state.version)
        assertEquals(10, state.versionCode)
        assertEquals(0, state.progress)
    }

    @Test
    fun `can update progress from Downloading`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://url")
        stateMachine.startDownload()

        assertTrue(stateMachine.updateProgress(50))
        val state = stateMachine.state as UpdateFlowState.Downloading
        assertEquals(50, state.progress)
    }

    @Test
    fun `progress is clamped to 0-100`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://url")
        stateMachine.startDownload()

        stateMachine.updateProgress(150)
        assertEquals(100, (stateMachine.state as UpdateFlowState.Downloading).progress)

        stateMachine.updateProgress(-10)
        assertEquals(0, (stateMachine.state as UpdateFlowState.Downloading).progress)
    }

    @Test
    fun `can complete download from Downloading`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://url")
        stateMachine.startDownload()

        assertTrue(stateMachine.downloadComplete("/path/to/file.apk"))
        val state = stateMachine.state as UpdateFlowState.Downloaded
        assertEquals("/path/to/file.apk", state.filePath)
    }

    @Test
    fun `can start install from Downloaded`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://url")
        stateMachine.startDownload()
        stateMachine.downloadComplete("/path/to/file.apk")

        assertTrue(stateMachine.startInstall())
        assertTrue(stateMachine.state is UpdateFlowState.Installing)
    }

    @Test
    fun `can mark pending user action from Installing`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://url")
        stateMachine.startDownload()
        stateMachine.downloadComplete("/path/to/file.apk")
        stateMachine.startInstall()

        assertTrue(stateMachine.pendingUserAction())
        assertTrue(stateMachine.state is UpdateFlowState.PendingUserAction)
    }

    @Test
    fun `can complete install from Installing`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://url")
        stateMachine.startDownload()
        stateMachine.downloadComplete("/path/to/file.apk")
        stateMachine.startInstall()

        assertTrue(stateMachine.installComplete())
        assertTrue(stateMachine.state is UpdateFlowState.Installed)
    }

    @Test
    fun `can complete install from PendingUserAction`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://url")
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
        val state = stateMachine.state as UpdateFlowState.Error
        assertEquals(UpdatePhase.CHECK, state.phase)
        assertEquals("Network error", state.message)
    }

    @Test
    fun `can set error from Downloading`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://url")
        stateMachine.startDownload()

        assertTrue(stateMachine.setError(UpdatePhase.DOWNLOAD, "Connection lost"))
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
        stateMachine.setError(UpdatePhase.CHECK, "Fatal", recoverable = false)

        assertFalse(stateMachine.reset())
        assertTrue(stateMachine.state is UpdateFlowState.Error)
    }

    @Test
    fun `can start check from UpToDate`() {
        stateMachine.startCheck()
        stateMachine.setUpToDate()

        assertTrue(stateMachine.startCheck())
        assertTrue(stateMachine.state is UpdateFlowState.Checking)
    }

    @Test
    fun `can start check from Error`() {
        stateMachine.startCheck()
        stateMachine.setError(UpdatePhase.CHECK, "Error")

        assertTrue(stateMachine.startCheck())
        assertTrue(stateMachine.state is UpdateFlowState.Checking)
    }

    @Test
    fun `state changes are notified`() {
        stateMachine.startCheck()
        stateMachine.setUpdateAvailable("1.2.0", 10, "Notes", "http://url")
        stateMachine.startDownload()

        assertEquals(3, stateChanges.size)
        assertTrue(stateChanges[0] is UpdateFlowState.Checking)
        assertTrue(stateChanges[1] is UpdateFlowState.Available)
        assertTrue(stateChanges[2] is UpdateFlowState.Downloading)
    }

    // Legacy Conversion Tests

    @Test
    fun `Idle converts to empty legacy state`() {
        val legacy = UpdateFlowState.Idle.toLegacyUpdateState()

        assertFalse(legacy.isUpdateAvailable)
        assertFalse(legacy.isDownloading)
        assertFalse(legacy.isInstalling)
        assertFalse(legacy.justUpdated)
    }

    @Test
    fun `Available converts with update info`() {
        val state = UpdateFlowState.Available("1.2.0", 10, "Fix bugs", "http://url")
        val legacy = state.toLegacyUpdateState()

        assertTrue(legacy.isUpdateAvailable)
        assertEquals("1.2.0", legacy.latestVersion)
        assertEquals(10, legacy.latestVersionCode)
        assertEquals("Fix bugs", legacy.releaseNotes)
    }

    @Test
    fun `Downloading converts with progress`() {
        val state = UpdateFlowState.Downloading("1.2.0", 10, 75)
        val legacy = state.toLegacyUpdateState()

        assertTrue(legacy.isUpdateAvailable)
        assertTrue(legacy.isDownloading)
        assertEquals(75, legacy.downloadProgress)
    }

    @Test
    fun `Installing converts correctly`() {
        val state = UpdateFlowState.Installing("1.2.0", 10)
        val legacy = state.toLegacyUpdateState()

        assertTrue(legacy.isInstalling)
        assertEquals(100, legacy.downloadProgress)
    }

    @Test
    fun `Installed converts with justUpdated`() {
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

    // State History Tests

    @Test
    fun `stateHistory records transitions`() {
        stateMachine.startCheck()
        stateMachine.setUpToDate()

        val history = stateMachine.stateHistory
        assertEquals(2, history.size)
        assertEquals(UpdateFlowState.Idle, history[0].fromState)
        assertTrue(history[0].toState is UpdateFlowState.Checking)
        assertTrue(history[1].fromState is UpdateFlowState.Checking)
        assertTrue(history[1].toState is UpdateFlowState.UpToDate)
    }

    @Test
    fun `stateHistory is bounded to maxHistorySize`() {
        val boundedMachine = UpdateStateMachine(
            onStateChange = {},
            maxHistorySize = 2
        )

        boundedMachine.startCheck()
        boundedMachine.setUpdateAvailable("1.0", 1, "Notes", "url")
        boundedMachine.startDownload()

        val history = boundedMachine.stateHistory
        assertEquals(2, history.size)
        // Oldest entry (Idle -> Checking) should be removed
        assertTrue(history[0].fromState is UpdateFlowState.Checking)
    }

    @Test
    fun `clearHistory removes all entries`() {
        stateMachine.startCheck()
        stateMachine.setUpToDate()

        stateMachine.clearHistory()

        assertTrue(stateMachine.stateHistory.isEmpty())
    }

    @Test
    fun `stateHistory includes timestamp`() {
        val beforeTime = System.currentTimeMillis()
        stateMachine.startCheck()
        val afterTime = System.currentTimeMillis()

        val history = stateMachine.stateHistory
        assertTrue(history[0].timestamp in beforeTime..afterTime)
    }
}
