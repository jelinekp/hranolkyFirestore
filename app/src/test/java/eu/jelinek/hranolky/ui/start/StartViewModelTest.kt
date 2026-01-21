package eu.jelinek.hranolky.ui.start

import android.app.Application
import eu.jelinek.hranolky.domain.AuthManager
import eu.jelinek.hranolky.domain.AuthState
import eu.jelinek.hranolky.domain.DeviceManager
import eu.jelinek.hranolky.domain.InputValidator
import eu.jelinek.hranolky.domain.UpdateManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertEquals
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

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onScannedCodeChange emits navigate when valid`() = runTest(testDispatcher) {
        val app = mockk<Application>(relaxed = true)

        // Create auth state flow - user not signed in to avoid triggering initializeAppData
        val authStateFlow = MutableStateFlow(AuthState(isSignedIn = false, isLoading = false))
        val authManager = mockk<AuthManager>(relaxed = true) {
            every { isSignedIn } returns false
            every { authState } returns authStateFlow
        }

        val validator = mockk<InputValidator> {
            every { isValidScannedTextFormat(any()) } returns true
            every { manipulateAndValidateItemCode(any()) } answers { firstArg() }
        }
        val updateManager = mockk<UpdateManager>(relaxed = true) {
            every { updateState } returns MutableStateFlow(eu.jelinek.hranolky.domain.UpdateState())
        }
        val deviceManager = mockk<DeviceManager>(relaxed = true)

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
}
