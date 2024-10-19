package eu.jelinek.hranolky.ui.overview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.jelinek.hranolky.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun OverviewScreen(
    modifier: Modifier = Modifier,
    navigateUp: () -> Unit,
    navigateToShowLastActions: (String) -> Unit,
    viewModel: OverViewModel = koinViewModel()
) {
    val screenState by viewModel.overviewScreenState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { OverviewTopBar(navigateUp) }
    ) { paddingValues ->
        Row( // on tablet we want to have the filter area on the left side
            modifier = Modifier.padding(paddingValues)
        ) {
            FilterSlots(
                modifier = Modifier.weight(3f),
                selectedFilters = screenState.selectedFilters,
                onQualityFilterChange = { viewModel.onQualityFilterChange(it) },
                onThicknessFilterChange = { viewModel.onThicknessFilterChange(it) },
                onWidthFilterChange = { viewModel.onWidthFilterChange(it) },
                onLengthFilterChange = { viewModel.onLengthFilterChange(it) },
            )
            AllSlotsContent(screenState.allSlots, screenState.sum, navigateToShowLastActions, modifier = Modifier.weight(7f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewTopBar(
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text("Přehled hranolků na skladě")
        },
        navigationIcon = {
            IconButton(onClick = {
                navigateUp()
            }) {
                Icon(
                    Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back_icon)
                )
            }
        },
        modifier = modifier
    )
}
