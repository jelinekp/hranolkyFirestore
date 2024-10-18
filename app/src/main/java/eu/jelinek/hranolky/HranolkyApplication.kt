package eu.jelinek.hranolky

import android.app.Application
import eu.jelinek.hranolky.di.coreModule
import eu.jelinek.hranolky.ui.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HranolkyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HranolkyApplication)
            modules(coreModule, uiModule)
        }
    }
}