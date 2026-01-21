package eu.jelinek.hranolky.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WarehouseSlotTest {

    @Test
    fun `parsePropertiesFromProductId parses H- prefixed beam`() {
        val slot = WarehouseSlot(fullProductId = "H-DUB-A-20-100-2000", quantity = 10, lastModified = Timestamp.now())
        val parsed = slot.parsePropertiesFromProductId()
        assertEquals(SlotType.Beam, parsed.slotType)
        assertEquals("DUB-A", parsed.quality)
        assertEquals(20.0f, parsed.thickness)
        assertEquals(100.0f, parsed.width)
        assertEquals(2000, parsed.length)
        assertEquals(slot.fullProductId, parsed.fullProductId)
        assertEquals(slot.quantity, parsed.quantity)
        assertEquals(slot.lastModified, parsed.lastModified)
    }

    @Test
    fun `parsePropertiesFromProductId parses S- prefixed jointer with point4 replacements`() {
        val slot = WarehouseSlot(fullProductId = "S-DUB-R-27-42-3000", quantity = 5)
        val parsed = slot.parsePropertiesFromProductId()
        assertEquals(SlotType.Jointer, parsed.slotType)
        assertEquals("DUB-R", parsed.quality)
        assertEquals(27.4f, parsed.thickness)
        assertEquals(42.4f, parsed.width)
        assertEquals(3000, parsed.length)
    }

    @Test
    fun `parsePropertiesFromProductId without prefix defaults to Beam`() {
        val slot = WarehouseSlot(fullProductId = "BUK-C-30-50-1500", quantity = 1)
        val parsed = slot.parsePropertiesFromProductId()
        assertEquals(SlotType.Beam, parsed.slotType)
        assertEquals("BUK-C", parsed.quality)
        assertEquals(30.0f, parsed.thickness)
        assertEquals(50.0f, parsed.width)
        assertEquals(1500, parsed.length)
    }

    @Test
    fun `parsePropertiesFromProductId invalid format leaves fields null`() {
        val slot = WarehouseSlot(fullProductId = "INVALID-ID", quantity = 1)
        val parsed = slot.parsePropertiesFromProductId()
        assertNull(parsed.slotType)
        assertNull(parsed.quality)
        assertNull(parsed.thickness)
        assertNull(parsed.width)
        assertNull(parsed.length)
    }

    @Test
    fun `getFullQualityName maps known codes`() {
        val slot = WarehouseSlot(fullProductId = "H-", quantity = 0).copy(quality = "DUB-A|A")
        assertEquals("DUB A/A", slot.getFullQualityName())
    }

    @Test
    fun `getVolume computes cubic meters`() {
        val slot = WarehouseSlot(
            fullProductId = "H-TEST-20-50-1000",
            quantity = 10,
            width = 50.0f,
            thickness = 20.0f,
            length = 1000
        )
        // volume = (quantity * length * thickness * width) / 1_000_000_000
        // = (10 * 1000 * 20 * 50) / 1e9 = 10_000_000 / 1e9 = 0.01
        assertEquals(0.01, slot.getVolume()!!, 0.000001)
    }

    @Test
    fun `getScreenTitle formats floats without trailing dot zero`() {
        val parsed = WarehouseSlot(fullProductId = "H-DUB-A-20-100-2000", quantity = 10)
            .parsePropertiesFromProductId()
        val title = parsed.getScreenTitle()
        // Width 100.0 -> "100", thickness 20.0 -> "20"
        // Expect two-line title with mm suffix as implemented
        assert(title.contains("Hranolek")) { "Title should contain 'Hranolek', but was: $title" }
        assert(title.contains("DUB-A")) { "Title should contain 'DUB-A', but was: $title" }
        assert(title.contains("20 x 100 x 2000 mm")) { "Title should contain '20 x 100 x 2000 mm', but was: $title" }
    }
}
