package eu.jelinek.hranolky.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SlotTypeTest {

    @Test
    fun `toString should return correct string for Beam`() {
        assertEquals("H", SlotType.Beam.toString())
    }

    @Test
    fun `toString should return correct string for Jointer`() {
        assertEquals("S", SlotType.Jointer.toString())
    }
}
