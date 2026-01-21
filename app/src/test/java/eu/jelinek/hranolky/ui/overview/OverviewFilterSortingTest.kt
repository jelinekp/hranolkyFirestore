package eu.jelinek.hranolky.ui.overview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for OverviewViewModel filtering and sorting data classes.
 */
class OverviewFilterSortingTest {

    // ========== SlotFilters tests ==========

    @Test
    fun `SlotFilters EMPTY has all empty lists`() {
        val empty = SlotFilters.EMPTY

        assertTrue(empty.qualityFilters.isEmpty())
        assertTrue(empty.thicknessFilters.isEmpty())
        assertTrue(empty.widthFilters.isEmpty())
        assertTrue(empty.lengthFilters.isEmpty())
        assertTrue(empty.isEmpty())
    }

    @Test
    fun `SlotFilters isEmpty returns true when all filters empty`() {
        val filters = SlotFilters(
            qualityFilters = emptyList(),
            thicknessFilters = emptyList(),
            widthFilters = emptyList(),
            lengthFilters = emptyList()
        )

        assertTrue(filters.isEmpty())
    }

    @Test
    fun `SlotFilters isEmpty returns false when any filter has values`() {
        val withQuality = SlotFilters.EMPTY.copy(qualityFilters = listOf("DUB-A"))
        val withThickness = SlotFilters.EMPTY.copy(thicknessFilters = listOf(20f))
        val withWidth = SlotFilters.EMPTY.copy(widthFilters = listOf(40f))
        val withLength = SlotFilters.EMPTY.copy(lengthFilters = listOf(IntervalMm(0, 999)))

        assertFalse(withQuality.isEmpty())
        assertFalse(withThickness.isEmpty())
        assertFalse(withWidth.isEmpty())
        assertFalse(withLength.isEmpty())
    }

    @Test
    fun `SlotFilters hasQualityFilters returns correct value`() {
        val empty = SlotFilters.EMPTY
        val withQuality = empty.copy(qualityFilters = listOf("DUB-A", "DUB-R"))

        assertFalse(empty.hasQualityFilters())
        assertTrue(withQuality.hasQualityFilters())
    }

    @Test
    fun `SlotFilters hasThicknessFilters returns correct value`() {
        val empty = SlotFilters.EMPTY
        val withThickness = empty.copy(thicknessFilters = listOf(20f, 27.4f))

        assertFalse(empty.hasThicknessFilters())
        assertTrue(withThickness.hasThicknessFilters())
    }

    @Test
    fun `SlotFilters hasWidthFilters returns correct value`() {
        val empty = SlotFilters.EMPTY
        val withWidth = empty.copy(widthFilters = listOf(40f, 50f))

        assertFalse(empty.hasWidthFilters())
        assertTrue(withWidth.hasWidthFilters())
    }

    @Test
    fun `SlotFilters hasLengthFilters returns correct value`() {
        val empty = SlotFilters.EMPTY
        val withLength = empty.copy(lengthFilters = listOf(IntervalMm(0, 999)))

        assertFalse(empty.hasLengthFilters())
        assertTrue(withLength.hasLengthFilters())
    }

    @Test
    fun `SlotFilters getNumberOfActiveFilters counts all active filters`() {
        val filters = SlotFilters(
            qualityFilters = listOf("A", "B"),
            thicknessFilters = listOf(20f),
            widthFilters = listOf(40f, 50f, 60f),
            lengthFilters = listOf(IntervalMm(0, 999))
        )

        assertEquals(7, filters.getNumberOfActiveFilters())
    }

    @Test
    fun `SlotFilters LENGTH_INTERVALS has predefined intervals`() {
        val intervals = SlotFilters.LENGTH_INTERVALS

        assertEquals(5, intervals.lengthFilters.size)
        assertTrue(intervals.qualityFilters.isEmpty())
        assertTrue(intervals.thicknessFilters.isEmpty())
        assertTrue(intervals.widthFilters.isEmpty())
    }

    // ========== IntervalMm tests ==========

    @Test
    fun `IntervalMm contains returns true for value in range`() {
        val interval = IntervalMm(1000, 1999)

        assertTrue(interval.contains(1000))
        assertTrue(interval.contains(1500))
        assertTrue(interval.contains(1999))
    }

    @Test
    fun `IntervalMm contains returns false for value outside range`() {
        val interval = IntervalMm(1000, 1999)

        assertFalse(interval.contains(999))
        assertFalse(interval.contains(2000))
        assertFalse(interval.contains(500))
    }

    @Test
    fun `IntervalMm contains returns false for null value`() {
        val interval = IntervalMm(1000, 1999)

        assertFalse(interval.contains(null))
    }

    @Test
    fun `IntervalMm toString formats correctly`() {
        val interval = IntervalMm(1000, 1999)

        assertEquals("1000 - 1999", interval.toString())
    }

    @Test
    fun `IntervalMm handles zero-based interval`() {
        val interval = IntervalMm(0, 599)

        assertTrue(interval.contains(0))
        assertTrue(interval.contains(599))
        assertFalse(interval.contains(600))
    }

    // ========== SlotSum tests ==========

    @Test
    fun `SlotSum EMPTY has zero values`() {
        val empty = SlotSum.EMPTY

        assertEquals(0, empty.count)
        assertEquals(0L, empty.quantitySum)
        assertEquals(0.0, empty.volumeSum, 0.001)
    }

    @Test
    fun `SlotSum can be created with values`() {
        val sum = SlotSum(
            count = 5,
            quantitySum = 100L,
            volumeSum = 2.5
        )

        assertEquals(5, sum.count)
        assertEquals(100L, sum.quantitySum)
        assertEquals(2.5, sum.volumeSum, 0.001)
    }

    @Test
    fun `SlotSum copy works correctly`() {
        val original = SlotSum(count = 3, quantitySum = 50L, volumeSum = 1.0)
        val modified = original.copy(count = 4)

        assertEquals(4, modified.count)
        assertEquals(50L, modified.quantitySum)
        assertEquals(1.0, modified.volumeSum, 0.001)
    }

    // ========== SortingDirection tests ==========

    @Test
    fun `SortingDirection has all expected values`() {
        val values = SortingDirection.entries

        assertEquals(3, values.size)
        assertTrue(values.contains(SortingDirection.ASC))
        assertTrue(values.contains(SortingDirection.DESC))
        assertTrue(values.contains(SortingDirection.NONE))
    }

    // ========== OverviewUiState tests ==========

    @Test
    fun `OverviewUiState has correct default values`() {
        val state = OverviewUiState()

        assertTrue(state.allSlots.isEmpty())
        assertTrue(state.selectedSlots.isEmpty())
        assertTrue(state.sortedSlots.isEmpty())
        assertEquals(SlotSum.EMPTY, state.sum)
        assertEquals("quantity", state.sortingBy)
        assertEquals(SortingDirection.DESC, state.sortingDirection)
        assertTrue(state.loading)
    }

    @Test
    fun `OverviewUiState copy preserves unmodified fields`() {
        val original = OverviewUiState(
            sortingBy = "length",
            sortingDirection = SortingDirection.ASC,
            loading = false
        )

        val modified = original.copy(loading = true)

        assertEquals("length", modified.sortingBy)
        assertEquals(SortingDirection.ASC, modified.sortingDirection)
        assertTrue(modified.loading)
    }
}
