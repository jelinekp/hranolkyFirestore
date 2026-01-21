package eu.jelinek.hranolky.domain.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State machine for authentication flow.
 *
 * Implements Separation of States (SoS) by defining clear state transitions
 * and ensuring only valid transitions can occur.
 *
 * State Flow:
 * ```
 * ┌───────────┐       check()       ┌──────────────┐
 * │  Unknown  │ ─────────────────> │   Checking   │
 * └───────────┘                    └──────────────┘
 *                                         │
 *                          ┌──────────────┼──────────────┐
 *                          │              │              │
 *                   authenticated()  notAuthenticated() error()
 *                          │              │              │
 *                          v              v              v
 *                   ┌─────────────┐ ┌──────────────┐ ┌─────────┐
 *                   │Authenticated│ │NotAuthenticated│ │  Error  │
 *                   └─────────────┘ └──────────────┘ └─────────┘
 *                                          │
 *                                    signIn()
 *                                          │
 *                                          v
 *                                  ┌───────────────┐
 *                                  │  SigningIn    │
 *                                  └───────────────┘
 *                                          │
 *                          ┌───────────────┼───────────────┐
 *                          │               │               │
 *                    success()         cancelled()      error()
 *                          │               │               │
 *                          v               v               v
 *                   ┌─────────────┐ ┌──────────────┐ ┌─────────┐
 *                   │Authenticated│ │NotAuthenticated│ │  Error  │
 *                   └─────────────┘ └──────────────┘ └─────────┘
 * ```
 */
sealed class AuthFlowState {

    /** Initial state - authentication status unknown */
    data object Unknown : AuthFlowState()

    /** Checking current authentication status */
    data object Checking : AuthFlowState()

    /** User is not authenticated */
    data object NotAuthenticated : AuthFlowState()

    /** Sign-in in progress */
    data class SigningIn(
        val method: SignInMethod
    ) : AuthFlowState()

    /** User is authenticated */
    data class Authenticated(
        val userId: String,
        val email: String?,
        val displayName: String?
    ) : AuthFlowState()

    /** Authentication error occurred */
    data class Error(
        val phase: AuthPhase,
        val message: String,
        val recoverable: Boolean = true
    ) : AuthFlowState()

    /** Signing out in progress */
    data object SigningOut : AuthFlowState()
}

/**
 * Sign-in methods supported
 */
enum class SignInMethod {
    GOOGLE_CREDENTIAL_MANAGER,
    GOOGLE_LEGACY,
    ANONYMOUS
}

/**
 * Phases of authentication (for error reporting)
 */
enum class AuthPhase {
    CHECK,
    SIGN_IN,
    SIGN_OUT
}

/**
 * Events that trigger state transitions
 */
sealed class AuthEvent {
    /** Start checking authentication status */
    data object CheckAuth : AuthEvent()

    /** User is authenticated */
    data class UserAuthenticated(
        val userId: String,
        val email: String?,
        val displayName: String?
    ) : AuthEvent()

    /** User is not authenticated */
    data object UserNotAuthenticated : AuthEvent()

    /** Start sign-in process */
    data class StartSignIn(val method: SignInMethod) : AuthEvent()

    /** Sign-in succeeded */
    data class SignInSucceeded(
        val userId: String,
        val email: String?,
        val displayName: String?
    ) : AuthEvent()

    /** Sign-in was cancelled by user */
    data object SignInCancelled : AuthEvent()

    /** Start sign-out process */
    data object StartSignOut : AuthEvent()

    /** Sign-out completed */
    data object SignOutCompleted : AuthEvent()

    /** Error occurred */
    data class ErrorOccurred(
        val phase: AuthPhase,
        val message: String,
        val recoverable: Boolean = true
    ) : AuthEvent()

    /** Reset from error state */
    data object Reset : AuthEvent()
}

/**
 * Authentication state machine that validates transitions.
 */
