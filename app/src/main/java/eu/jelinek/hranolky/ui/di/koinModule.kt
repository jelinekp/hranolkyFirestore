package eu.jelinek.hranolky.ui.di

import eu.jelinek.hranolky.ui.showlast.ShowLastActionsViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val koinModule get() = module {
    viewModelOf(::ShowLastActionsViewModel)
}