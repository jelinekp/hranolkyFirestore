package eu.jelinek.hranolky.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StartViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    fun updateScannedCode(scannedCode: String) {
        _uiState.value = _uiState.value.copy(scannedCode = scannedCode)
    }
}

data class StartUiState(
    val scannedCode: String = "",

)