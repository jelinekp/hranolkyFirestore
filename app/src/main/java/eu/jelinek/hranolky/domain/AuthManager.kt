package eu.jelinek.hranolky.domain

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages Google Sign-In authentication with Firebase.
 * Uses the modern Credential Manager API for sign-in.
 *
 * Authentication state is persisted by Firebase and survives app restarts/updates.
 * One Google account can be used on multiple devices.
 * Works on devices with no Google account - allows adding account during sign-in.
 */
class AuthManager(
    private val auth: FirebaseAuth
) {
    private val TAG = "AuthManager"

    // Web Client ID from Firebase Console -> Authentication -> Sign-in method -> Google
    // To get this:
    // 1. Go to Firebase Console -> Authentication -> Sign-in method
    // 2. Enable "Google" provider
    // 3. Copy the "Web client ID" (NOT the Android client ID)
    // The Web Client ID looks like: 123456789-xxxx.apps.googleusercontent.com
    companion object {
        const val WEB_CLIENT_ID = "657740368257-25rd5mbgs9scckap948c8u4s4rtdlpku.apps.googleusercontent.com"
        const val LEGACY_SIGN_IN_REQUEST_CODE = 9001
    }

    private val _authState = MutableStateFlow(AuthState())
    val authState = _authState.asStateFlow()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    init {
        // Check if user is already signed in (persisted by Firebase)
        auth.currentUser?.let { user ->
            _authState.value = AuthState(
                isSignedIn = true,
                userEmail = user.email,
                userName = user.displayName,
                isLoading = false
            )
            Log.d(TAG, "User already signed in: ${user.email}")
        }

        // Add Firebase auth state listener to detect sign-in completion
        // This is crucial for legacy sign-in flow where auth happens asynchronously
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d(TAG, "Auth state changed: User signed in - ${user.email}")
                _authState.value = AuthState(
                    isSignedIn = true,
                    userEmail = user.email,
                    userName = user.displayName,
                    isLoading = false
                )
            } else {
                Log.d(TAG, "Auth state changed: User signed out")
                _authState.value = AuthState(isLoading = false)
            }
        }
    }

    /**
     * Initiates Google Sign-In using the Credential Manager API (Android 8+)
     * or shows an error for Android 7 (Credential Manager not fully supported).
     *
     * For production use on Android 7, you should implement legacy GoogleSignInClient.
     * For now, we show a helpful error message.
     */
    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        _authState.value = _authState.value.copy(isLoading = true, error = null)

        // Use legacy Google Sign-In Activity for Android 7 (API 27) and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            Log.d(TAG, "Using legacy Google Sign-In Activity for Android ${Build.VERSION.SDK_INT}")
            return signInWithLegacyActivity(context)
        }

        // Modern Credential Manager API for Android 8+
        val result = trySignInWithGoogleId(context)

        // If no credentials available, try with Sign In With Google button flow
        // This allows users to add a new Google account
        return if (result.isFailure && result.exceptionOrNull() is NoCredentialException) {
            Log.d(TAG, "No existing credentials, trying Sign In With Google flow")
            trySignInWithGoogleButton(context)
        } else {
            result
        }
    }

    /**
     * Sign in using legacy GoogleSignInClient through a dedicated Activity.
     * This is required for Android 7 and below where Credential Manager doesn't work.
     *
     * The Activity handles the full sign-in flow and updates Firebase Auth.
     * This method starts the activity and returns immediately - the auth state
     * will be updated asynchronously when sign-in completes via Firebase's auth listener.
     */
    private fun signInWithLegacyActivity(context: Context): Result<FirebaseUser> {
        try {
            // Launch GoogleSignInActivity which will handle the sign-in flow
            val intent = Intent(context, Class.forName("eu.jelinek.hranolky.ui.auth.GoogleSignInActivity"))

            if (context is Activity) {
                context.startActivityForResult(intent, LEGACY_SIGN_IN_REQUEST_CODE)
                Log.d(TAG, "Started legacy sign-in activity")
            } else {
                // If context is not an Activity, start as new task
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "Started legacy sign-in activity as new task")
            }

            // Sign-in will complete asynchronously in the Activity
            // Firebase auth state listener will automatically update the UI
            // Keep loading state until Firebase auth completes
            Log.d(TAG, "Legacy sign-in activity launched successfully - waiting for auth callback")

            // Return a dummy success - auth state managed by Firebase listener
            // We can't return the actual user yet because it's null until sign-in completes
            // The non-null assertion will throw, which we catch below
            return Result.success(auth.currentUser!!)
        } catch (e: NullPointerException) {
            // Expected - currentUser is null until sign-in completes
            // Firebase auth listener will update state when ready
            // Just return a failure but the auth flow continues in background
            Log.d(TAG, "Sign-in pending - waiting for Firebase auth callback")
            // We need to return something, but Result<FirebaseUser> requires non-null
            // So we return failure here but keep loading state
            // The Firebase listener will update to success when auth completes
            return Result.failure(Exception("Sign-in in progress"))
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "GoogleSignInActivity not found", e)
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Chyba konfigurace přihlášení"
            )
            return Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start legacy sign-in activity", e)
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Nepodařilo se spustit přihlášení: ${e.message}"
            )
            return Result.failure(e)
        }
    }

    /**
     * Try sign-in using GetGoogleIdOption - works when Google accounts exist on device
     */
    private suspend fun trySignInWithGoogleId(context: Context): Result<FirebaseUser> {
        return try {
            val credentialManager = CredentialManager.create(context)

            // Configure Google Sign-In options for existing accounts
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Show all accounts on device
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(true) // Auto-select if only one account
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            handleSignInResult(result)
        } catch (e: NoCredentialException) {
            Log.d(TAG, "No credentials available on device", e)
            _authState.value = _authState.value.copy(isLoading = false)
            Result.failure(e)
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Sign-in cancelled by user")
            _authState.value = _authState.value.copy(isLoading = false, error = null)
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential error", e)
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Chyba při přihlašování: ${e.message}"
            )
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Přihlášení selhalo: ${e.message}"
            )
            Result.failure(e)
        }
    }

    /**
     * Try sign-in using GetSignInWithGoogleOption - shows Google Sign-In button
     * This works even on devices with no Google account as it allows adding one
     */
    private suspend fun trySignInWithGoogleButton(context: Context): Result<FirebaseUser> {
        _authState.value = _authState.value.copy(isLoading = true, error = null)

        return try {
            val credentialManager = CredentialManager.create(context)

            // Use Sign In With Google option - shows the Google sign-in button
            // This allows users to sign in even without an existing account on device
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            handleSignInResult(result)
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Sign-in cancelled by user")
            _authState.value = _authState.value.copy(isLoading = false, error = null)
            Result.failure(e)
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No credentials available even with Google Sign-In flow", e)
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Nelze se přihlásit. Zkontrolujte připojení k internetu."
            )
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential error in Google Sign-In flow", e)
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Chyba při přihlašování: ${e.message}"
            )
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Přihlášení selhalo: ${e.message}"
            )
            Result.failure(e)
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse): Result<FirebaseUser> {
        val credential = result.credential

        return when {
            credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e(TAG, "Failed to parse Google ID token", e)
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Chyba při zpracování přihlášení"
                    )
                    Result.failure(e)
                }
            }
            else -> {
                Log.e(TAG, "Unexpected credential type: ${credential.type}")
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Neočekávaný typ přihlašovacích údajů"
                )
                Result.failure(Exception("Unexpected credential type"))
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user

            if (user != null) {
                Log.d(TAG, "Firebase auth successful: ${user.email}")
                _authState.value = AuthState(
                    isSignedIn = true,
                    userEmail = user.email,
                    userName = user.displayName,
                    isLoading = false
                )
                Result.success(user)
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Přihlášení selhalo"
                )
                Result.failure(Exception("Firebase auth returned null user"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase auth failed", e)
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Firebase přihlášení selhalo: ${e.message}"
            )
            Result.failure(e)
        }
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        auth.signOut()
        _authState.value = AuthState()
        Log.d(TAG, "User signed out")
    }

    /**
     * Reset loading state - used when sign-in is cancelled or fails before Firebase auth changes.
     */
    fun resetLoadingState() {
        _authState.value = _authState.value.copy(isLoading = false)
        Log.d(TAG, "Loading state reset")
    }

    /**
     * Refresh auth state - checks current Firebase user and updates state accordingly.
     * Used after legacy sign-in activity completes.
     */
    fun refreshAuthState() {
        val user = auth.currentUser
        if (user != null) {
            _authState.value = AuthState(
                isSignedIn = true,
                userEmail = user.email,
                userName = user.displayName,
                isLoading = false
            )
            Log.d(TAG, "Auth state refreshed: user signed in as ${user.email}")
        } else {
            _authState.value = AuthState(isLoading = false)
            Log.d(TAG, "Auth state refreshed: no user signed in")
        }
    }
}

data class AuthState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val userName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

