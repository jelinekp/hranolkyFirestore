package eu.jelinek.hranolky.domain

import eu.jelinek.hranolky.model.ActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InputValidatorTest {

    private lateinit var validator: InputValidator

    @Before
    fun setup() {
        validator = InputValidator()
    }

    // ======== validateQuantity tests ========

    @Test
    fun `validateQuantity returns success for valid positive quantity on ADD`() {
        val result = validator.validateQuantity("10", 5, ActionType.ADD)
        assertTrue(result.isSuccess)
        assertEquals(10L, result.getOrNull())
    }

    @Test
    fun `validateQuantity returns success for valid positive quantity on REMOVE within stock`() {
        val result = validator.validateQuantity("3", 5, ActionType.REMOVE)
        assertTrue(result.isSuccess)
        assertEquals(3L, result.getOrNull())
    }

    @Test
    fun `validateQuantity returns failure for non-numeric input`() {
        val result = validator.validateQuantity("abc", 5, ActionType.ADD)
        assertTrue(result.isFailure)
        assertEquals("Neplatné množství", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateQuantity returns failure for negative quantity`() {
        val result = validator.validateQuantity("-5", 10, ActionType.ADD)
        assertTrue(result.isFailure)
        assertEquals("Množství musí být kladné číslo", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateQuantity returns failure for zero quantity on ADD`() {
        val result = validator.validateQuantity("0", 5, ActionType.ADD)
        assertTrue(result.isFailure)
        assertEquals("Pohyb nemůže být nulový", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateQuantity returns failure for zero quantity on REMOVE`() {
        val result = validator.validateQuantity("0", 5, ActionType.REMOVE)
        assertTrue(result.isFailure)
        assertEquals("Pohyb nemůže být nulový", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateQuantity allows zero quantity for INVENTORY_CHECK`() {
        val result = validator.validateQuantity("0", 5, ActionType.INVENTORY_CHECK)
        assertTrue(result.isSuccess)
        assertEquals(0L, result.getOrNull())
    }

    @Test
    fun `validateQuantity returns failure when REMOVE quantity exceeds stock`() {
        val result = validator.validateQuantity("10", 5, ActionType.REMOVE)
        assertTrue(result.isFailure)
        assertEquals("Nedostatek množství na skladě", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateQuantity allows REMOVE of exact stock amount`() {
        val result = validator.validateQuantity("5", 5, ActionType.REMOVE)
        assertTrue(result.isSuccess)
        assertEquals(5L, result.getOrNull())
    }

    @Test
    fun `validateQuantity handles empty string`() {
        val result = validator.validateQuantity("", 5, ActionType.ADD)
        assertTrue(result.isFailure)
        assertEquals("Neplatné množství", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateQuantity handles whitespace string`() {
        val result = validator.validateQuantity("   ", 5, ActionType.ADD)
        assertTrue(result.isFailure)
        assertEquals("Neplatné množství", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateQuantity handles large numbers`() {
        val result = validator.validateQuantity("999999999", 1000000000, ActionType.ADD)
        assertTrue(result.isSuccess)
        assertEquals(999999999L, result.getOrNull())
    }

    // ======== isValidScannedTextFormat tests ========
    // Note: The InputValidator only accepts specific lengths: 16, 18, or 22 characters
    // 16-char: no prefix, e.g., "BUK-C-30-50-2000"
    // 18-char: H- prefix, e.g., "H-BUK-C-30-50-2000"
    // 22-char: S- prefix for jointers, e.g., "S-DUB-ABP-42-0272-1860"

    @Test
    fun `isValidScannedTextFormat returns true for valid 16-char beam code`() {
        // 16-char format: e.g., "BUK-C-30-50-2000" (no H- prefix)
        // Conditions: text[1] != '-', text[11] == '-', last 4 chars are digits
        assertTrue(validator.isValidScannedTextFormat("BUK-C-30-50-2000"))
    }

    @Test
    fun `isValidScannedTextFormat returns true for valid 18-char beam code with H prefix`() {
        // 18-char format: e.g., "H-BUK-C-30-50-2000" (with H- prefix)
        assertTrue(validator.isValidScannedTextFormat("H-BUK-C-30-50-2000"))
    }

    @Test
    fun `isValidScannedTextFormat returns true for valid 22-char jointer code`() {
        // 22-char format: S-XXX-XXX-XX-XXXX-XXXX
        assertTrue(validator.isValidScannedTextFormat("S-DUB-ABP-42-0272-1860"))
        assertTrue(validator.isValidScannedTextFormat("S-DUB-A|A-27-0095-2050"))
        assertTrue(validator.isValidScannedTextFormat("S-ZIR-ZIR-32-0245-2150"))
    }

    @Test
    fun `isValidScannedTextFormat returns false for invalid length`() {
        // 17-char is not valid (common product ID like "DUB-A-20-100-2000" won't pass!)
        assertFalse(validator.isValidScannedTextFormat("DUB-A-20-100-2000"))  // 17 chars - not valid
        assertFalse(validator.isValidScannedTextFormat("SHORT"))
    }

    @Test
    fun `isValidScannedTextFormat returns false for 16-char with dash at position 1`() {
        // 16 chars but text[1] == '-', fails the condition
        assertFalse(validator.isValidScannedTextFormat("X-UK-C-30-50-200"))
    }

    @Test
    fun `isValidScannedTextFormat returns false for 18-char without H prefix`() {
        assertFalse(validator.isValidScannedTextFormat("X-BUK-C-30-50-2000"))
    }

    @Test
    fun `isValidScannedTextFormat returns false for 22-char without S prefix`() {
        assertFalse(validator.isValidScannedTextFormat("H-DUB-ABP-42-0272-1860"))
    }

    @Test
    fun `isValidScannedTextFormat returns false for code with non-digit ending`() {
        assertFalse(validator.isValidScannedTextFormat("BUK-C-30-50-200X"))
    }

    @Test
    fun `isValidScannedTextFormat returns false for empty string`() {
        assertFalse(validator.isValidScannedTextFormat(""))
    }

    // ======== manipulateAndValidateItemCode tests ========

    @Test
    fun `manipulateAndValidateItemCode adds H prefix to valid 16-char code`() {
        // 16-char code gets H- prefix added -> becomes 18-char
        val result = validator.manipulateAndValidateItemCode("BUK-C-30-50-2000")
        assertEquals("H-BUK-C-30-50-2000", result)
    }

    @Test
    fun `manipulateAndValidateItemCode uppercases input`() {
        val result = validator.manipulateAndValidateItemCode("buk-c-30-50-2000")
        assertEquals("H-BUK-C-30-50-2000", result)
    }

    @Test
    fun `manipulateAndValidateItemCode returns null for invalid code`() {
        val result = validator.manipulateAndValidateItemCode("INVALID")
        assertNull(result)
    }

    @Test
    fun `manipulateAndValidateItemCode preserves valid 18-char H-prefixed code`() {
        val result = validator.manipulateAndValidateItemCode("H-BUK-C-30-50-2000")
        assertEquals("H-BUK-C-30-50-2000", result)
    }

    @Test
    fun `manipulateAndValidateItemCode preserves valid 22-char S-prefixed code`() {
        val result = validator.manipulateAndValidateItemCode("S-DUB-ABP-42-0272-1860")
        assertEquals("S-DUB-ABP-42-0272-1860", result)
    }

    @Test
    fun `manipulateAndValidateItemCode does not add H to 16-char code starting with S`() {
        // A 16-char code starting with S should NOT get H- prefix added
        // The code is still valid as a 16-char beam code, just returned as-is
        val result = validator.manipulateAndValidateItemCode("SUB-C-30-50-2000")
        // Stays as 16-char since first char is S, and 16-char format is still valid
        assertEquals("SUB-C-30-50-2000", result)
    }

    @Test
    fun `manipulateAndValidateItemCode returns null for 17-char codes`() {
        // 17-char codes like "DUB-A-20-100-2000" are NOT handled by InputValidator
        // This is a potential gap in the validator!
        val result = validator.manipulateAndValidateItemCode("DUB-A-20-100-2000")
        assertNull(result)
    }
}
