package eu.jelinek.hranolky.data.di

import SlotRepositoryImpl
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import eu.jelinek.hranolky.data.KtorSheetDbRepository
import eu.jelinek.hranolky.data.SheetDbRepository
import eu.jelinek.hranolky.data.SlotRepository
import org.koin.dsl.module

val dataModule = module {
    single<SlotRepository> { SlotRepositoryImpl(get()) }
    single<SheetDbRepository> { KtorSheetDbRepository(get()) }
    single { Firebase.auth }
}