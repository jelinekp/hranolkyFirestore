package eu.jelinek.hranolky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import eu.jelinek.hranolky.di.coreModule
import eu.jelinek.hranolky.navigation.HranolkyNavHost
import eu.jelinek.hranolky.ui.di.koinModule
import eu.jelinek.hranolky.ui.theme.HranolkyFirestoreTheme
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startKoin {
            androidContext(this@MainActivity)
            modules(koinModule, coreModule) // Your Koin modules
        }
        enableEdgeToEdge()
        setContent {
            HranolkyFirestoreTheme {
                HranolkyNavHost()
            }
        }
    }
}