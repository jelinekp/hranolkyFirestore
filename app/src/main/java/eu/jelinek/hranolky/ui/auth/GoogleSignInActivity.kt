package eu.jelinek.hranolky.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import eu.jelinek.hranolky.domain.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Dedicated Activity for handling Google Sign-In on Android 7 and below.
 * This is required because legacy GoogleSignInClient needs startActivityForResult.
 *
 * The activity is transparent and finishes immediately after handling the sign-in.
 */
@Suppress("DEPRECATION") // Legacy GoogleSignInClient is deprecated but needed for Android 7 support
class GoogleSignInActivity : ComponentActivity() {
    private val TAG = "GoogleSignInActivity"
    private lateinit var googleSignInClient: GoogleSignInClient
    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val RC_SIGN_IN = 9001
        const val EXTRA_RESULT_SUCCESS = "result_success"
        const val EXTRA_RESULT_ERROR = "result_error"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "GoogleSignInActivity created")

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(AuthManager.WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Start sign-in immediately
        startSignIn()
    }

    private fun startSignIn() {
        Log.d(TAG, "Starting legacy Google Sign-In flow")
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "Sign-in result received: resultCode=$resultCode")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "Google sign-in succeeded: ${account.email}")

            // Sign in to Firebase with the Google account
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    firebaseAuthWithGoogle(account.idToken!!)

                    // Success - finish with success result
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_RESULT_SUCCESS, true)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase authentication failed", e)
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_RESULT_ERROR, "Firebase přihlášení selhalo: ${e.message}")
                    }
                    setResult(Activity.RESULT_CANCELED, resultIntent)
                    finish()
                }
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign-in failed with code: ${e.statusCode}", e)

            val errorMessage = when (e.statusCode) {
                12501 -> null // User cancelled - don't show error
                else -> "Přihlášení Google selhalo (${e.statusCode}): ${e.message}"
            }

            val resultIntent = Intent().apply {
                errorMessage?.let { putExtra(EXTRA_RESULT_ERROR, it) }
            }
            setResult(Activity.RESULT_CANCELED, resultIntent)
            finish()
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        Log.d(TAG, "Firebase auth successful")
    }
}

