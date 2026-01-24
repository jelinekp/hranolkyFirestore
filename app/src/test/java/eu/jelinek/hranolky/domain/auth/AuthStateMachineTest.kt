package eu.jelinek.hranolky.domain.auth

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthStateMachineTest {

    private lateinit var stateMachine: AuthStateMachine

    @Before
    fun setup() {
        stateMachine = AuthStateMachine()
    }

    // Initial state tests

    @Test
    fun `initial state is Unknown`() = runTest {
        assertEquals(AuthFlowState.Unknown, stateMachine.state.first())
    }

    // CheckAuth transitions

    @Test
    fun `CheckAuth from Unknown transitions to Checking`() {
        val result = stateMachine.transition(AuthEvent.CheckAuth)

        assertTrue(result.isSuccess)
        assertEquals(AuthFlowState.Checking, result.getOrNull())
    }

    @Test
    fun `CheckAuth from Authenticated is not allowed`() {
        stateMachine.transition(AuthEvent.CheckAuth)
        stateMachine.transition(AuthEvent.UserAuthenticated("uid", "email", "name"))

        val result = stateMachine.transition(AuthEvent.CheckAuth)

        assertTrue(result.isFailure)
    }

    // UserAuthenticated transitions

    @Test
    fun `UserAuthenticated from Checking transitions to Authenticated`() {
        stateMachine.transition(AuthEvent.CheckAuth)

        val result = stateMachine.transition(
            AuthEvent.UserAuthenticated("uid123", "test@test.com", "Test User")
        )

        assertTrue(result.isSuccess)
        val state = result.getOrNull() as AuthFlowState.Authenticated
        assertEquals("uid123", state.userId)
        assertEquals("test@test.com", state.email)
        assertEquals("Test User", state.displayName)
    }

    @Test
    fun `UserAuthenticated from Unknown is not allowed`() {
        val result = stateMachine.transition(
            AuthEvent.UserAuthenticated("uid", "email", "name")
        )

        assertTrue(result.isFailure)
    }

    // UserNotAuthenticated transitions

    @Test
    fun `UserNotAuthenticated from Checking transitions to NotAuthenticated`() {
        stateMachine.transition(AuthEvent.CheckAuth)

        val result = stateMachine.transition(AuthEvent.UserNotAuthenticated)

        assertTrue(result.isSuccess)
        assertEquals(AuthFlowState.NotAuthenticated, result.getOrNull())
    }

    // StartSignIn transitions

    @Test
    fun `StartSignIn from NotAuthenticated transitions to SigningIn`() {
        stateMachine.transition(AuthEvent.CheckAuth)
        stateMachine.transition(AuthEvent.UserNotAuthenticated)

        val result = stateMachine.transition(
            AuthEvent.StartSignIn(SignInMethod.GOOGLE_CREDENTIAL_MANAGER)
        )

        assertTrue(result.isSuccess)
        val state = result.getOrNull() as AuthFlowState.SigningIn
        assertEquals(SignInMethod.GOOGLE_CREDENTIAL_MANAGER, state.method)
    }

    @Test
    fun `StartSignIn from Authenticated is not allowed`() {
        stateMachine.transition(AuthEvent.CheckAuth)
        stateMachine.transition(AuthEvent.UserAuthenticated("uid", "email", "name"))

        val result = stateMachine.transition(
            AuthEvent.StartSignIn(SignInMethod.GOOGLE_LEGACY)
        )

        assertTrue(result.isFailure)
    }

    // SignInSucceeded transitions

    @Test
    fun `SignInSucceeded from SigningIn transitions to Authenticated`() {
        stateMachine.transition(AuthEvent.CheckAuth)
        stateMachine.transition(AuthEvent.UserNotAuthenticated)
        stateMachine.transition(AuthEvent.StartSignIn(SignInMethod.GOOGLE_CREDENTIAL_MANAGER))

        val result = stateMachine.transition(
            AuthEvent.SignInSucceeded("uid456", "user@example.com", "User Name")
        )

        assertTrue(result.isSuccess)
        val state = result.getOrNull() as AuthFlowState.Authenticated
        assertEquals("uid456", state.userId)
    }

    // SignInCancelled transitions

    @Test
    fun `SignInCancelled from SigningIn transitions to NotAuthenticated`() {
        stateMachine.transition(AuthEvent.CheckAuth)
        stateMachine.transition(AuthEvent.UserNotAuthenticated)
        stateMachine.transition(AuthEvent.StartSignIn(SignInMethod.GOOGLE_CREDENTIAL_MANAGER))

        val result = stateMachine.transition(AuthEvent.SignInCancelled)

        assertTrue(result.isSuccess)
        assertEquals(AuthFlowState.NotAuthenticated, result.getOrNull())
    }

    // StartSignOut transitions

    @Test
    fun `StartSignOut from Authenticated transitions to SigningOut`() {
        stateMachine.transition(AuthEvent.CheckAuth)
        stateMachine.transition(AuthEvent.UserAuthenticated("uid", "email", "name"))

        val result = stateMachine.transition(AuthEvent.StartSignOut)

        assertTrue(result.isSuccess)
        assertEquals(AuthFlowState.SigningOut, result.getOrNull())
    }

    @Test
    fun `StartSignOut from NotAuthenticated is not allowed`() {
        stateMachine.transition(AuthEvent.CheckAuth)
        stateMachine.transition(AuthEvent.UserNotAuthenticated)

        val result = stateMachine.transition(AuthEvent.StartSignOut)

        assertTrue(result.isFailure)
    }

    // SignOutCompleted transitions

    @Test
    fun `SignOutCompleted from SigningOut transitions to NotAuthenticated`() {
        stateMachine.transition(AuthEvent.CheckAuth)
        stateMachine.transition(AuthEvent.UserAuthenticated("uid", "email", "name"))
        stateMachine.transition(AuthEvent.StartSignOut)

        val result = stateMachine.transition(AuthEvent.SignOutCompleted)

        assertTrue(result.isSuccess)
        assertEquals(AuthFlowState.NotAuthenticated, result.getOrNull())
    }

    // Error transitions

    @Test
    fun `ErrorOccurred from Checking transitions to Error`() {
        stateMachine.transition(AuthEvent.CheckAuth)

        val result = stateMachine.transition(
            AuthEvent.ErrorOccurred(AuthPhase.CHECK, "Network error", true)
        )

        assertTrue(result.isSuccess)
        val state = result.getOrNull() as AuthFlowState.Error
        assertEquals(AuthPhase.CHECK, state.phase)
        assertEquals("Network error", state.message)
        assertTrue(state.recoverable)
    }

    @Test
    fun `ErrorOccurred from Unknown is not allowed`() {
        val result = stateMachine.transition(
            AuthEvent.ErrorOccurred(AuthPhase.CHECK, "Error", true)
        )

        assertTrue(result.isFailure)
    }

    // Reset transitions

    @Test
    fun `Reset from recoverable Error transitions to Unknown`() {
        stateMachine.transition(AuthEvent.CheckAuth)
        stateMachine.transition(
            AuthEvent.ErrorOccurred(AuthPhase.CHECK, "Error", recoverable = true)
        )

        val result = stateMachine.transition(AuthEvent.Reset)

        assertTrue(result.isSuccess)
        assertEquals(AuthFlowState.Unknown, result.getOrNull())
    }

    @Test
    fun `Reset from non-recoverable Error is not allowed`() {
        stateMachine.transition(AuthEvent.CheckAuth)
        stateMachine.transition(
            AuthEvent.ErrorOccurred(AuthPhase.CHECK, "Fatal error", recoverable = false)
        )

        val result = stateMachine.transition(AuthEvent.Reset)

        assertTrue(result.isFailure)
    }

    // canHandle tests

    @Test
    fun `canHandle returns true for valid transition`() {
        assertTrue(stateMachine.canHandle(AuthEvent.CheckAuth))
    }

    @Test
    fun `canHandle returns false for invalid transition`() {
        assertFalse(stateMachine.canHandle(AuthEvent.UserAuthenticated("uid", null, null)))
    }

    // History tests

    @Test
    fun `history records transitions`() {
        stateMachine.transition(AuthEvent.CheckAuth)
        stateMachine.transition(AuthEvent.UserNotAuthenticated)

        val history = stateMachine.history

        assertEquals(2, history.size)
        assertEquals(AuthFlowState.Unknown, history[0].fromState)
        assertEquals(AuthFlowState.Checking, history[0].toState)
        assertEquals(AuthFlowState.Checking, history[1].fromState)
        assertEquals(AuthFlowState.NotAuthenticated, history[1].toState)
    }

    @Test
    fun `history is bounded to maxHistorySize`() {
        val machine = AuthStateMachine(maxHistorySize = 2)

        machine.transition(AuthEvent.CheckAuth)
        machine.transition(AuthEvent.UserNotAuthenticated)
        machine.transition(AuthEvent.StartSignIn(SignInMethod.GOOGLE_LEGACY))

        assertEquals(2, machine.history.size)
        // Oldest entry should be removed
        assertEquals(AuthFlowState.Checking, machine.history[0].fromState)
    }

    @Test
    fun `clearHistory removes all entries`() {
        stateMachine.transition(AuthEvent.CheckAuth)
        stateMachine.transition(AuthEvent.UserNotAuthenticated)

        stateMachine.clearHistory()

        assertTrue(stateMachine.history.isEmpty())
    }

    // toCompositeAuthState conversion tests

    @Test
    fun `Unknown converts to CompositeAuthState with SignedOut status`() {
        val composite = AuthFlowState.Unknown.toCompositeAuthState()

        assertEquals(AuthenticationStatus.SignedOut, composite.status)
    }

    @Test
    fun `Authenticated converts to CompositeAuthState with SignedIn status`() {
        val state = AuthFlowState.Authenticated("uid", "email@test.com", "Name")
        val composite = state.toCompositeAuthState()

        val status = composite.status as AuthenticationStatus.SignedIn
        assertEquals("email@test.com", status.email)
        assertEquals("Name", status.displayName)
    }

    @Test
    fun `Error converts to CompositeAuthState with error`() {
        val state = AuthFlowState.Error(AuthPhase.SIGN_IN, "Sign in failed", true)
        val composite = state.toCompositeAuthState()

        assertEquals(AuthenticationStatus.SignedOut, composite.status)
        assertTrue(composite.operation is SignInOperation.Failed)
    }
}
