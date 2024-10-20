package eu.jelinek.hranolky.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.navigation.HranolkyNavHost
import eu.jelinek.hranolky.ui.shared.ScreenSize
import eu.jelinek.hranolky.ui.theme.HranolkyFirestoreTheme

class MainActivity : ComponentActivity() {
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
    }
}