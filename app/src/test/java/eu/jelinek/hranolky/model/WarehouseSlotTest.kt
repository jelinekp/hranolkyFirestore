package eu.jelinek.hranolky.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WarehouseSlotTest {


    @Test
    fun `parsePropertiesFromProductId should correctly parse H-prefixed product ID`() {
        val slot = WarehouseSlot(productId = "H-DUB-A-20-100-2000", quantity = 1)
        val parsedSlot = slot.parsePropertiesFromProductId()

        assertEquals(SlotType.Beam, parsedSlot.slotType)
        assertEquals("DUB-A", parsedSlot.quality)
        assertEquals(20.0f, parsedSlot.thickness)
        assertEquals(100.0f, parsedSlot.width)
        assertEquals(2000, parsedSlot.length)
    }

    @Test
    fun `parsePropertiesFromProductId should correctly parse S-prefixed product ID`() {
        val slot = WarehouseSlot(productId = "S-DUB-R-27-42-3000", quantity = 1)

        println("before:")
        println(slot)

        val parsedSlot = slot.parsePropertiesFromProductId()

        println("after:")
        println(parsedSlot)

        assertEquals(SlotType.Jointer, parsedSlot.slotType)
        assertEquals("DUB-R", parsedSlot.quality)
        assertEquals(27.4f, parsedSlot.thickness) // Specific conversion
        assertEquals(42.4f, parsedSlot.width) // Specific conversion
        assertEquals(3000, parsedSlot.length)
    }

    @Test
    fun `parsePropertiesFromProductId should handle product ID without prefix`() {
        val slot = WarehouseSlot(productId = "BUK-C-30-50-1500", quantity = 1)
        val parsedSlot = slot.parsePropertiesFromProductId()

        assertEquals(SlotType.Beam, parsedSlot.slotType) // Default to Beam
        assertEquals("BUK-C", parsedSlot.quality)
        assertEquals(30.0f, parsedSlot.thickness)
        assertEquals(50.0f, parsedSlot.width)
        assertEquals(1500, parsedSlot.length)
    }

    @Test
    fun `parsePropertiesFromProductId should handle specific thickness conversion`() {
        val slot = WarehouseSlot(productId = "H-000-0-20-100-1000", quantity = 1)
        val parsedSlot = slot.parsePropertiesFromProductId()
        assertEquals(20.0f, parsedSlot.thickness)

        val slot2 = WarehouseSlot(productId = "H-000-0-27-100-1000", quantity = 1)
        val parsedSlot2 = slot2.parsePropertiesFromProductId()
        assertEquals(27.4f, parsedSlot2.thickness)

        val slot3 = WarehouseSlot(productId = "H-000-0-42-100-1000", quantity = 1)
        val parsedSlot3 = slot3.parsePropertiesFromProductId()
        assertEquals(42.4f, parsedSlot3.thickness)
    }

    @Test
    fun `parsePropertiesFromProductId should handle specific width conversion`() {
        val slot = WarehouseSlot(productId = "H-000-0-100-42-1000", quantity = 1)
        val parsedSlot = slot.parsePropertiesFromProductId()
        assertEquals(42.4f, parsedSlot.width)

        val slot2 = WarehouseSlot(productId = "H-000-0-100-50-1000", quantity = 1)
        val parsedSlot2 = slot2.parsePropertiesFromProductId()
        assertEquals(50.0f, parsedSlot2.width)
    }

    @Test
    fun `parsePropertiesFromProductId should return original slot if parts size is less than 5`() {
        val originalSlot = WarehouseSlot(productId = "H-123-45", quantity = 1, quality = "original", width = 10f)
        val parsedSlot = originalSlot.parsePropertiesFromProductId()

        // Expecting the original slot to be returned if parsing fails due to insufficient parts
        assertEquals(originalSlot, parsedSlot)
    }

    @Test
    fun `getVolume should return correct volume for valid properties`() {
        val slot = WarehouseSlot(
            productId = "test",
            quantity = 2,
            width = 100f,
            length = 2000,
            thickness = 50f
        )
        val expectedVolume = (2 * 2000 * 50.0 * 100.0) / 1_000_000_000.0
        assertEquals(expectedVolume, slot.getVolume()!!, 0.000000001) // Delta for float comparison
    }

    @Test
    fun `getVolume should return null if width is null`() {
        val slot = WarehouseSlot(
            productId = "test",
            quantity = 2,
            width = null,
            length = 2000,
            thickness = 50f
        )
        assertEquals(null, slot.getVolume())
    }

    @Test
    fun `getVolume should return null if length is null`() {
        val slot = WarehouseSlot(
            productId = "test",
            quantity = 2,
            width = 100f,
            length = null,
            thickness = 50f
        )
        assertEquals(null, slot.getVolume())
    }

    @Test
    fun `getVolume should return null if thickness is null`() {
        val slot = WarehouseSlot(
            productId = "test",
            quantity = 2,
            width = 100f,
            length = 2000,
            thickness = null
        )
        assertEquals(null, slot.getVolume())
    }

    @Test
    fun `hasAllProperties should return true when all properties are present`() {
        val slot = WarehouseSlot(
            productId = "test",
            quantity = 1,
            quality = "good",
            width = 10f,
            thickness = 5f,
            length = 100
        )
        assertEquals(true, slot.hasAllProperties())
    }

    @Test
    fun `hasAllProperties should return false if quality is null`() {
        val slot = WarehouseSlot(
            productId = "test",
            quantity = 1,
            quality = null,
            width = 10f,
            thickness = 5f,
            length = 100
        )
        assertEquals(false, slot.hasAllProperties())
    }

    @Test
    fun `hasAllProperties should return false if width is null`() {
        val slot = WarehouseSlot(
            productId = "test",
            quantity = 1,
            quality = "good",
            width = null,
            thickness = 5f,
            length = 100
        )
        assertEquals(false, slot.hasAllProperties())
    }

    @Test
    fun `hasAllProperties should return false if thickness is null`() {
        val slot = WarehouseSlot(
            productId = "test",
            quantity = 1,
            quality = "good",
            width = 10f,
            thickness = null,
            length = 100
        )
        assertEquals(false, slot.hasAllProperties())
    }

    @Test
    fun `hasAllProperties should return false if length is null`() {
        val slot = WarehouseSlot(
            productId = "test",
            quantity = 1,
            quality = "good",
            width = 10f,
            thickness = 5f,
            length = null
        )
        assertEquals(false, slot.hasAllProperties())
    }

    @After
    fun tearDown() {
        stopKoin()
    }
}
