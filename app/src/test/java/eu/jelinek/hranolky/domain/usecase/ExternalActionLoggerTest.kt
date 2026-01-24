package eu.jelinek.hranolky.domain.usecase

import com.google.firebase.Timestamp
import eu.jelinek.hranolky.data.SheetDbRepository
import eu.jelinek.hranolky.data.network.JointerReportingRow
import eu.jelinek.hranolky.data.network.SheetDbPostResponse
import eu.jelinek.hranolky.model.SlotAction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class SheetDbActionLoggerTest {

    private lateinit var sheetDbRepository: SheetDbRepository
    private lateinit var logger: SheetDbActionLogger

    @Before
    fun setup() {
        sheetDbRepository = mockk()
        logger = SheetDbActionLogger(sheetDbRepository)
    }

    @Test
    fun `logAction skips non-ADD actions`() = runTest {
        val action = SlotAction(
            action = "REMOVE",
            quantityChange = -5,
            newQuantity = 10,
            timestamp = Timestamp.now()
        )

        val result = logger.logAction(action, "H-DUB-A-27-42-3000", "device123")

        assertTrue(result.isSuccess)
        assertEquals(LoggingResult.Skipped, result.getOrNull())
        coVerify(exactly = 0) { sheetDbRepository.addLogRow(any()) }
    }

    @Test
    fun `logAction logs ADD actions to SheetDB`() = runTest {
        val rowSlot = slot<JointerReportingRow>()
        coEvery { sheetDbRepository.addLogRow(capture(rowSlot)) } returns Result.success(
            SheetDbPostResponse(created = 1)
        )

        val action = SlotAction(
            action = "ADD",
            quantityChange = 10,
            newQuantity = 20,
            timestamp = Timestamp(Date())
        )

        val result = logger.logAction(action, "S-DUB-A-27-42-3000", "device123")

        assertTrue(result.isSuccess)
        val loggingResult = result.getOrNull()
        assertTrue(loggingResult is LoggingResult.Success)

        // Verify row was created with correct data
        val capturedRow = rowSlot.captured
        assertEquals("DUB-A", capturedRow.quality)
        assertEquals("27", capturedRow.thickness)
        assertEquals("42", capturedRow.width)
        assertEquals("3000", capturedRow.length)
        assertEquals("10", capturedRow.quantityChange)
        assertEquals("device123", capturedRow.madeBy)
    }

    @Test
    fun `logAction parses H-prefix slotId correctly`() = runTest {
        val rowSlot = slot<JointerReportingRow>()
        coEvery { sheetDbRepository.addLogRow(capture(rowSlot)) } returns Result.success(
            SheetDbPostResponse(created = 1)
        )

        val action = SlotAction(
            action = "ADD",
            quantityChange = 5,
            newQuantity = 15,
            timestamp = Timestamp(Date())
        )

        val result = logger.logAction(action, "H-BUK-C-30-50-1500", "device456")

        assertTrue(result.isSuccess)

        val capturedRow = rowSlot.captured
        assertEquals("BUK-C", capturedRow.quality)
        assertEquals("30", capturedRow.thickness)
        assertEquals("50", capturedRow.width)
        assertEquals("1500", capturedRow.length)
    }

    @Test
    fun `logAction handles SheetDB failure gracefully`() = runTest {
        coEvery { sheetDbRepository.addLogRow(any()) } returns Result.failure(
            Exception("Network error")
        )

        val action = SlotAction(
            action = "ADD",
            quantityChange = 10,
            newQuantity = 20,
            timestamp = Timestamp(Date())
        )

        val result = logger.logAction(action, "S-DUB-A-27-42-3000", "device123")

        // Should succeed with NonBlockingFailure (doesn't block main operation)
        assertTrue(result.isSuccess)
        val loggingResult = result.getOrNull()
        assertTrue(loggingResult is LoggingResult.NonBlockingFailure)
        assertEquals("Network error", (loggingResult as LoggingResult.NonBlockingFailure).error)
    }

    @Test
    fun `logAction handles malformed slotId`() = runTest {
        val rowSlot = slot<JointerReportingRow>()
        coEvery { sheetDbRepository.addLogRow(capture(rowSlot)) } returns Result.success(
            SheetDbPostResponse(created = 1)
        )

        val action = SlotAction(
            action = "ADD",
            quantityChange = 5,
            newQuantity = 10,
            timestamp = Timestamp(Date())
        )

        val result = logger.logAction(action, "INVALID", "device123")

        assertTrue(result.isSuccess)

        // Should use fallback values for missing parts
        val capturedRow = rowSlot.captured
        assertEquals("INVALID", capturedRow.quality)
        assertEquals("?", capturedRow.thickness)
    }
}

class NoOpExternalActionLoggerTest {

    @Test
    fun `logAction always returns Skipped`() = runTest {
        val logger = NoOpExternalActionLogger()

        val action = SlotAction(
            action = "ADD",
            quantityChange = 10,
            newQuantity = 20,
            timestamp = Timestamp(Date())
        )

        val result = logger.logAction(action, "test-slot", "device123")

        assertTrue(result.isSuccess)
        assertEquals(LoggingResult.Skipped, result.getOrNull())
    }
}
