package eu.jelinek.hranolky.ui.showlast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val screenState by viewModel.screenStateStream.collectAsState()
    var isShown by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            "Slot: $slotId",
            style = MaterialTheme.typography.headlineSmall,
        )

        if (screenState.slot == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary, // Custom color
                strokeWidth = 4.dp, // Custom stroke width
            )
        } else {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = modifier.width(250.dp),
            ) {
                DataRow("Množství na skladě", screenState.slot!!.quantity.toString())
                DataRow("Kvalita", screenState.slot!!.quality.toString())
                DataRow("Tloušťka", screenState.slot!!.thickness.toString())
                DataRow("Šířka", screenState.slot!!.width.toString())
                DataRow("Délka", screenState.slot!!.length.toString())
            }
        }
    }
}

@Composable
fun DataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row (
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "$label: ",
        )
        Text(text = value)
    }
}