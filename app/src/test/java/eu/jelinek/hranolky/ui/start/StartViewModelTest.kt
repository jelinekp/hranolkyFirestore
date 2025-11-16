package eu.jelinek.hranolky.ui.start

import android.app.Application
import eu.jelinek.hranolky.domain.DeviceManager
import eu.jelinek.hranolky.domain.InputValidator
import eu.jelinek.hranolky.domain.UpdateManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Test

class StartViewModelTest {

    @Test
    fun `onScannedCodeChange emits navigate when valid`() = runBlocking {
        val app = mockk<Application>(relaxed = true)
        val auth = mockk<com.google.firebase.auth.FirebaseAuth>(relaxed = true) {
            every { currentUser } returns mockk(relaxed = true)
        }
        val validator = mockk<InputValidator> {
            every { isValidScannedTextFormat(any()) } returns true
        }
        val updateManager = mockk<UpdateManager>(relaxed = true) {
            every { updateState } returns MutableStateFlow(eu.jelinek.hranolky.domain.UpdateState())
            coEvery { checkForUpdate(any(), any()) } returns Unit
        }
        val deviceManager = mockk<DeviceManager>(relaxed = true) {
            coEvery { fetchDeviceNameAndInventoryPermit(any(), any()) } answers { secondArg() }
            coEvery { logDeviceId(any(), any()) } answers { secondArg() }
        }

        val vm = StartViewModel(app, auth, validator, updateManager, deviceManager)

        vm.onScannedCodeChange("H-TEST-20-40-1000", isManualInput = false)
        val destination = vm.navigateToManageItem
        val emitted: String? = withTimeoutOrNull(1000) { destination.first() }
        assertEquals("H-TEST-20-40-1000", emitted)
    }
}
