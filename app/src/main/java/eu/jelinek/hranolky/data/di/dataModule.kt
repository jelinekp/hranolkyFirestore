package eu.jelinek.hranolky.data.di

import eu.jelinek.hranolky.data.KtorSheetDbRepository
import eu.jelinek.hranolky.data.SheetDbRepository
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.data.SlotRepositoryImpl
import org.koin.dsl.module

val dataModule = module {
    single<SlotRepository> { SlotRepositoryImpl(get()) }
    single<SheetDbRepository> { KtorSheetDbRepository(get()) }
}