package eu.jelinek.hranolky.ui.di

import eu.jelinek.hranolky.ui.overview.OverViewModel
import eu.jelinek.hranolky.ui.showlast.ShowLastActionsViewModel
import eu.jelinek.hranolky.ui.start.StartViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule get() = module {
    viewModel { ShowLastActionsViewModel(get(), get(), get()) }
    viewModel { StartViewModel(get(), get(), get()) }
    viewModel { OverViewModel(get()) }
}