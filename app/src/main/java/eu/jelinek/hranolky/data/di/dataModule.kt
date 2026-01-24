package eu.jelinek.hranolky.data.di

import SlotRepositoryImpl
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import eu.jelinek.hranolky.data.AppConfigRepository
import eu.jelinek.hranolky.data.AppConfigRepositoryImpl
import eu.jelinek.hranolky.data.DeviceRepository
import eu.jelinek.hranolky.data.DeviceRepositoryImpl
import eu.jelinek.hranolky.data.KtorSheetDbRepository
import eu.jelinek.hranolky.data.SheetDbRepository
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.data.config.RemoteConfigProvider
import eu.jelinek.hranolky.data.preferences.InventoryCheckPreferencesRepository
import eu.jelinek.hranolky.data.preferences.InventoryCheckPreferencesRepositoryImpl
import eu.jelinek.hranolky.domain.config.ConfigCache
import eu.jelinek.hranolky.domain.config.ConfigProvider
import org.koin.dsl.module

val dataModule = module {
    single<SlotRepository> { SlotRepositoryImpl(get()) }
    single<DeviceRepository> { DeviceRepositoryImpl(get()) }
    single<AppConfigRepository> { AppConfigRepositoryImpl(get()) }
    single<SheetDbRepository> { KtorSheetDbRepository(get()) }
    single<InventoryCheckPreferencesRepository> { InventoryCheckPreferencesRepositoryImpl(get()) }
    single { ConfigCache() }
    single<ConfigProvider> { RemoteConfigProvider(get(), get()) }
    single { Firebase.auth }
}