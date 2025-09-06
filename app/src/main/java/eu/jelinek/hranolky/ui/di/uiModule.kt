package eu.jelinek.hranolky.ui.di

import eu.jelinek.hranolky.ui.history.HistoryViewModel
import eu.jelinek.hranolky.ui.manageitem.ManageItemViewModel
import eu.jelinek.hranolky.ui.overview.OverViewModel
import eu.jelinek.hranolky.ui.start.StartViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule get() = module {
    viewModel { ManageItemViewModel(get(), get(), get(), get()) }
    viewModel { StartViewModel(get(), get()) }
    viewModel { OverViewModel(get()) }
    viewModel { HistoryViewModel(get(), get()) }
}