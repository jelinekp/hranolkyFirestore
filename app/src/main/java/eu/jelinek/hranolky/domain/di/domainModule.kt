package eu.jelinek.hranolky.domain.di

import eu.jelinek.hranolky.domain.AddSlotActionUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { AddSlotActionUseCase(get()) }
}