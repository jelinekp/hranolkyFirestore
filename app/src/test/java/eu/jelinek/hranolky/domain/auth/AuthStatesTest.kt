package eu.jelinek.hranolky.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthStatesTest {

    // ======== AuthenticationStatus tests ========

    @Test
    fun `AuthenticationStatus SignedOut is the default state`() {
        val state = CompositeAuthState()
        assertTrue(state.status is AuthenticationStatus.SignedOut)
    }

    @Test
    fun `AuthenticationStatus SignedIn contains user info`() {
        val signedIn = AuthenticationStatus.SignedIn(
            email = "user@example.com",
            displayName = "Test User"
        )
        assertEquals("user@example.com", signedIn.email)
        assertEquals("Test User", signedIn.displayName)
    }

    @Test
    fun `AuthenticationStatus SignedIn allows null displayName`() {
        val signedIn = AuthenticationStatus.SignedIn(
            email = "user@example.com",
            displayName = null
        )
        assertEquals("user@example.com", signedIn.email)
        assertNull(signedIn.displayName)
    }

    // ======== SignInOperation tests ========

    @Test
    fun `SignInOperation Idle is the default state`() {
        val state = CompositeAuthState()
        assertTrue(state.operation is SignInOperation.Idle)
    }

    @Test
    fun `SignInOperation Failed contains error`() {
        val failed = SignInOperation.Failed(AuthError.NoCredentials)
        assertEquals(AuthError.NoCredentials, failed.error)
    }

    // ======== AuthError tests ========

    @Test
    fun `AuthError NetworkError contains message`() {
        val error = AuthError.NetworkError("Connection failed")
        assertEquals("Connection failed", error.message)
    }

    @Test
    fun `AuthError FirebaseError contains message`() {
        val error = AuthError.FirebaseError("Auth failed")
        assertEquals("Auth failed", error.message)
    }

    @Test
    fun `AuthError UnknownError contains message`() {
        val error = AuthError.UnknownError("Something went wrong")
        assertEquals("Something went wrong", error.message)
    }

    // ======== CompositeAuthState conversion tests ========

    @Test
    fun `toLegacyState converts SignedOut correctly`() {
        val composite = CompositeAuthState(
            status = AuthenticationStatus.SignedOut
        )
        val legacy = composite.toLegacyState()

        assertFalse(legacy.isSignedIn)
        assertNull(legacy.userEmail)
        assertNull(legacy.userName)
    }

    @Test
    fun `toLegacyState converts SignedIn correctly`() {
        val composite = CompositeAuthState(
            status = AuthenticationStatus.SignedIn(
                email = "test@example.com",
                displayName = "Test User"
            )
        )
        val legacy = composite.toLegacyState()

        assertTrue(legacy.isSignedIn)
        assertEquals("test@example.com", legacy.userEmail)
        assertEquals("Test User", legacy.userName)
    }

    @Test
    fun `toLegacyState converts InProgress correctly`() {
        val composite = CompositeAuthState(
            operation = SignInOperation.InProgress
        )
        val legacy = composite.toLegacyState()

        assertTrue(legacy.isLoading)
    }

    @Test
    fun `toLegacyState converts Failed with error correctly`() {
        val composite = CompositeAuthState(
            operation = SignInOperation.Failed(AuthError.NetworkError("Network error"))
        )
        val legacy = composite.toLegacyState()

        assertEquals("Network error", legacy.error)
    }

    @Test
    fun `toLegacyState converts Cancelled without error`() {
        val composite = CompositeAuthState(
            operation = SignInOperation.Failed(AuthError.Cancelled)
        )
        val legacy = composite.toLegacyState()

        // Cancelled should not be shown as an error to the user
        assertNull(legacy.error)
    }

    // ======== fromLegacyState conversion tests ========

    @Test
    fun `fromLegacyState converts signed in correctly`() {
        val legacy = AuthState(
            isSignedIn = true,
            userEmail = "user@test.com",
            userName = "User Name"
        )
        val composite = CompositeAuthState.fromLegacyState(legacy)

        assertTrue(composite.status is AuthenticationStatus.SignedIn)
        val signedIn = composite.status as AuthenticationStatus.SignedIn
        assertEquals("user@test.com", signedIn.email)
        assertEquals("User Name", signedIn.displayName)
    }

    @Test
    fun `fromLegacyState converts signed out correctly`() {
        val legacy = AuthState(
            isSignedIn = false
        )
        val composite = CompositeAuthState.fromLegacyState(legacy)

        assertTrue(composite.status is AuthenticationStatus.SignedOut)
    }

    @Test
    fun `fromLegacyState converts loading correctly`() {
        val legacy = AuthState(
            isLoading = true
        )
        val composite = CompositeAuthState.fromLegacyState(legacy)

        assertTrue(composite.operation is SignInOperation.InProgress)
    }

    @Test
    fun `fromLegacyState converts error correctly`() {
        val legacy = AuthState(
            error = "Something failed"
        )
        val composite = CompositeAuthState.fromLegacyState(legacy)

        assertTrue(composite.operation is SignInOperation.Failed)
        val failed = composite.operation as SignInOperation.Failed
        assertTrue(failed.error is AuthError.UnknownError)
    }

    @Test
    fun `fromLegacyState converts idle state correctly`() {
        val legacy = AuthState()
        val composite = CompositeAuthState.fromLegacyState(legacy)

        assertTrue(composite.status is AuthenticationStatus.SignedOut)
        assertTrue(composite.operation is SignInOperation.Idle)
    }
}
