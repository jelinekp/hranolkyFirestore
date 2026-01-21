package eu.jelinek.hranolky.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateStatesTest {

    // ======== UpdateAvailability tests ========

    @Test
    fun `UpdateAvailability Unknown is the default state`() {
        val state = CompositeUpdateState()
        assertTrue(state.availability is UpdateAvailability.Unknown)
    }

    @Test
    fun `UpdateAvailability Available contains version info`() {
        val available = UpdateAvailability.Available(
            version = "1.2.0",
            versionCode = 120,
            releaseNotes = "Bug fixes"
        )
        assertEquals("1.2.0", available.version)
        assertEquals(120, available.versionCode)
        assertEquals("Bug fixes", available.releaseNotes)
    }

    // ======== DownloadState tests ========

    @Test
    fun `DownloadState Idle is the default state`() {
        val state = CompositeUpdateState()
        assertTrue(state.download is DownloadState.Idle)
    }

    @Test
    fun `DownloadState Downloading tracks progress`() {
        val downloading = DownloadState.Downloading(progress = 50)
        assertEquals(50, downloading.progress)
    }

    @Test
    fun `DownloadState Paused contains reason`() {
        val paused = DownloadState.Paused(PauseReason.WAITING_FOR_WIFI)
        assertEquals(PauseReason.WAITING_FOR_WIFI, paused.reason)
    }

    @Test
    fun `DownloadState Failed contains error type`() {
        val failed = DownloadState.Failed(DownloadError.INSUFFICIENT_SPACE)
        assertEquals(DownloadError.INSUFFICIENT_SPACE, failed.error)
    }

    // ======== InstallationState tests ========

    @Test
    fun `InstallationState Idle is the default state`() {
        val state = CompositeUpdateState()
        assertTrue(state.installation is InstallationState.Idle)
    }

    @Test
    fun `InstallationState Failed contains error type`() {
        val failed = InstallationState.Failed(InstallationError.STORAGE)
        assertEquals(InstallationError.STORAGE, failed.error)
    }

    // ======== CompositeUpdateState conversion tests ========

    @Test
    fun `toLegacyState converts Unknown availability correctly`() {
        val composite = CompositeUpdateState(
            availability = UpdateAvailability.Unknown
        )
        val legacy = composite.toLegacyState()

        assertFalse(legacy.isUpdateAvailable)
        assertEquals("", legacy.latestVersion)
        assertEquals(0, legacy.latestVersionCode)
    }

    @Test
    fun `toLegacyState converts Available state correctly`() {
        val composite = CompositeUpdateState(
            availability = UpdateAvailability.Available(
                version = "2.0.0",
                versionCode = 200,
                releaseNotes = "New features"
            )
        )
        val legacy = composite.toLegacyState()

        assertTrue(legacy.isUpdateAvailable)
        assertEquals("2.0.0", legacy.latestVersion)
        assertEquals(200, legacy.latestVersionCode)
        assertEquals("New features", legacy.releaseNotes)
    }

    @Test
    fun `toLegacyState converts downloading state correctly`() {
        val composite = CompositeUpdateState(
            download = DownloadState.Downloading(75)
        )
        val legacy = composite.toLegacyState()

        assertTrue(legacy.isDownloading)
        assertEquals(75, legacy.downloadProgress)
    }

    @Test
    fun `toLegacyState converts completed download correctly`() {
        val composite = CompositeUpdateState(
            download = DownloadState.Completed
        )
        val legacy = composite.toLegacyState()

        assertFalse(legacy.isDownloading)
        assertEquals(100, legacy.downloadProgress)
    }

    @Test
    fun `toLegacyState converts installing state correctly`() {
        val composite = CompositeUpdateState(
            installation = InstallationState.Installing
        )
        val legacy = composite.toLegacyState()

        assertTrue(legacy.isInstalling)
    }

    @Test
    fun `toLegacyState converts error states correctly`() {
        val composite = CompositeUpdateState(
            availability = UpdateAvailability.CheckFailed("Network error")
        )
        val legacy = composite.toLegacyState()

        assertEquals("Network error", legacy.error)
    }

    @Test
    fun `toLegacyState converts justUpdated correctly`() {
        val composite = CompositeUpdateState(
            postUpdate = PostUpdateState(justUpdated = true)
        )
        val legacy = composite.toLegacyState()

        assertTrue(legacy.justUpdated)
    }

    // ======== fromLegacyState conversion tests ========

    @Test
    fun `fromLegacyState converts update available correctly`() {
        val legacy = UpdateState(
            isUpdateAvailable = true,
            latestVersion = "1.5.0",
            latestVersionCode = 150,
            releaseNotes = "Minor fixes"
        )
        val composite = CompositeUpdateState.fromLegacyState(legacy)

        assertTrue(composite.availability is UpdateAvailability.Available)
        val available = composite.availability as UpdateAvailability.Available
        assertEquals("1.5.0", available.version)
        assertEquals(150, available.versionCode)
        assertEquals("Minor fixes", available.releaseNotes)
    }

    @Test
    fun `fromLegacyState converts downloading correctly`() {
        val legacy = UpdateState(
            isDownloading = true,
            downloadProgress = 30
        )
        val composite = CompositeUpdateState.fromLegacyState(legacy)

        assertTrue(composite.download is DownloadState.Downloading)
        assertEquals(30, (composite.download as DownloadState.Downloading).progress)
    }

    @Test
    fun `fromLegacyState converts installing correctly`() {
        val legacy = UpdateState(
            isInstalling = true
        )
        val composite = CompositeUpdateState.fromLegacyState(legacy)

        assertTrue(composite.installation is InstallationState.Installing)
    }

    @Test
    fun `fromLegacyState converts justUpdated correctly`() {
        val legacy = UpdateState(
            justUpdated = true
        )
        val composite = CompositeUpdateState.fromLegacyState(legacy)

        assertTrue(composite.postUpdate.justUpdated)
    }
}
