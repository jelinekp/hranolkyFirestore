package eu.jelinek.hranolky

import android.app.Application
import com.google.firebase.crashlytics.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import eu.jelinek.hranolky.data.di.dataModule
import eu.jelinek.hranolky.di.coreModule
import eu.jelinek.hranolky.domain.config.ConfigInitializer
import eu.jelinek.hranolky.domain.di.domainModule
import eu.jelinek.hranolky.ui.di.uiModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HranolkyApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Disable Crashlytics in debug builds to avoid polluting production data
        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        } catch (@Suppress("UNUSED_PARAMETER") e: IllegalStateException) {
            // Firebase not initialized - likely in unit tests
        }

        // Only start Koin if it's not already started (for tests)
        if (org.koin.core.context.GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@HranolkyApplication)
                modules(coreModule, uiModule, dataModule, domainModule)
            }
        }

        // Initialize remote configuration (non-blocking)
        initializeRemoteConfig()
    }

    private fun initializeRemoteConfig() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val configInitializer: ConfigInitializer = org.koin.core.context.GlobalContext.get().get()
                configInitializer.initialize()
            } catch (e: Exception) {
                // Config initialization is optional, continue with defaults
                // This may fail in tests where Firebase is not initialized
                android.util.Log.w("HranolkyApplication", "Config initialization skipped: ${e.message}")
            }
        }
    }
}