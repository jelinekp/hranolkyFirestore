package eu.jelinek.hranolky.domain

import eu.jelinek.hranolky.config.AppConfig
import eu.jelinek.hranolky.model.ActionType
import eu.jelinek.hranolky.model.SlotAction
import java.util.Calendar
import java.util.Date

/**
 * Use case for checking if a slot's inventory has been verified recently.
 *
 * This follows the Action Version Transparency (AVT) principle by:
 * 1. Isolating the inventory check algorithm into a dedicated class
 * 2. Making the business rule (75 days) configurable
 * 3. Allowing the algorithm to evolve independently of the ViewModel
 */
class CheckInventoryStatusUseCase {

    /**
     * Checks if any INVENTORY_CHECK action exists within the configured stale period.
     *
     * @param slotActions List of actions performed on the slot
     * @return true if a recent inventory check exists, false otherwise
     */
    fun isInventoryCheckDone(slotActions: List<SlotAction>?): Boolean {
        if (slotActions.isNullOrEmpty()) {
            return false
        }

        val limitDate = calculateStaleDate()

        return slotActions.any { slotAction ->
            isRecentInventoryCheck(slotAction, limitDate)
        }
    }

    /**
     * Calculates the date before which inventory checks are considered stale.
     */
    private fun calculateStaleDate(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -AppConfig.InventoryCheck.STALE_DAYS)
        return calendar.time
    }

    /**
     * Checks if a given action is a recent inventory check.
     */
    private fun isRecentInventoryCheck(slotAction: SlotAction, limitDate: Date): Boolean {
        if (slotAction.action != ActionType.INVENTORY_CHECK.toString()) {
            return false
        }

        val timestamp = slotAction.timestamp ?: return false
        val actionDate = timestamp.toDate()

        return actionDate.after(limitDate)
    }
}
