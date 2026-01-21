package eu.jelinek.hranolky.ui.start

import android.app.Application
import eu.jelinek.hranolky.domain.AuthManager
import eu.jelinek.hranolky.domain.AuthState
import eu.jelinek.hranolky.domain.DeviceManager
import eu.jelinek.hranolky.domain.InputValidator
import eu.jelinek.hranolky.domain.UpdateManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StartViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var authManager: AuthManager
    private lateinit var authStateFlow: MutableStateFlow<AuthState>
    private lateinit var validator: InputValidator
    private lateinit var updateManager: UpdateManager
    private lateinit var deviceManager: DeviceManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        app = mockk(relaxed = true)

        // Create auth state flow - user not signed in to avoid triggering initializeAppData
        authStateFlow = MutableStateFlow(AuthState(isSignedIn = false, isLoading = false))
        authManager = mockk(relaxed = true) {
            every { isSignedIn } returns false
            every { authState } returns authStateFlow
        }

        validator = mockk {
            every { isValidScannedTextFormat(any()) } returns true
            every { manipulateAndValidateItemCode(any()) } answers { firstArg() }
        }

        updateManager = mockk(relaxed = true) {
            every { updateState } returns MutableStateFlow(eu.jelinek.hranolky.domain.UpdateState())
        }

        deviceManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ======== onScannedCodeChange tests ========

    @Test
    fun `onScannedCodeChange emits navigate when valid`() = runTest(testDispatcher) {
        val vm = StartViewModel(app, authManager, validator, updateManager, deviceManager)

        // Allow init coroutines to start
        testScheduler.runCurrent()

        // Trigger the action
        vm.onScannedCodeChange("H-TEST-20-40-1000", isManualInput = false)

        // Use first() with timeout to get the emitted value - this avoids infinite collection
        val result = withTimeout(1000) {
            vm.navigateToManageItem.first()
        }

        assertEquals("H-TEST-20-40-1000", result)
    }

    @Test
    fun `onScannedCodeChange does not emit for invalid code`() = runTest(testDispatcher) {
        every { validator.isValidScannedTextFormat(any()) } returns false
        every { validator.manipulateAndValidateItemCode(any()) } returns null

        val vm = StartViewModel(app, authManager, validator, updateManager, deviceManager)
        testScheduler.runCurrent()

        vm.onScannedCodeChange("INVALID", isManualInput = false)
        testScheduler.runCurrent()

        // State should have the scanned code but isFormatError is NOT set
        // (isFormatError is only set in onSubmit, not in onScannedCodeChange)
        val state = vm.startScreenState.value
        assertEquals("INVALID", state.scannedCode)
        assertFalse("isFormatError should NOT be set by onScannedCodeChange", state.isFormatError)
    }

    @Test
    fun `onSubmit sets format error on invalid input`() = runTest(testDispatcher) {
        every { validator.isValidScannedTextFormat(any()) } returns false
        every { validator.manipulateAndValidateItemCode(any()) } returns null

        val vm = StartViewModel(app, authManager, validator, updateManager, deviceManager)
        testScheduler.runCurrent()

        // First set the code
        vm.onScannedCodeChange("INVALID", isManualInput = true)
        testScheduler.runCurrent()

        // Then submit - this should set isFormatError
        vm.onSubmit()
        testScheduler.advanceUntilIdle()

        val state = vm.startScreenState.value
        assertTrue("isFormatError should be set after onSubmit", state.isFormatError)
    }

    @Test
    fun `onScannedCodeChange clears format error on valid input`() = runTest(testDispatcher) {
        val vm = StartViewModel(app, authManager, validator, updateManager, deviceManager)
        testScheduler.runCurrent()

        // First set an error state via onSubmit (only way to set isFormatError)
        every { validator.isValidScannedTextFormat(any()) } returns false
        every { validator.manipulateAndValidateItemCode(any()) } returns null
        vm.onScannedCodeChange("INVALID", isManualInput = true)
        testScheduler.runCurrent()
        vm.onSubmit()
        testScheduler.advanceUntilIdle()

        assertTrue("isFormatError should be set after onSubmit with invalid input",
            vm.startScreenState.value.isFormatError)

        // Now provide valid input - onScannedCodeChange clears isFormatError
        every { validator.isValidScannedTextFormat(any()) } returns true
        every { validator.manipulateAndValidateItemCode(any()) } answers { firstArg() }
        vm.onScannedCodeChange("H-BUK-C-30-50-2000", isManualInput = false)

        val result = withTimeout(1000) {
            vm.navigateToManageItem.first()
        }

        assertNotNull(result)
        assertFalse("isFormatError should be cleared after valid input",
            vm.startScreenState.value.isFormatError)
    }

    @Test
    fun `onScannedCodeChange uses manipulated code from validator`() = runTest(testDispatcher) {
        // Validator adds H- prefix to 16-char codes
        every { validator.manipulateAndValidateItemCode("DUB-A-20-100-2000") } returns "H-DUB-A-20-100-2000"

        val vm = StartViewModel(app, authManager, validator, updateManager, deviceManager)
        testScheduler.runCurrent()

        vm.onScannedCodeChange("DUB-A-20-100-2000", isManualInput = false)

        val result = withTimeout(1000) {
            vm.navigateToManageItem.first()
        }

        assertEquals("H-DUB-A-20-100-2000", result)
    }

    // ======== clearScannedCode tests ========

    @Test
    fun `clearScannedCode resets scanned code state`() = runTest(testDispatcher) {
        val vm = StartViewModel(app, authManager, validator, updateManager, deviceManager)
        testScheduler.runCurrent()

        // Set some state first
        vm.onScannedCodeChange("H-TEST-20-40-1000", isManualInput = true)
        testScheduler.runCurrent()

        vm.clearScannedCode()
        testScheduler.runCurrent()

        assertEquals("", vm.startScreenState.value.scannedCode)
    }

    // ======== Initial state tests ========

    @Test
    fun `initial state has empty scanned code`() = runTest(testDispatcher) {
        val vm = StartViewModel(app, authManager, validator, updateManager, deviceManager)
        testScheduler.runCurrent()

        assertEquals("", vm.startScreenState.value.scannedCode)
        assertFalse(vm.startScreenState.value.isFormatError)
    }
}
