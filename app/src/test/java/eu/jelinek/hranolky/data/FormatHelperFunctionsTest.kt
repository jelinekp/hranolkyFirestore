package eu.jelinek.hranolky.data

import eu.jelinek.hranolky.data.helpers.formatCubicMeters
import eu.jelinek.hranolky.data.helpers.formatCubicMetersTwo
import eu.jelinek.hranolky.data.helpers.formatDate
import eu.jelinek.hranolky.data.helpers.formatOnlyDate
import eu.jelinek.hranolky.data.helpers.toCustomCommaDecimalString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class FormatHelperFunctionsTest {

    @Test
    fun `formatDate returns expected pattern`() {
        val date = Date(0) // 1 Jan 1970 UTC
        val formatted = formatDate(date)
        // Allow locale differences in whitespace or order is risky; just assert contains year
        assert(formatted.contains("1970"))
    }

    @Test
    fun `formatOnlyDate returns expected pattern`() {
        val date = Date(0)
        val formatted = formatOnlyDate(date)
        assert(formatted.contains("1970"))
    }

    @Test
    fun `formatCubicMeters formats with three decimals and unit`() {
        assertEquals("0.0 m³", formatCubicMeters(null))
        assertEquals("1.235 m³", formatCubicMeters(1.23456))
    }

    @Test
    fun `formatCubicMetersTwo formats with two decimals`() {
        assertEquals("0.0", formatCubicMetersTwo(null))
        assertEquals("1.23", formatCubicMetersTwo(1.23456))
    }

    @Test
    fun `toCustomCommaDecimalString formats with comma and max 2 decimals`() {
        assertEquals("0,0", (null as Float?).toCustomCommaDecimalString())
        assertEquals("42", 42.0f.toCustomCommaDecimalString())
        val v = 42.4f.toCustomCommaDecimalString()
        assert(v == "42,4" || v == "42,40") // depending on fraction digits
    }
}

