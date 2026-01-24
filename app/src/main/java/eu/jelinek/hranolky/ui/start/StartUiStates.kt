package eu.jelinek.hranolky.ui.start

/**
 * Isolated UI state classes for StartScreen, following Separation of States (SoS) principle.
 * Each state class focuses on a single concern.
 */

/**
 * State for the scanned code input and validation.
 */
data class ScannedCodeState(
    val code: String = "",
    val isFormatError: Boolean = false
)

/**
 * State for device information display.
 */
data class DeviceInfoState(
    val shortenedDeviceId: String = "",
    val deviceName: String? = null,
    val appVersion: String = "",
    val appVersionCode: Int = -1
)

/**
 * State for inventory check feature.
 */
data class InventoryCheckState(
    val isPermitted: Boolean = false,
    val isEnabled: Boolean = false
)

/**
 * State for sign-in process status.
 */
data class SignInProcessState(
    val isInProgress: Boolean = false,
    val hasError: Boolean = false,
    val isProblem: Boolean = false
)

/**
 * Composite UI state that combines all StartScreen-related states.
 * This provides better state isolation while maintaining compatibility.
 */
data class CompositeStartUiState(
    val scannedCode: ScannedCodeState = ScannedCodeState(),
    val deviceInfo: DeviceInfoState = DeviceInfoState(),
    val inventoryCheck: InventoryCheckState = InventoryCheckState(),
    val signInProcess: SignInProcessState = SignInProcessState()
) {
    /**
     * Convert to legacy StartUiState for backward compatibility.
     */
    fun toLegacyState(): StartUiState = StartUiState(
        scannedCode = scannedCode.code,
        shortenedDeviceId = deviceInfo.shortenedDeviceId,
        deviceName = deviceInfo.deviceName,
        appVersion = deviceInfo.appVersion,
        appVersionCode = deviceInfo.appVersionCode,
        isInventoryCheckPermitted = inventoryCheck.isPermitted,
        isInventoryCheckEnabled = inventoryCheck.isEnabled,
        isSigningIn = signInProcess.isInProgress,
        isSignInProblem = signInProcess.isProblem,
        isFormatError = scannedCode.isFormatError,
        isSignInError = signInProcess.hasError
    )

    companion object {
        /**
         * Create from legacy StartUiState for migration purposes.
         */
        fun fromLegacyState(legacy: StartUiState): CompositeStartUiState = CompositeStartUiState(
            scannedCode = ScannedCodeState(
                code = legacy.scannedCode,
                isFormatError = legacy.isFormatError
            ),
            deviceInfo = DeviceInfoState(
                shortenedDeviceId = legacy.shortenedDeviceId,
                deviceName = legacy.deviceName,
                appVersion = legacy.appVersion,
                appVersionCode = legacy.appVersionCode
            ),
            inventoryCheck = InventoryCheckState(
                isPermitted = legacy.isInventoryCheckPermitted,
                isEnabled = legacy.isInventoryCheckEnabled
            ),
            signInProcess = SignInProcessState(
                isInProgress = legacy.isSigningIn,
                hasError = legacy.isSignInError,
                isProblem = legacy.isSignInProblem
            )
        )
    }
}
