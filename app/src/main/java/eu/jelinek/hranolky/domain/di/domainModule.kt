package eu.jelinek.hranolky.domain.di

import com.google.firebase.auth.FirebaseAuth
import eu.jelinek.hranolky.domain.AddSlotActionUseCase
import eu.jelinek.hranolky.domain.AuthManager
import eu.jelinek.hranolky.domain.CheckInventoryStatusUseCase
import eu.jelinek.hranolky.domain.DeviceManager
import eu.jelinek.hranolky.domain.InputValidator
import eu.jelinek.hranolky.domain.QuantityParser
import eu.jelinek.hranolky.domain.UndoSlotActionUseCase
import eu.jelinek.hranolky.domain.UpdateManager
import org.koin.dsl.module

val domainModule = module {
    single { FirebaseAuth.getInstance() }
    single { AuthManager(get()) }
    factory { AddSlotActionUseCase(get(), get(), get()) }
    factory { UndoSlotActionUseCase(get()) }
    single { DeviceManager(get()) }
    single { UpdateManager(get()) }
    single { InputValidator() }
    single { QuantityParser() }
    single { CheckInventoryStatusUseCase() }
}