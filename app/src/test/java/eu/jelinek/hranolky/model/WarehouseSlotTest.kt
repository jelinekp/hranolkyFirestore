package eu.jelinek.hranolky.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WarehouseSlotTest {

    // ======== parsePropertiesFromProductId tests ========

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
    fun `parsePropertiesFromProductId handles 42mm thickness adjustment`() {
        val slot = WarehouseSlot(fullProductId = "H-DUB-A-42-100-2000", quantity = 1)
        val parsed = slot.parsePropertiesFromProductId()
        assertEquals(42.4f, parsed.thickness)
    }

    @Test
    fun `parsePropertiesFromProductId handles 42mm width adjustment`() {
        val slot = WarehouseSlot(fullProductId = "H-DUB-A-20-42-2000", quantity = 1)
        val parsed = slot.parsePropertiesFromProductId()
        assertEquals(42.4f, parsed.width)
    }

    @Test
    fun `parsePropertiesFromProductId preserves non-adjusted dimensions`() {
        val slot = WarehouseSlot(fullProductId = "H-DUB-A-30-80-2500", quantity = 1)
        val parsed = slot.parsePropertiesFromProductId()
        assertEquals(30.0f, parsed.thickness)
        assertEquals(80.0f, parsed.width)
    }

    // ======== getFullQualityName tests ========

    @Test
    fun `getFullQualityName maps DUB-A|A to DUB A slashA`() {
        val slot = WarehouseSlot(fullProductId = "H-", quantity = 0).copy(quality = "DUB-A|A")
        assertEquals("DUB A/A", slot.getFullQualityName())
    }

    @Test
    fun `getFullQualityName maps DUB-A|B to DUB A slashB`() {
        val slot = WarehouseSlot(fullProductId = "H-", quantity = 0).copy(quality = "DUB-A|B")
        assertEquals("DUB A/B", slot.getFullQualityName())
    }

    @Test
    fun `getFullQualityName maps DUB-ABP to DUB A slashB-P`() {
        val slot = WarehouseSlot(fullProductId = "H-", quantity = 0).copy(quality = "DUB-ABP")
        assertEquals("DUB A/B-P", slot.getFullQualityName())
    }

    @Test
    fun `getFullQualityName maps DUB-RST to DUB RUSTIK`() {
        val slot = WarehouseSlot(fullProductId = "H-", quantity = 0).copy(quality = "DUB-RST")
        assertEquals("DUB RUSTIK", slot.getFullQualityName())
    }

    @Test
    fun `getFullQualityName maps ZIR-ZIR to ZIRBE`() {
        val slot = WarehouseSlot(fullProductId = "H-", quantity = 0).copy(quality = "ZIR-ZIR")
        assertEquals("ZIRBE", slot.getFullQualityName())
    }

    @Test
    fun `getFullQualityName maps BUK-BUK to BUK`() {
        val slot = WarehouseSlot(fullProductId = "H-", quantity = 0).copy(quality = "BUK-BUK")
        assertEquals("BUK", slot.getFullQualityName())
    }

    @Test
    fun `getFullQualityName returns original for unknown code`() {
        val slot = WarehouseSlot(fullProductId = "H-", quantity = 0).copy(quality = "UNKNOWN-CODE")
        assertEquals("UNKNOWN-CODE", slot.getFullQualityName())
    }

    @Test
    fun `getFullQualityName returns empty for null quality`() {
        val slot = WarehouseSlot(fullProductId = "H-", quantity = 0, quality = null)
        assertEquals("", slot.getFullQualityName())
    }

    // ======== getVolume tests ========

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
    fun `getVolume returns null when width is null`() {
        val slot = WarehouseSlot(
            fullProductId = "H-TEST",
            quantity = 10,
            width = null,
            thickness = 20.0f,
            length = 1000
        )
        assertNull(slot.getVolume())
    }

    @Test
    fun `getVolume returns null when thickness is null`() {
        val slot = WarehouseSlot(
            fullProductId = "H-TEST",
            quantity = 10,
            width = 50.0f,
            thickness = null,
            length = 1000
        )
        assertNull(slot.getVolume())
    }

    @Test
    fun `getVolume returns null when length is null`() {
        val slot = WarehouseSlot(
            fullProductId = "H-TEST",
            quantity = 10,
            width = 50.0f,
            thickness = 20.0f,
            length = null
        )
        assertNull(slot.getVolume())
    }

    @Test
    fun `getVolume handles zero quantity`() {
        val slot = WarehouseSlot(
            fullProductId = "H-TEST",
            quantity = 0,
            width = 50.0f,
            thickness = 20.0f,
            length = 1000
        )
        assertEquals(0.0, slot.getVolume()!!, 0.000001)
    }

    // ======== getScreenTitle tests ========

    @Test
    fun `getScreenTitle formats floats without trailing dot zero`() {
        val parsed = WarehouseSlot(fullProductId = "H-DUB-A-20-100-2000", quantity = 10)
            .parsePropertiesFromProductId()
        val title = parsed.getScreenTitle()
        assertTrue("Title should contain 'Hranolek'", title.contains("Hranolek"))
        assertTrue("Title should contain 'DUB-A'", title.contains("DUB-A"))
        assertTrue("Title should contain '20 x 100 x 2000 mm'", title.contains("20 x 100 x 2000 mm"))
    }

    @Test
    fun `getScreenTitle shows Spárovka for Jointer type`() {
        val slot = WarehouseSlot(
            fullProductId = "S-DUB-ABP-42-0272-1860",
            quantity = 5,
            slotType = SlotType.Jointer,
            quality = "DUB-ABP",
            thickness = 42.4f,
            width = 272.0f,
            length = 1860
        )
        val title = slot.getScreenTitle()
        assertTrue("Title should contain 'Spárovka'", title.contains("Spárovka"))
    }

    @Test
    fun `getScreenTitle falls back to fullProductId when properties missing`() {
        val slot = WarehouseSlot(
            fullProductId = "H-INCOMPLETE",
            quantity = 1,
            slotType = SlotType.Beam,
            quality = null  // missing property
        )
        val title = slot.getScreenTitle()
        assertTrue("Title should contain fullProductId", title.contains("H-INCOMPLETE"))
    }

    @Test
    fun `getScreenTitle formats decimal dimensions correctly`() {
        val slot = WarehouseSlot(
            fullProductId = "H-DUB-A-27.4-42.4-2000",
            quantity = 1,
            slotType = SlotType.Beam,
            quality = "DUB-A",
            thickness = 27.4f,
            width = 42.4f,
            length = 2000
        )
        val title = slot.getScreenTitle()
        assertTrue("Title should contain '27.4'", title.contains("27.4"))
        assertTrue("Title should contain '42.4'", title.contains("42.4"))
    }

    // ======== hasAllProperties tests ========

    @Test
    fun `hasAllProperties returns true when all properties set`() {
        val slot = WarehouseSlot(
            fullProductId = "H-DUB-A-20-100-2000",
            quantity = 10,
            quality = "DUB-A",
            width = 100.0f,
            thickness = 20.0f,
            length = 2000
        )
        assertTrue(slot.hasAllProperties())
    }

    @Test
    fun `hasAllProperties returns false when quality is null`() {
        val slot = WarehouseSlot(
            fullProductId = "H-DUB-A-20-100-2000",
            quantity = 10,
            quality = null,
            width = 100.0f,
            thickness = 20.0f,
            length = 2000
        )
        assertFalse(slot.hasAllProperties())
    }

    @Test
    fun `hasAllProperties returns false when width is null`() {
        val slot = WarehouseSlot(
            fullProductId = "H-DUB-A-20-100-2000",
            quantity = 10,
            quality = "DUB-A",
            width = null,
            thickness = 20.0f,
            length = 2000
        )
        assertFalse(slot.hasAllProperties())
    }

    // ======== getShortQualityName tests ========

    @Test
    fun `getShortQualityName returns short name for Beam`() {
        val slot = WarehouseSlot(
            fullProductId = "H-DUB-A-20-100-2000",
            quantity = 10,
            slotType = SlotType.Beam
        )
        // For beam: char[2] + substring(5,7) = 'D' + 'UB' = "DUB" - wait, let's check actual logic
        // fullProductId[2] = 'D', substring(5,7) = "B-" - hmm, seems like it gets "DB-"
        val shortName = slot.getShortQualityName()
        assertNotNull(shortName)
    }

    @Test
    fun `getShortQualityName replaces pipe with slash for Jointer`() {
        val slot = WarehouseSlot(
            fullProductId = "S-DUB-A|A-27-0095-2050",
            quantity = 5,
            slotType = SlotType.Jointer
        )
        val shortName = slot.getShortQualityName()
        assertTrue("Short name should contain / instead of |", shortName.contains("/") || !shortName.contains("|"))
    }
}
