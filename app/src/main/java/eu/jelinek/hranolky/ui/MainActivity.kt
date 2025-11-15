package eu.jelinek.hranolky.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import eu.jelinek.hranolky.navigation.HranolkyNavHost
import eu.jelinek.hranolky.ui.shared.ScreenSize
import eu.jelinek.hranolky.ui.theme.HranolkyFirestoreTheme

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val screenHeight = configuration.screenHeightDp.dp
            val screenSize = if (screenWidth > 1000.dp) ScreenSize.TABLET else ScreenSize.PHONE
            HranolkyFirestoreTheme {
                HranolkyNavHost(
                    screenSize = screenSize,
                )
            }
        }

        auth = Firebase.auth

        // Check if user is already signed in
        if (auth.currentUser == null) {
            // Not signed in, so sign in anonymously
            signInAnonymously()
        } else {
            // User is already signed in
            Log.d("Auth", "User already signed in with UID: ${auth.currentUser?.uid}")
        }
    }

    private fun signInAnonymously() {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    val user = auth.currentUser
                    Log.d("Auth", "signInAnonymously:success. UID: ${user?.uid}")
                } else {
                    // If sign in fails
                    Log.w("Auth", "signInAnonymously:failure", task.exception)
                    // You might want to retry or handle the error
                }
            }
    }
}