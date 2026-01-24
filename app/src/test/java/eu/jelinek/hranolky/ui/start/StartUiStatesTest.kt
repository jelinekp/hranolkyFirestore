package eu.jelinek.hranolky.ui.start

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for StartScreen UI state classes following SoS principle.
 */
class StartUiStatesTest {

    @Test
    fun `ScannedCodeState default values are correct`() {
        val state = ScannedCodeState()
        assertEquals("", state.code)
        assertFalse(state.isFormatError)
    }

    @Test
    fun `DeviceInfoState default values are correct`() {
        val state = DeviceInfoState()
        assertEquals("", state.shortenedDeviceId)
        assertNull(state.deviceName)
        assertEquals("", state.appVersion)
        assertEquals(-1, state.appVersionCode)
    }

    @Test
    fun `InventoryCheckState default values are correct`() {
        val state = InventoryCheckState()
        assertFalse(state.isPermitted)
        assertFalse(state.isEnabled)
    }

    @Test
    fun `SignInProcessState default values are correct`() {
        val state = SignInProcessState()
        assertFalse(state.isInProgress)
        assertFalse(state.hasError)
        assertFalse(state.isProblem)
    }

    @Test
    fun `CompositeStartUiState default values are correct`() {
        val state = CompositeStartUiState()
        assertEquals(ScannedCodeState(), state.scannedCode)
        assertEquals(DeviceInfoState(), state.deviceInfo)
        assertEquals(InventoryCheckState(), state.inventoryCheck)
        assertEquals(SignInProcessState(), state.signInProcess)
    }

    @Test
    fun `CompositeStartUiState toLegacyState converts correctly`() {
        val composite = CompositeStartUiState(
            scannedCode = ScannedCodeState(code = "H-DUB-A-20-100-2000", isFormatError = false),
            deviceInfo = DeviceInfoState(
                shortenedDeviceId = "ABC123",
                deviceName = "Test Device",
                appVersion = "1.2.3",
                appVersionCode = 10
            ),
            inventoryCheck = InventoryCheckState(isPermitted = true, isEnabled = true),
            signInProcess = SignInProcessState(isInProgress = false, hasError = false, isProblem = false)
        )

        val legacy = composite.toLegacyState()

        assertEquals("H-DUB-A-20-100-2000", legacy.scannedCode)
        assertEquals("ABC123", legacy.shortenedDeviceId)
        assertEquals("Test Device", legacy.deviceName)
        assertEquals("1.2.3", legacy.appVersion)
        assertEquals(10, legacy.appVersionCode)
        assertTrue(legacy.isInventoryCheckPermitted)
        assertTrue(legacy.isInventoryCheckEnabled)
        assertFalse(legacy.isSigningIn)
        assertFalse(legacy.isSignInProblem)
        assertFalse(legacy.isFormatError)
        assertFalse(legacy.isSignInError)
    }

    @Test
    fun `CompositeStartUiState fromLegacyState converts correctly`() {
        val legacy = StartUiState(
            scannedCode = "S-BUK-C-27-42-3000",
            shortenedDeviceId = "XYZ789",
            deviceName = "Warehouse Scanner",
            appVersion = "2.0.0",
            appVersionCode = 20,
            isInventoryCheckPermitted = true,
            isInventoryCheckEnabled = false,
            isSigningIn = true,
            isSignInProblem = false,
            isFormatError = true,
            isSignInError = false
        )

        val composite = CompositeStartUiState.fromLegacyState(legacy)

        assertEquals("S-BUK-C-27-42-3000", composite.scannedCode.code)
        assertTrue(composite.scannedCode.isFormatError)
        assertEquals("XYZ789", composite.deviceInfo.shortenedDeviceId)
        assertEquals("Warehouse Scanner", composite.deviceInfo.deviceName)
        assertEquals("2.0.0", composite.deviceInfo.appVersion)
        assertEquals(20, composite.deviceInfo.appVersionCode)
        assertTrue(composite.inventoryCheck.isPermitted)
        assertFalse(composite.inventoryCheck.isEnabled)
        assertTrue(composite.signInProcess.isInProgress)
        assertFalse(composite.signInProcess.isProblem)
        assertFalse(composite.signInProcess.hasError)
    }

    @Test
    fun `roundtrip conversion preserves state`() {
        val original = StartUiState(
            scannedCode = "TEST-CODE",
            shortenedDeviceId = "DEV123",
            deviceName = "Test",
            appVersion = "1.0.0",
            appVersionCode = 5,
            isInventoryCheckPermitted = true,
            isInventoryCheckEnabled = true,
            isSigningIn = false,
            isSignInProblem = true,
            isFormatError = false,
            isSignInError = true
        )

        val composite = CompositeStartUiState.fromLegacyState(original)
        val roundTripped = composite.toLegacyState()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `individual states can be updated independently`() {
        val state = CompositeStartUiState()

        // Update only scanned code
        val withCode = state.copy(
            scannedCode = state.scannedCode.copy(code = "NEW-CODE")
        )
        assertEquals("NEW-CODE", withCode.scannedCode.code)
        assertEquals(state.deviceInfo, withCode.deviceInfo)
        assertEquals(state.inventoryCheck, withCode.inventoryCheck)
        assertEquals(state.signInProcess, withCode.signInProcess)

        // Update only sign-in process
        val withSignIn = state.copy(
            signInProcess = state.signInProcess.copy(isInProgress = true)
        )
        assertTrue(withSignIn.signInProcess.isInProgress)
        assertEquals(state.scannedCode, withSignIn.scannedCode)
        assertEquals(state.deviceInfo, withSignIn.deviceInfo)
        assertEquals(state.inventoryCheck, withSignIn.inventoryCheck)
    }
}
