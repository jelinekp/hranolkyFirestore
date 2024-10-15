package eu.jelinek.hranolky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { HranolkyTopBar() }
                ) { innerPadding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)) {
                        HranolkyNavHost()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HranolkyTopBar(modifier: Modifier = Modifier) {
    TopAppBar(
        title = { Text("Hranolky") },
        modifier = modifier
    )
}