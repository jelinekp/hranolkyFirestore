package eu.jelinek.hranolky.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.model.SlotType
import eu.jelinek.hranolky.ui.history.TabHeader
import eu.jelinek.hranolky.ui.shared.ScreenSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel


@Composable
fun OverviewScreen(
    modifier: Modifier = Modifier,
    screenSize: ScreenSize,
    navigateUp: () -> Unit,
    navigateToManageItemScreen: (String) -> Unit,
    viewModel: OverViewModel = koinViewModel()
) {
    val screenState by viewModel.overviewScreenState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { SlotType.entries.size })
    val coroutineScope = rememberCoroutineScope()

    // Use LaunchedEffect to react to page changes
    LaunchedEffect(pagerState) {
        // snapshotFlow converts Composable state (pagerState.currentPage) into a Flow
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged() // Only react when the page actually changes
            .collect { pageIndex ->
                val newType = when (pageIndex) {
                    0 -> SlotType.Beam
                    1 -> SlotType.Jointer
                    else -> SlotType.Beam // Or handle error/default
                }
                viewModel.onTypeChange(newType)
            }
    }

    Scaffold(
        topBar = {
            OverviewTopBar(
                navigateToStart = navigateUp,
                screenSize = screenSize,
                pagerState = pagerState,
                coroutineScope = coroutineScope,
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        if (screenSize.isTablet()) {
            Row( // on tablet we want to have the filter area on the right side
                modifier = Modifier.padding(paddingValues)
            ) {
                AllSlotsContent(
                    slots = screenState.sortedSlots,
                    slotSum = screenState.sum,
                    sortingBy = screenState.sortingBy,
                    sortingDirection = screenState.sortingDirection,
                    updateSorting = { viewModel.updateSorting(it) },
                    navigateToManageItemScreen = navigateToManageItemScreen,
                    modifier = Modifier.weight(25f)
                )

                FilterSlots(
                    modifier = Modifier.weight(15f),
                    allFilters = screenState.allFilters,
                    selectedFilters = screenState.selectedFilters,
                    onQualityFilterChange = { viewModel.onQualityFilterChange(it) },
                    onThicknessFilterChange = { viewModel.onThicknessFilterChange(it) },
                    onWidthFilterChange = { viewModel.onWidthFilterChange(it) },
                    onLengthFilterChange = { viewModel.onLengthFilterChange(it) },
                    onFilterClear = { viewModel.onFilterClear() }
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(paddingValues),
            ) {
                FiltersAndTable(
                    screenState = screenState,
                    viewModel = viewModel,
                    navigateToManageItemScreen = navigateToManageItemScreen,
                    screenSize = screenSize,
                    loading = screenState.loading,
                )
            }
        }
    }
}

@Composable
private fun FiltersAndTable(
    screenState: OverviewUiState,
    viewModel: OverViewModel,
    navigateToManageItemScreen: (String) -> Unit,
    screenSize: ScreenSize,
    loading: Boolean,
) {
    Column {
        var expanded by rememberSaveable {
            mutableStateOf(false)
        }

        Row(
            modifier = Modifier
                .padding(start = 16.dp, bottom = 8.dp, end = 4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = {
                    expanded = !expanded
                }
            ) {
                val filterText = if (expanded) {
                    "Použít filtry"
                } else {
                    "Zobrazit filtry"
                }

                Text(
                    text = "$filterText (${screenState.selectedFilters.getNumberOfActiveFilters()})",
                )
                val icon = if (expanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                }

                Icon(
                    imageVector = icon,
                    contentDescription = "Zobrazit více",
                    modifier = Modifier.size(32.dp) // Adjust icon size
                )
            }

            if (loading && screenState.selectedFilters.getNumberOfActiveFilters() == 0)
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp)
                )

            if (screenState.selectedFilters.getNumberOfActiveFilters() > 0) {
                TextButton(
                    onClick = { viewModel.onFilterClear() },
                ) {
                    Text(text = "Resetovat všechny")
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Resetovat filtry"
                    )
                }
            }

        }

        if (expanded) {
            FilterMobileSlots(
                allFilters = screenState.allFilters,
                selectedFilters = screenState.selectedFilters,
                onQualityFilterChange = { viewModel.onQualityFilterChange(it) },
                onThicknessFilterChange = { viewModel.onThicknessFilterChange(it) },
                onWidthFilterChange = { viewModel.onWidthFilterChange(it) },
                onLengthFilterChange = { viewModel.onLengthFilterChange(it) },
            )
        }
        AllSlotsContent(
            slots = screenState.sortedSlots,
            slotSum = screenState.sum,
            sortingBy = screenState.sortingBy,
            sortingDirection = screenState.sortingDirection,
            updateSorting = { viewModel.updateSorting(it) },
            navigateToManageItemScreen = navigateToManageItemScreen,
            screenSize = screenSize,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewTopBar(
    navigateToStart: () -> Unit,
    screenSize: ScreenSize,
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    coroutineScope: CoroutineScope
) {
    TopAppBar(
        title = {
            TabHeader(
                pagerState = pagerState,
                coroutineScope = coroutineScope,
            )
        },
        navigationIcon = {
            if (screenSize.isPhone()) {
                IconButton(onClick = {
                    navigateToStart()
                }) {
                    Icon(
                        Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back_icon)
                    )
                }
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "ikona aplikace",
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(48.dp),
                    tint = Color.Unspecified
                )
            }
        },
        actions = {
            if (screenSize.isTablet()) {
                Button(
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = {
                        navigateToStart()
                    },
                ) {
                    Text("Skenování a hranolky s posledními pohyby")
                    Icon(
                        Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = stringResource(R.string.forward_icon)
                    )
                }
            }
        },
        modifier = modifier
    )
}
