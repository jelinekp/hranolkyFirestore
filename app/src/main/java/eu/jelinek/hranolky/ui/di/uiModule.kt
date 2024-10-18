package eu.jelinek.hranolky.ui.di

import eu.jelinek.hranolky.ui.overview.OverViewModel
import eu.jelinek.hranolky.ui.showlast.ShowLastActionsViewModel
import eu.jelinek.hranolky.ui.start.StartViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val uiModule get() = module {
    viewModelOf(::ShowLastActionsViewModel)
    viewModelOf(::StartViewModel)
    viewModelOf(::OverViewModel)
}