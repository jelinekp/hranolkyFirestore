package eu.jelinek.hranolky.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for QuantityParser following NS principle verification
 */
class QuantityParserTest {

    private lateinit var parser: QuantityParser

    @Before
    fun setup() {
        parser = QuantityParser()
    }

    // ======== Simple number parsing ========

    @Test
    fun `parse returns success for simple positive number`() {
        val result = parser.parse("123")
        assertTrue(result is QuantityParser.ParseResult.Success)
        assertEquals(123L, (result as QuantityParser.ParseResult.Success).quantity)
    }

    @Test
    fun `parse returns success for zero`() {
        val result = parser.parse("0")
        assertTrue(result is QuantityParser.ParseResult.Success)
        assertEquals(0L, (result as QuantityParser.ParseResult.Success).quantity)
    }

    @Test
    fun `parse returns error for negative number`() {
        val result = parser.parse("-5")
        assertTrue(result is QuantityParser.ParseResult.Error)
        assertEquals(QuantityParser.ErrorMessages.NEGATIVE, (result as QuantityParser.ParseResult.Error).message)
    }

    @Test
    fun `parse returns error for empty string`() {
        val result = parser.parse("")
        assertTrue(result is QuantityParser.ParseResult.Error)
        assertEquals(QuantityParser.ErrorMessages.EMPTY, (result as QuantityParser.ParseResult.Error).message)
    }

    @Test
    fun `parse returns error for blank string`() {
        val result = parser.parse("   ")
        assertTrue(result is QuantityParser.ParseResult.Error)
        assertEquals(QuantityParser.ErrorMessages.EMPTY, (result as QuantityParser.ParseResult.Error).message)
    }

    // ======== Sum parsing ========

    @Test
    fun `parse returns success for simple sum`() {
        val result = parser.parse("50+30")
        assertTrue(result is QuantityParser.ParseResult.Success)
        assertEquals(80L, (result as QuantityParser.ParseResult.Success).quantity)
    }

    @Test
    fun `parse returns success for multi-part sum`() {
        val result = parser.parse("10+20+30+40")
        assertTrue(result is QuantityParser.ParseResult.Success)
        assertEquals(100L, (result as QuantityParser.ParseResult.Success).quantity)
    }

    @Test
    fun `parse handles spaces in sum`() {
        val result = parser.parse("50 + 30")
        assertTrue(result is QuantityParser.ParseResult.Success)
        assertEquals(80L, (result as QuantityParser.ParseResult.Success).quantity)
    }

    @Test
    fun `parse returns error for empty part in sum`() {
        val result = parser.parse("50++30")
        assertTrue(result is QuantityParser.ParseResult.Error)
        assertEquals(QuantityParser.ErrorMessages.EMPTY_PART, (result as QuantityParser.ParseResult.Error).message)
    }

    @Test
    fun `parse returns error for trailing plus`() {
        val result = parser.parse("50+")
        assertTrue(result is QuantityParser.ParseResult.Error)
        assertEquals(QuantityParser.ErrorMessages.EMPTY_PART, (result as QuantityParser.ParseResult.Error).message)
    }

    @Test
    fun `parse returns error for leading plus`() {
        val result = parser.parse("+50")
        assertTrue(result is QuantityParser.ParseResult.Error)
        assertEquals(QuantityParser.ErrorMessages.EMPTY_PART, (result as QuantityParser.ParseResult.Error).message)
    }

    @Test
    fun `parse returns error for invalid part in sum`() {
        val result = parser.parse("50+abc+30")
        assertTrue(result is QuantityParser.ParseResult.Error)
        assertTrue((result as QuantityParser.ParseResult.Error).message.contains("abc"))
    }

    // ======== Item code detection ========

    @Test
    fun `parse detects item code when validator provided`() {
        val validator: (String) -> String? = { code ->
            if (code == "H-DUB-A-20-100-2000") code else null
        }

        val result = parser.parse("H-DUB-A-20-100-2000", validator)
        assertTrue(result is QuantityParser.ParseResult.ItemCodeDetected)
        assertEquals("H-DUB-A-20-100-2000", (result as QuantityParser.ParseResult.ItemCodeDetected).itemCode)
    }

    @Test
    fun `parse returns error for invalid string when no validator`() {
        val result = parser.parse("abc")
        assertTrue(result is QuantityParser.ParseResult.Error)
        assertEquals(QuantityParser.ErrorMessages.INVALID_FORMAT, (result as QuantityParser.ParseResult.Error).message)
    }

    @Test
    fun `parse returns error for unrecognized item code`() {
        val validator: (String) -> String? = { null }

        val result = parser.parse("INVALID-CODE", validator)
        assertTrue(result is QuantityParser.ParseResult.Error)
        assertEquals(QuantityParser.ErrorMessages.INVALID_FORMAT, (result as QuantityParser.ParseResult.Error).message)
    }

    // ======== Legacy method ========

    @Test
    fun `parseToResult returns success for valid number`() {
        val result = parser.parseToResult("42")
        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrNull())
    }

    @Test
    fun `parseToResult returns failure for invalid input`() {
        val result = parser.parseToResult("")
        assertTrue(result.isFailure)
    }

    // ======== Large numbers ========

    @Test
    fun `parse handles large numbers within Long range`() {
        val result = parser.parse("1000000000")
        assertTrue(result is QuantityParser.ParseResult.Success)
        assertEquals(1_000_000_000L, (result as QuantityParser.ParseResult.Success).quantity)
    }

    @Test
    fun `parse handles sum of large numbers`() {
        val result = parser.parse("500000000+500000000")
        assertTrue(result is QuantityParser.ParseResult.Success)
        assertEquals(1_000_000_000L, (result as QuantityParser.ParseResult.Success).quantity)
    }
}
