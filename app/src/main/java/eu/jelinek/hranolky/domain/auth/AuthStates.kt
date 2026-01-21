package eu.jelinek.hranolky.domain.auth

/**
 * Isolated authentication states following Separation of States (SoS) principle.
 */

/**
 * Represents the authentication status of the current user.
 */
sealed class AuthenticationStatus {
    /** User is not signed in */
    data object SignedOut : AuthenticationStatus()

    /** User is signed in with Google */
    data class SignedIn(
        val email: String,
        val displayName: String?
    ) : AuthenticationStatus()
}

/**
 * Represents the state of a sign-in operation.
 */
sealed class SignInOperation {
    /** No sign-in operation in progress */
    data object Idle : SignInOperation()

    /** Sign-in operation is in progress */
    data object InProgress : SignInOperation()

    /** Sign-in operation completed successfully */
    data object Success : SignInOperation()

    /** Sign-in operation was cancelled by user */
    data object Cancelled : SignInOperation()

    /** Sign-in operation failed */
    data class Failed(val error: AuthError) : SignInOperation()
}

/**
 * Types of authentication errors.
 */
sealed class AuthError {
    data object NoCredentials : AuthError()
    data object Cancelled : AuthError()
    data class NetworkError(val message: String) : AuthError()
    data class FirebaseError(val message: String) : AuthError()
    data class UnknownError(val message: String) : AuthError()
}

/**
 * Composite state that combines all auth-related states.
 * This maintains backward compatibility while providing better state isolation.
 */
data class CompositeAuthState(
    val status: AuthenticationStatus = AuthenticationStatus.SignedOut,
    val operation: SignInOperation = SignInOperation.Idle
) {
    /**
     * Convert to legacy AuthState for backward compatibility.
     */
    fun toLegacyState(): AuthState {
        val (isSignedIn, email, name) = when (val s = status) {
            is AuthenticationStatus.SignedIn -> Triple(true, s.email, s.displayName)
            AuthenticationStatus.SignedOut -> Triple(false, null, null)
        }

        val isLoading = operation is SignInOperation.InProgress

        val error = when (val op = operation) {
            is SignInOperation.Failed -> when (val e = op.error) {
                is AuthError.NoCredentials -> "Žádné přihlašovací údaje"
                is AuthError.Cancelled -> null // Not an error
                is AuthError.NetworkError -> e.message
                is AuthError.FirebaseError -> e.message
                is AuthError.UnknownError -> e.message
            }
            else -> null
        }

        return AuthState(
            isSignedIn = isSignedIn,
            userEmail = email,
            userName = name,
            isLoading = isLoading,
            error = error
        )
    }

    companion object {
        /**
         * Create from legacy AuthState for migration purposes.
         */
        fun fromLegacyState(legacy: AuthState): CompositeAuthState {
            val status = if (legacy.isSignedIn && legacy.userEmail != null) {
                AuthenticationStatus.SignedIn(
                    email = legacy.userEmail,
                    displayName = legacy.userName
                )
            } else {
                AuthenticationStatus.SignedOut
            }

            val operation = when {
                legacy.isLoading -> SignInOperation.InProgress
                legacy.error != null -> SignInOperation.Failed(AuthError.UnknownError(legacy.error))
                else -> SignInOperation.Idle
            }

            return CompositeAuthState(
                status = status,
                operation = operation
            )
        }
    }
}

/**
 * Legacy AuthState kept for backward compatibility.
 * New code should prefer using CompositeAuthState with isolated states.
 */
data class AuthState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val userName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
