package eu.jelinek.hranolky.domain.di

import eu.jelinek.hranolky.domain.AddSlotActionUseCase
import eu.jelinek.hranolky.domain.InputValidator
import org.koin.dsl.module

val domainModule = module {
    factory { AddSlotActionUseCase(get(), get()) }
    single { InputValidator() }
}