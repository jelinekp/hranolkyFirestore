package eu.jelinek.hranolky

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * Test-specific Application class that doesn't initialize Firebase.
 * Used by Robolectric tests to avoid Firebase initialization errors.
 */
class TestHranolkyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Stop any existing Koin instance to avoid "already started" errors
        try {
            stopKoin()
        } catch (_: Exception) {
            // Ignore if Koin wasn't started
        }

        // Start Koin with test-safe modules
        // Note: Tests that need specific mocks should stop Koin and configure their own
        try {
            startKoin {
                androidContext(this@TestHranolkyApplication)
                // Don't load modules that require Firebase
                // Tests should set up their own Koin modules as needed
            }
        } catch (_: Exception) {
            // Koin already started, that's fine
        }

        // Skip Firebase and remote config initialization entirely
        // Tests should mock their own dependencies
    }
}
