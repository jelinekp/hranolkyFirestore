package eu.jelinek.hranolky.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test

class SlotActionTest {

    @Test
    fun `SlotAction should be created with correct values`() {
        val timestamp = Timestamp.now()
        val slotAction = SlotAction(
            action = "add",
            quantityChange = 10,
            newQuantity = 20,
            userId = "user1",
            userName = "John Doe",
            timestamp = timestamp
        )

        assertEquals("add", slotAction.action)
        assertEquals(10, slotAction.quantityChange)
        assertEquals(20, slotAction.newQuantity)
        assertEquals("user1", slotAction.userId)
        assertEquals("John Doe", slotAction.userName)
        assertEquals(timestamp, slotAction.timestamp)
    }
}
