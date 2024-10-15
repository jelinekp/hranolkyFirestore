package eu.jelinek.hranolky.ui.showlast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun ShowLastActionsScreen(
    modifier: Modifier = Modifier,
    navigateUp: () -> Unit,
    navigateToAddAction: () -> Unit,
    navBackStackEntry: NavBackStackEntry,
    viewModel: ShowLastActionsViewModel = koinViewModel()
    ) {
    val slotId = viewModel.slotId
    val screenState by viewModel.screenStateStream.collectAsState()
    var isShown by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        Text("Slot: $slotId")

        Button(
            onClick = { viewModel.sendNewSlotToFirestore() }
        ) {
            Text("Send Test Data")
        }

        if (screenState.slot == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary, // Custom color
                strokeWidth = 4.dp, // Custom stroke width
            )
        } else {
            Text(
                text = "Slot: ${screenState.slot!!.productId}"
            )
        }
    }
}