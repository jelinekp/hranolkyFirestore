package eu.jelinek.hranolky.domain.navigation

import eu.jelinek.hranolky.domain.InputValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManageItemNavigationCoordinatorTest {

    private lateinit var inputValidator: InputValidator
    private lateinit var coordinator: ManageItemNavigationCoordinator

    @Before
    fun setup() {
        inputValidator = InputValidator()
        coordinator = ManageItemNavigationCoordinator(inputValidator)
    }

    @Test
    fun `empty input returns ContinueWithInput`() {
        val result = coordinator.checkForItemCodeNavigation("", "DUB-A-20-40-1000")

        assertTrue(result is NavigationResult.ContinueWithInput)
    }

    @Test
    fun `numeric input returns ContinueWithInput`() {
        val result = coordinator.checkForItemCodeNavigation("123", "DUB-A-20-40-1000")

        assertTrue(result is NavigationResult.ContinueWithInput)
    }

    @Test
    fun `negative numeric input returns ContinueWithInput`() {
        val result = coordinator.checkForItemCodeNavigation("-50", "DUB-A-20-40-1000")

        assertTrue(result is NavigationResult.ContinueWithInput)
    }

    @Test
    fun `different item code returns NavigateToItem`() {
        val result = coordinator.checkForItemCodeNavigation(
            "H-BUK-B-30-50-2000",
            "DUB-A-20-40-1000"
        )

        assertTrue(result is NavigationResult.NavigateToItem)
        assertEquals("H-BUK-B-30-50-2000", (result as NavigationResult.NavigateToItem).itemCode)
    }

    @Test
    fun `same item code returns ContinueWithInput`() {
        val result = coordinator.checkForItemCodeNavigation(
            "H-DUB-A-20-40-1000",
            "DUB-A-20-40-1000"
        )

        assertTrue(result is NavigationResult.ContinueWithInput)
    }

    @Test
    fun `same item code with H prefix match returns ContinueWithInput`() {
        val result = coordinator.checkForItemCodeNavigation(
            "DUB-A-20-40-1000", // Will get H- prefix added
            "H-DUB-A-20-40-1000"
        )

        assertTrue(result is NavigationResult.ContinueWithInput)
    }

    @Test
    fun `S prefix item code triggers navigation`() {
        val result = coordinator.checkForItemCodeNavigation(
            "S-DUB-ABP-27-0042-3000",
            "DUB-A-20-40-1000"
        )

        assertTrue(result is NavigationResult.NavigateToItem)
    }

    @Test
    fun `invalid format returns ContinueWithInput`() {
        val result = coordinator.checkForItemCodeNavigation(
            "invalid-code",
            "DUB-A-20-40-1000"
        )

        assertTrue(result is NavigationResult.ContinueWithInput)
    }

    @Test
    fun `whitespace is trimmed`() {
        val result = coordinator.checkForItemCodeNavigation(
            "  H-BUK-B-30-50-2000  ",
            "DUB-A-20-40-1000"
        )

        assertTrue(result is NavigationResult.NavigateToItem)
        assertEquals("H-BUK-B-30-50-2000", (result as NavigationResult.NavigateToItem).itemCode)
    }

    @Test
    fun `navigateToItem completes successfully`() = runTest {
        // Just verify that navigateToItem doesn't throw an exception
        // Flow collection is tested via integration in actual usage
        coordinator.navigateToItem("H-DUB-A-20-40-1000")

        // If we get here, the emit succeeded without subscribers (which is OK for SharedFlow)
    }

    @Test
    fun `null currentSlotId handles different item`() {
        val result = coordinator.checkForItemCodeNavigation(
            "H-DUB-A-20-40-1000",
            null
        )

        assertTrue(result is NavigationResult.NavigateToItem)
    }

    @Test
    fun `16 char code without prefix gets H prefix added`() {
        // A valid 16-char beam code without prefix
        val result = coordinator.checkForItemCodeNavigation(
            "DUB-A-20-40-1000",
            "BUK-B-30-50-2000"
        )

        assertTrue(result is NavigationResult.NavigateToItem)
        // The code should have H- prefix added
        assertEquals("H-DUB-A-20-40-1000", (result as NavigationResult.NavigateToItem).itemCode)
    }
}
