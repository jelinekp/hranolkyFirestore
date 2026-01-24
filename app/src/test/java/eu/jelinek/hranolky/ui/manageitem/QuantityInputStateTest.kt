package eu.jelinek.hranolky.ui.manageitem

import eu.jelinek.hranolky.domain.QuantityParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuantityInputStateTest {

    private lateinit var quantityParser: QuantityParser
    private lateinit var state: QuantityInputState

    @Before
    fun setup() {
        quantityParser = QuantityParser()
        state = QuantityInputState(quantityParser)
    }

    @Test
    fun `initial state is empty`() {
        assertTrue(state.isEmpty)
        assertEquals("", state.quantity)
    }

    @Test
    fun `onQuantityChanged updates quantity`() {
        state.onQuantityChanged("123")

        assertEquals("123", state.quantity)
    }

    @Test
    fun `parsedQuantity returns value for valid input`() {
        state.onQuantityChanged("42")

        assertEquals(42L, state.parsedQuantity)
    }

    @Test
    fun `parsedQuantity returns null for invalid input`() {
        state.onQuantityChanged("abc")

        assertNull(state.parsedQuantity)
    }

    @Test
    fun `parsedQuantity handles negative numbers as invalid`() {
        state.onQuantityChanged("-10")

        // Negative numbers are not valid quantities
        assertNull(state.parsedQuantity)
        assertFalse(state.isValid)
    }

    @Test
    fun `isValid returns true for empty input`() {
        assertTrue(state.isValid)
    }

    @Test
    fun `isValid returns true for valid numeric input`() {
        state.onQuantityChanged("100")

        assertTrue(state.isValid)
    }

    @Test
    fun `isValid returns false for invalid input`() {
        state.onQuantityChanged("not-a-number")

        assertFalse(state.isValid)
    }

    @Test
    fun `hasParseError returns false for empty input`() {
        assertFalse(state.hasParseError)
    }

    @Test
    fun `hasParseError returns false for valid input`() {
        state.onQuantityChanged("123")

        assertFalse(state.hasParseError)
    }

    @Test
    fun `hasParseError returns true for invalid non-empty input`() {
        state.onQuantityChanged("invalid")

        assertTrue(state.hasParseError)
    }

    @Test
    fun `clear resets quantity to empty`() {
        state.onQuantityChanged("123")
        state.clear()

        assertTrue(state.isEmpty)
        assertEquals("", state.quantity)
    }

    @Test
    fun `requireParsedQuantity returns value when valid`() {
        state.onQuantityChanged("50")

        assertEquals(50L, state.requireParsedQuantity())
    }

    @Test(expected = IllegalStateException::class)
    fun `requireParsedQuantity throws when invalid`() {
        state.onQuantityChanged("invalid")

        state.requireParsedQuantity()
    }

    @Test
    fun `isEmpty returns false after input`() {
        state.onQuantityChanged("1")

        assertFalse(state.isEmpty)
    }
}
