package eu.jelinek.hranolky.data.preferences

import android.content.Context
import android.preference.PreferenceManager

/**
 * Repository for managing inventory check preference persistence.
 * Follows Separation of Concerns (SoC) by isolating preference management
 * from the ViewModel.
 */
interface InventoryCheckPreferencesRepository {
    /**
     * Check if inventory check mode is enabled.
     */
    fun isInventoryCheckEnabled(): Boolean

    /**
     * Enable or disable inventory check mode.
     */
    fun setInventoryCheckEnabled(enabled: Boolean)
}

/**
 * Implementation using SharedPreferences for persistence.
 */
class InventoryCheckPreferencesRepositoryImpl(
    private val context: Context
) : InventoryCheckPreferencesRepository {

    private companion object {
        const val PREF_INVENTORY_CHECK_ENABLED = "inventory_check_enabled"
    }

    @Suppress("DEPRECATION")
    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun isInventoryCheckEnabled(): Boolean =
        prefs.getBoolean(PREF_INVENTORY_CHECK_ENABLED, false)

    override fun setInventoryCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_INVENTORY_CHECK_ENABLED, enabled).apply()
    }
}
