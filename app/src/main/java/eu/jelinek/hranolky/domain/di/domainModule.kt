package eu.jelinek.hranolky.domain.di

import eu.jelinek.hranolky.domain.AddSlotActionUseCase
import eu.jelinek.hranolky.domain.DeviceManager
import eu.jelinek.hranolky.domain.InputValidator
import eu.jelinek.hranolky.domain.UpdateManager
import org.koin.dsl.module

val domainModule = module {
    factory { AddSlotActionUseCase(get(), get(), get()) }
    single { DeviceManager(get()) }
    single { UpdateManager(get()) }
    single { InputValidator() }
}