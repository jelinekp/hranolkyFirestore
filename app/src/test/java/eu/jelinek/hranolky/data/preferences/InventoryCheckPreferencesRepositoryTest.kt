package eu.jelinek.hranolky.data.preferences

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InventoryCheckPreferencesRepositoryTest {

    private lateinit var repository: InventoryCheckPreferencesRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        repository = InventoryCheckPreferencesRepositoryImpl(context)
    }

    @Test
    fun `isInventoryCheckEnabled returns false by default`() {
        // Given fresh preferences
        // When checking enabled state
        val enabled = repository.isInventoryCheckEnabled()

        // Then should return false
        assertFalse(enabled)
    }

    @Test
    fun `setInventoryCheckEnabled true persists value`() {
        // Given repository
        // When enabling inventory check
        repository.setInventoryCheckEnabled(true)

        // Then should be enabled
        assertTrue(repository.isInventoryCheckEnabled())
    }

    @Test
    fun `setInventoryCheckEnabled false after true disables it`() {
        // Given enabled inventory check
        repository.setInventoryCheckEnabled(true)

        // When disabling
        repository.setInventoryCheckEnabled(false)

        // Then should be disabled
        assertFalse(repository.isInventoryCheckEnabled())
    }

    @Test
    fun `value persists across repository instances`() {
        // Given enabled inventory check
        repository.setInventoryCheckEnabled(true)

        // When creating new repository instance
        val newRepository = InventoryCheckPreferencesRepositoryImpl(context)

        // Then value should persist
        assertTrue(newRepository.isInventoryCheckEnabled())
    }
}
