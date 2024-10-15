package eu.jelinek.hranolky.ui.showlast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import org.koin.androidx.compose.koinViewModel

@Composable
fun ShowLastActionsScreen(
    modifier: Modifier = Modifier,
    navigateUp: () -> Unit,
    navigateToAddAction: () -> Unit,
    navBackStackEntry: NavBackStackEntry,
    viewModel: ShowLastActionsViewModel = koinViewModel()
    ) {
    val slotId = viewModel.slotId

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        Text("Slot: $slotId")

        Button(
            onClick = { viewModel.fetchLastActionsFromFirestore() }
        ) {
            Text("Fetch Last Actions")
        }

        Button(
            onClick = { viewModel.sendNewSlotToFirestore() }
        ) {
            Text("Send Test Data")
        }
    }
}