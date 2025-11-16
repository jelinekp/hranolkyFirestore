package eu.jelinek.hranolky.ui.start

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import eu.jelinek.hranolky.domain.DeviceManager
import eu.jelinek.hranolky.domain.InputValidator
import eu.jelinek.hranolky.domain.UpdateManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StartViewModel(
    application: Application,
    private val auth: FirebaseAuth,
    private val inputValidator: InputValidator,
    private val updateManager: UpdateManager,
    private val deviceManager: DeviceManager,
) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext

    private val _startScreenState = MutableStateFlow(StartUiState())
    val startScreenState get() = _startScreenState.asStateFlow()

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
        signInAnonymously()
    }

    private fun signInAnonymously() {
        viewModelScope.launch {
            _startScreenState.value = _startScreenState.value.copy(isSigningIn = true)
            try {
                if (auth.currentUser == null) {
                    auth.signInAnonymously().await()
                    Log.d("Auth", "signInAnonymously:success. UID: ${auth.currentUser?.uid}")
                } else {
                    Log.d("Auth", "User already signed in with UID: ${auth.currentUser?.uid}")
                }

                _startScreenState.value = deviceManager.fetchDeviceNameAndInventoryPermit(context, _startScreenState.value)
                if(_startScreenState.value.isInventoryCheckPermitted) loadInventoryCheckSetting()
                else toggleInventoryCheck(false)

                val state = deviceManager.logDeviceId(context, _startScreenState.value)
                _startScreenState.value = state
                updateManager.checkForUpdate(state.appVersionCode, context)

            } catch (e: Exception) {
                Log.w("Auth", "signInAnonymously:failure", e)
                _startScreenState.value = _startScreenState.value.copy(isSignInProblem = true)
            } finally {
                _startScreenState.value = _startScreenState.value.copy(isSigningIn = false)
            }
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
        var manipulatedCode = code
        val textLength = manipulatedCode.length
        if (textLength == 16) {
            if (manipulatedCode[1] != '-' && manipulatedCode[textLength - 5] == '-' && manipulatedCode.substring(textLength - 4).all(Char::isDigit)) {
                if (manipulatedCode.first() != 'S') {
                    manipulatedCode = "H-$manipulatedCode"
                }
            }
        }
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
