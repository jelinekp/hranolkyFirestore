package eu.jelinek.hranolky.ui.start

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.jelinek.hranolky.domain.AuthManager
import eu.jelinek.hranolky.domain.DeviceManager
import eu.jelinek.hranolky.domain.InputValidator
import eu.jelinek.hranolky.domain.UpdateManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StartViewModel(
    application: Application,
    private val authManager: AuthManager,
    private val inputValidator: InputValidator,
    private val updateManager: UpdateManager,
    private val deviceManager: DeviceManager,
) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext

    private val _startScreenState = MutableStateFlow(StartUiState())
    val startScreenState get() = _startScreenState.asStateFlow()

    val authState = authManager.authState
    val updateState = updateManager.updateState

    private val _navigateToManageItem = MutableSharedFlow<String>()
    val navigateToManageItem = _navigateToManageItem.asSharedFlow()

    private companion object {
        const val PREF_INVENTORY_CHECK_ENABLED = "inventory_check_enabled"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    }

    init {
        // Collect auth state changes
        viewModelScope.launch {
            authManager.authState.collect { authState ->
                if (authState.isSignedIn && !authState.isLoading) {
                    // User is signed in, initialize app data
                    initializeAppData()
                }
            }
        }

        // Check if already signed in (persisted from previous session)
        if (authManager.isSignedIn) {
            viewModelScope.launch {
                initializeAppData()
            }
        }
    }

    /**
     * Initiates Google Sign-In. Call this from the UI when user taps sign-in button.
     */
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            authManager.signInWithGoogle(context)
        }
    }

    private suspend fun initializeAppData() {
        _startScreenState.value = _startScreenState.value.copy(isSigningIn = true)
        try {
            _startScreenState.value = deviceManager.fetchDeviceNameAndInventoryPermit(context, _startScreenState.value)
            if (_startScreenState.value.isInventoryCheckPermitted) loadInventoryCheckSetting()
            else toggleInventoryCheck(false)

            val state = deviceManager.logDeviceId(context, _startScreenState.value)
            _startScreenState.value = state

            // Check if app was just updated (before checking for new updates)
            updateManager.checkIfJustUpdated(context, state.appVersionCode)

            updateManager.checkForUpdate(state.appVersionCode, context)

        } catch (e: Exception) {
            Log.w("StartViewModel", "Failed to initialize app data", e)
            _startScreenState.value = _startScreenState.value.copy(isSignInProblem = true)
        } finally {
            _startScreenState.value = _startScreenState.value.copy(isSigningIn = false)
        }
    }

    fun onScannedCodeChange(text: String, isManualInput: Boolean) {
        viewModelScope.launch {
            _startScreenState.value = _startScreenState.value.copy(
                scannedCode = text.uppercase(),
                isFormatError = false,
                isSignInError = false
            )
            if (!isManualInput && inputValidator.isValidScannedTextFormat(text)) {
                processAndNavigate(text)
            }
        }
    }

    fun onSubmit() {
        viewModelScope.launch {
            if (_startScreenState.value.isSigningIn) {
                _startScreenState.value = _startScreenState.value.copy(isSignInError = true)
                return@launch
            }
            _startScreenState.value = _startScreenState.value.copy(isSignInError = false)

            val code = _startScreenState.value.scannedCode
            if (inputValidator.isValidScannedTextFormat(code)) {
                processAndNavigate(code)
                _startScreenState.value = _startScreenState.value.copy(isFormatError = false)
            } else {
                _startScreenState.value = _startScreenState.value.copy(isFormatError = true)
            }
        }
    }

    private suspend fun processAndNavigate(code: String) {
        val manipulatedCode = inputValidator.manipulateAndValidateItemCode(code) ?: code
        _navigateToManageItem.emit(manipulatedCode)
        // Clear the scanned code immediately after navigation to prevent re-navigation when coming back
        clearScannedCode()
    }

    fun clearScannedCode() {
        _startScreenState.value = _startScreenState.value.copy(scannedCode = "", isFormatError = false, isSignInError = false)
    }

    fun toggleInventoryCheck(isEnabled: Boolean) {
        _startScreenState.value = _startScreenState.value.copy(isInventoryCheckEnabled = isEnabled)
        sharedPreferences.edit { putBoolean(PREF_INVENTORY_CHECK_ENABLED, isEnabled) }
        Log.d("StartViewModel", "Saved inventory check setting: $isEnabled")
    }

    private fun loadInventoryCheckSetting() {
        val isEnabled = sharedPreferences.getBoolean(PREF_INVENTORY_CHECK_ENABLED, false)
        _startScreenState.value = _startScreenState.value.copy(isInventoryCheckEnabled = isEnabled)
        Log.d("StartViewModel", "Loaded inventory check setting: $isEnabled")
    }

    fun signOut() {
        authManager.signOut()
        _startScreenState.value = StartUiState() // Reset state
    }
}

data class StartUiState(
    val scannedCode: String = "",
    val shortenedDeviceId: String = "",
    val deviceName: String? = null,
    val appVersion: String = "",
    val appVersionCode: Int = -1,
    val isInventoryCheckPermitted: Boolean = false,
    val isInventoryCheckEnabled: Boolean = false,
    val isSigningIn: Boolean = false,
    val isSignInProblem: Boolean = false,
    val isFormatError: Boolean = false,
    val isSignInError: Boolean = false
)
