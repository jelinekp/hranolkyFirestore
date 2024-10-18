package eu.jelinek.hranolky.ui.showlast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.jelinek.hranolky.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun ShowLastActionsScreen(
    modifier: Modifier = Modifier,
    navigateUp: () -> Unit,
    viewModel: ShowLastActionsViewModel = koinViewModel()
) {
    val slotId = viewModel.slotId
    val screenState by viewModel.screenStateStream.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ShowLastActionsTopBar(
                text = "$slotId",
                navigateUp = navigateUp,
            )
        }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                // 16.dp for bigger screens, 8.dp for smaller screens
                8.dp
            ),
            modifier = modifier
                .padding(padding)
                .fillMaxWidth()
        ) {
            if (screenState.slot == null) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary, // Custom color
                    strokeWidth = 4.dp, // Custom stroke width
                )
            } else {
                val slot = screenState.slot!!
                SlotData(slot = slot)
                AddAction(viewModel = viewModel)
                LastActions(slot.slotActions)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowLastActionsTopBar(
    text: String?,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(end = 48.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = text ?: "Neznámý kód",
                )
            }
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