class AuthStateMachine(
    private val maxHistorySize: Int = 10
) {
    private val _state = MutableStateFlow<AuthFlowState>(AuthFlowState.Unknown)
    val state: StateFlow<AuthFlowState> = _state.asStateFlow()

    private val _history = mutableListOf<AuthStateHistoryEntry>()
    val history: List<AuthStateHistoryEntry> get() = _history.toList()

    /**
     * Check if an event can be handled in current state.
     */
    fun canHandle(event: AuthEvent): Boolean {
        val current = _state.value
        return when (event) {
            is AuthEvent.CheckAuth ->
                current is AuthFlowState.Unknown ||
                current is AuthFlowState.Error

            is AuthEvent.UserAuthenticated ->
                current is AuthFlowState.Checking

            is AuthEvent.UserNotAuthenticated ->
                current is AuthFlowState.Checking

            is AuthEvent.StartSignIn ->
                current is AuthFlowState.NotAuthenticated ||
                current is AuthFlowState.Error

            is AuthEvent.SignInSucceeded ->
                current is AuthFlowState.SigningIn

            is AuthEvent.SignInCancelled ->
                current is AuthFlowState.SigningIn

            is AuthEvent.StartSignOut ->
                current is AuthFlowState.Authenticated

            is AuthEvent.SignOutCompleted ->
                current is AuthFlowState.SigningOut

            is AuthEvent.ErrorOccurred ->
                current !is AuthFlowState.Unknown

            is AuthEvent.Reset ->
                current is AuthFlowState.Error &&
                (current as AuthFlowState.Error).recoverable
        }
    }

    /**
     * Process an event and transition to new state.
     *
     * @return Result containing new state or failure if transition is invalid
     */
    fun transition(event: AuthEvent): Result<AuthFlowState> {
        if (!canHandle(event)) {
            return Result.failure(
                IllegalStateException("Cannot handle $event in state ${_state.value}")
            )
        }

        val newState = computeNewState(event)
        recordTransition(_state.value, newState, event)
        _state.value = newState

        return Result.success(newState)
    }

    private fun computeNewState(event: AuthEvent): AuthFlowState {
        return when (event) {
            is AuthEvent.CheckAuth -> AuthFlowState.Checking

            is AuthEvent.UserAuthenticated -> AuthFlowState.Authenticated(
                userId = event.userId,
                email = event.email,
                displayName = event.displayName
            )

            is AuthEvent.UserNotAuthenticated -> AuthFlowState.NotAuthenticated

            is AuthEvent.StartSignIn -> AuthFlowState.SigningIn(event.method)

            is AuthEvent.SignInSucceeded -> AuthFlowState.Authenticated(
                userId = event.userId,
                email = event.email,
                displayName = event.displayName
            )

            is AuthEvent.SignInCancelled -> AuthFlowState.NotAuthenticated

            is AuthEvent.StartSignOut -> AuthFlowState.SigningOut

            is AuthEvent.SignOutCompleted -> AuthFlowState.NotAuthenticated

            is AuthEvent.ErrorOccurred -> AuthFlowState.Error(
                phase = event.phase,
                message = event.message,
                recoverable = event.recoverable
            )

            is AuthEvent.Reset -> AuthFlowState.Unknown
        }
    }

    private fun recordTransition(from: AuthFlowState, to: AuthFlowState, event: AuthEvent) {
        _history.add(
            AuthStateHistoryEntry(
                fromState = from,
                toState = to,
                event = event,
                timestamp = System.currentTimeMillis()
            )
        )

        // Keep history bounded
        while (_history.size > maxHistorySize) {
            _history.removeAt(0)
        }
    }

    /**
     * Clear state history.
     */
    fun clearHistory() {
        _history.clear()
    }

    /**
     * Reset to initial state (for testing).
     */
    fun reset() {
        _state.value = AuthFlowState.Unknown
        _history.clear()
    }
}

/**
 * Entry in the state history.
 */
data class AuthStateHistoryEntry(
    val fromState: AuthFlowState,
    val toState: AuthFlowState,
    val event: AuthEvent,
    val timestamp: Long
)

/**
 * Extension to convert AuthFlowState to legacy CompositeAuthState
 */
fun AuthFlowState.toCompositeAuthState(): CompositeAuthState = when (this) {
    is AuthFlowState.Unknown -> CompositeAuthState(
        status = AuthenticationStatus.SignedOut,
        operation = SignInOperation.Idle
    )
    is AuthFlowState.Checking -> CompositeAuthState(
        status = AuthenticationStatus.SignedOut,
        operation = SignInOperation.InProgress
    )
    is AuthFlowState.NotAuthenticated -> CompositeAuthState(
        status = AuthenticationStatus.SignedOut,
        operation = SignInOperation.Idle
    )
    is AuthFlowState.SigningIn -> CompositeAuthState(
        status = AuthenticationStatus.SignedOut,
        operation = SignInOperation.InProgress
    )
    is AuthFlowState.Authenticated -> CompositeAuthState(
        status = AuthenticationStatus.SignedIn(
            email = email ?: "",
            displayName = displayName
        ),
        operation = SignInOperation.Success
    )
    is AuthFlowState.Error -> CompositeAuthState(
        status = AuthenticationStatus.SignedOut,
        operation = SignInOperation.Failed(
            error = AuthError.UnknownError(message)
        )
    )
    is AuthFlowState.SigningOut -> CompositeAuthState(
        status = AuthenticationStatus.SignedOut,
        operation = SignInOperation.InProgress
    )
}
