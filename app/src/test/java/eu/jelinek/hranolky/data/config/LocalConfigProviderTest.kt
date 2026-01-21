package eu.jelinek.hranolky.data.config

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LocalConfigProviderTest {

    private val provider = LocalConfigProvider()

    @Test
    fun `getQualityMappings returns default mappings`() = runBlocking {
        val mappings = provider.getQualityMappings()

        assertEquals(LocalConfigDefaults.qualityMappings, mappings)
        assertEquals("DUB A/A", mappings["DUB-A|A"])
    }

    @Test
    fun `getDimensionAdjustments returns default adjustments`() = runBlocking {
        val adjustments = provider.getDimensionAdjustments()

        assertEquals(LocalConfigDefaults.dimensionAdjustments, adjustments)
        assertEquals(27.4f, adjustments[27.0f])
    }

    @Test
    fun `getInventoryCheckPeriodDays returns default period`() = runBlocking {
        val period = provider.getInventoryCheckPeriodDays()

        assertEquals(LocalConfigDefaults.inventoryCheckPeriodDays, period)
        assertEquals(75, period)
    }

    @Test
    fun `getCollectionConfig returns default config`() = runBlocking {
        val config = provider.getCollectionConfig()

        assertEquals(LocalConfigDefaults.collectionConfig, config)
        assertEquals("Hranolky", config.beamCollection)
        assertEquals("Sparovky", config.jointerCollection)
    }

    @Test
    fun `isRemoteConfigLoaded always returns false`() {
        assertFalse(provider.isRemoteConfigLoaded())
    }

    @Test
    fun `invalidateCache does nothing`() {
        // Should not throw
        provider.invalidateCache()

        // Still returns defaults
        runBlocking {
            assertEquals(LocalConfigDefaults.qualityMappings, provider.getQualityMappings())
        }
    }
}
