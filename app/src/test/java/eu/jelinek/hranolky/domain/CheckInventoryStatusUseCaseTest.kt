package eu.jelinek.hranolky.domain

import com.google.firebase.Timestamp
import eu.jelinek.hranolky.model.ActionType
import eu.jelinek.hranolky.model.SlotAction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Tests for CheckInventoryStatusUseCase
 */
class CheckInventoryStatusUseCaseTest {

    private lateinit var useCase: CheckInventoryStatusUseCase

    @Before
    fun setup() {
        useCase = CheckInventoryStatusUseCase()
    }

    private fun createTimestamp(daysAgo: Int): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return Timestamp(calendar.time)
    }

    private fun createSlotAction(actionType: ActionType, daysAgo: Int): SlotAction {
        return SlotAction(
            action = actionType.toString(),
            timestamp = createTimestamp(daysAgo),
            userId = "test-device",
            quantityChange = 0,
            newQuantity = 100
        )
    }

    // ======== Basic functionality ========

    @Test
    fun `returns false for null actions`() {
        val result = useCase.isInventoryCheckDone(null)
        assertFalse(result)
    }

    @Test
    fun `returns false for empty actions list`() {
        val result = useCase.isInventoryCheckDone(emptyList())
        assertFalse(result)
    }

    @Test
    fun `returns true for recent inventory check`() {
        val actions = listOf(
            createSlotAction(ActionType.INVENTORY_CHECK, daysAgo = 30)
        )
        val result = useCase.isInventoryCheckDone(actions)
        assertTrue(result)
    }

    @Test
    fun `returns false for old inventory check beyond 75 days`() {
        val actions = listOf(
            createSlotAction(ActionType.INVENTORY_CHECK, daysAgo = 80)
        )
        val result = useCase.isInventoryCheckDone(actions)
        assertFalse(result)
    }

    // ======== Boundary conditions ========

    @Test
    fun `returns true for inventory check exactly 74 days ago`() {
        val actions = listOf(
            createSlotAction(ActionType.INVENTORY_CHECK, daysAgo = 74)
        )
        val result = useCase.isInventoryCheckDone(actions)
        assertTrue(result)
    }

    @Test
    fun `returns false for inventory check exactly 76 days ago`() {
        val actions = listOf(
            createSlotAction(ActionType.INVENTORY_CHECK, daysAgo = 76)
        )
        val result = useCase.isInventoryCheckDone(actions)
        assertFalse(result)
    }

    @Test
    fun `returns true for inventory check today`() {
        val actions = listOf(
            createSlotAction(ActionType.INVENTORY_CHECK, daysAgo = 0)
        )
        val result = useCase.isInventoryCheckDone(actions)
        assertTrue(result)
    }

    // ======== Action type filtering ========

    @Test
    fun `ignores ADD actions when checking inventory status`() {
        val actions = listOf(
            createSlotAction(ActionType.ADD, daysAgo = 10)
        )
        val result = useCase.isInventoryCheckDone(actions)
        assertFalse(result)
    }

    @Test
    fun `ignores REMOVE actions when checking inventory status`() {
        val actions = listOf(
            createSlotAction(ActionType.REMOVE, daysAgo = 10)
        )
        val result = useCase.isInventoryCheckDone(actions)
        assertFalse(result)
    }

    @Test
    fun `finds inventory check among other actions`() {
        val actions = listOf(
            createSlotAction(ActionType.ADD, daysAgo = 5),
            createSlotAction(ActionType.REMOVE, daysAgo = 10),
            createSlotAction(ActionType.INVENTORY_CHECK, daysAgo = 30),
            createSlotAction(ActionType.ADD, daysAgo = 50)
        )
        val result = useCase.isInventoryCheckDone(actions)
        assertTrue(result)
    }

    // ======== Null timestamp handling ========

    @Test
    fun `ignores action with null timestamp`() {
        val action = SlotAction(
            action = ActionType.INVENTORY_CHECK.toString(),
            timestamp = null,
            userId = "test-device",
            quantityChange = 0,
            newQuantity = 100
        )
        val result = useCase.isInventoryCheckDone(listOf(action))
        assertFalse(result)
    }

    // ======== Multiple inventory checks ========

    @Test
    fun `returns true if any inventory check is recent`() {
        val actions = listOf(
            createSlotAction(ActionType.INVENTORY_CHECK, daysAgo = 100), // Old
            createSlotAction(ActionType.INVENTORY_CHECK, daysAgo = 30)   // Recent
        )
        val result = useCase.isInventoryCheckDone(actions)
        assertTrue(result)
    }

    @Test
    fun `returns false if all inventory checks are old`() {
        val actions = listOf(
            createSlotAction(ActionType.INVENTORY_CHECK, daysAgo = 100),
            createSlotAction(ActionType.INVENTORY_CHECK, daysAgo = 90)
        )
        val result = useCase.isInventoryCheckDone(actions)
        assertFalse(result)
    }
}
