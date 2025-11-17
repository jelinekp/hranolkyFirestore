package eu.jelinek.hranolky.model

import com.google.firebase.Timestamp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirestoreSlotTest {

    @Test
    fun `toWarehouseSlot should correctly convert FirestoreSlot to WarehouseSlot with H-prefix`() {
        val firestoreSlot = FirestoreSlot(
            quantity = 5,
            lastModified = Timestamp.now()
        )
        val productId = "DUB-A-20-100-2000"
        val warehouseSlot = firestoreSlot.toWarehouseSlot(SlotType.Beam, productId)

        assertEquals(productId, warehouseSlot.productId)
        assertEquals(5, warehouseSlot.quantity)
        assertEquals(firestoreSlot.lastModified, warehouseSlot.lastModified)
        assertEquals(SlotType.Beam, warehouseSlot.slotType)
        assertEquals("DUB-A", warehouseSlot.quality)
        assertEquals(20.0f, warehouseSlot.thickness)
        assertEquals(100.0f, warehouseSlot.width)
        assertEquals(2000, warehouseSlot.length)
    }

    @Test
    fun `toWarehouseSlot should correctly convert FirestoreSlot to WarehouseSlot with S-prefix`() {
        val firestoreSlot = FirestoreSlot(
            quantity = 10,
            lastModified = Timestamp.now()
        )
        val productId = "DUB-R-27-42-3000"
        val warehouseSlot = firestoreSlot.toWarehouseSlot(SlotType.Jointer, productId)

        assertEquals(productId, warehouseSlot.productId)
        assertEquals(10, warehouseSlot.quantity)
        assertEquals(firestoreSlot.lastModified, warehouseSlot.lastModified)
        assertEquals(SlotType.Jointer, warehouseSlot.slotType)
        assertEquals("DUB-R", warehouseSlot.quality)
        assertEquals(27.4f, warehouseSlot.thickness)
        assertEquals(42.4f, warehouseSlot.width)
        assertEquals(3000, warehouseSlot.length)
    }

    @Test
    fun `toWarehouseSlot should correctly convert FirestoreSlot to WarehouseSlot without prefix`() {
        val firestoreSlot = FirestoreSlot(
            quantity = 15,
            lastModified = Timestamp.now()
        )
        val productId = "BUK-C-30-50-1500"
        val warehouseSlot = firestoreSlot.toWarehouseSlot(SlotType.Beam, productId)

        assertEquals(productId, warehouseSlot.productId)
        assertEquals(15, warehouseSlot.quantity)
        assertEquals(firestoreSlot.lastModified, warehouseSlot.lastModified)
        assertEquals(SlotType.Beam, warehouseSlot.slotType) // Default to Beam
        assertEquals("BUK-C", warehouseSlot.quality)
        assertEquals(30.0f, warehouseSlot.thickness)
        assertEquals(50.0f, warehouseSlot.width)
        assertEquals(1500, warehouseSlot.length)
    }

    @Test
    fun `toWarehouseSlot should handle invalid product ID format`() {
        val firestoreSlot = FirestoreSlot(
            quantity = 1,
            lastModified = Timestamp.now()
        )
        val productId = "INVALID-ID"
        val warehouseSlot = firestoreSlot.toWarehouseSlot(SlotType.Beam, productId)

        assertEquals(productId, warehouseSlot.productId)
        assertEquals(1, warehouseSlot.quantity)
        assertEquals(firestoreSlot.lastModified, warehouseSlot.lastModified)
        // Assert that properties derived from product ID are null or default if parsing fails
        assertEquals(null, warehouseSlot.slotType)
        assertEquals(null, warehouseSlot.quality)
        assertEquals(null, warehouseSlot.thickness)
        assertEquals(null, warehouseSlot.width)
        assertEquals(null, warehouseSlot.length)
    }

    @After
    fun tearDown() {
        stopKoin()
    }
}
