package eu.jelinek.hranolky.ui.history

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import eu.jelinek.hranolky.ui.shared.ScreenSize
import org.koin.androidx.compose.koinViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    navigateUp: () -> Unit,
    navigateToManageItem: (String) -> Unit,
    viewModel: HistoryViewModel = koinViewModel(),
    screenSize: ScreenSize = ScreenSize.PHONE
) {
    val screenState by viewModel.historyScreenState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {HistoryTopBar(
            navigateUp,
            screenSize
        )},
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            SlotTable(
                screenState.lastModifiedBeamSlots,
                screenState.lastModifiedJointerSlots,
                screenSize = screenSize,
                navigateToManageItem = navigateToManageItem,
                onLoadJointerSlots = { viewModel.loadJointerLastModifiedSlots() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTopBar(
    navigateUp: () -> Unit,
    screenSize: ScreenSize,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = "Historie pohybů"
            )
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
        actions = {

        },
        modifier = modifier
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TabletSlotTable(
    navigateToManageItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val screenState by viewModel.historyScreenState.collectAsStateWithLifecycle()
    SlotTable(
        screenState.lastModifiedBeamSlots,
        screenState.lastModifiedJointerSlots,
        screenSize = ScreenSize.TABLET,
        navigateToManageItem = navigateToManageItem,
        onLoadJointerSlots = { viewModel.loadJointerLastModifiedSlots() },
    )
}