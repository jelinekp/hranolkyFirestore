package eu.jelinek.hranolky.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import eu.jelinek.hranolky.navigation.HranolkyNavHost
import eu.jelinek.hranolky.ui.theme.HranolkyFirestoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            HranolkyFirestoreTheme {
                HranolkyNavHost()
            }
        }
    }
}