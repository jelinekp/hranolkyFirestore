package eu.jelinek.hranolky.ui.showlast

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.model.SlotAction
import eu.jelinek.hranolky.model.WarehouseSlot
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShowLastActionsScreen(
    modifier: Modifier = Modifier,
    navigateUp: () -> Unit,
    navigateToAddAction: () -> Unit,
    navBackStackEntry: NavBackStackEntry,
    viewModel: ShowLastActionsViewModel = koinViewModel()
) {
    val slotId = viewModel.slotId
    val screenState by viewModel.screenStateStream.collectAsStateWithLifecycle()

    Scaffold (
        topBar = { ShowLastActionsTopBar(
            text = "$slotId",
            navigateUp = navigateUp,
        ) }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
fun SlotData(
    slot : WarehouseSlot,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(260.dp),
    ) {
        val quantity = slot.quantity
        val width = slot.width ?: 1
        val length = slot.length ?: 1
        val thickness = slot.thickness ?: 1.0f
        val volume = ((quantity * width * length).toFloat() * thickness)/1_000_000_000f
        val formattedVolume = String.format("%.3f", volume)

        val alternateRowModifier =
            Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)
        DataRow("Množství na skladě", quantity.toString(), alternateRowModifier)
        DataRow("Kvalita", slot.quality.toString())
        DataRow("Tloušťka", "$thickness mm", alternateRowModifier)
        DataRow("Šířka", "$width mm")
        DataRow("Délka", "$length mm", alternateRowModifier)
        DataRow("Celkový objem", "$formattedVolume m³")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LastActions(
    lastActions: List<SlotAction>,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Poslední akce",
        style = MaterialTheme.typography.headlineSmall,
    )
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 100.dp),
    ) {
        stickyHeader { // Makes the header sticky
            HeaderRowContent() // Your header row composable function
        }
        items(lastActions) { slotAction ->
            LastActionRow(slotAction)
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
            Row (
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

@Composable
fun DataRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value ?: "neznámá hodnota",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun HeaderRowContent() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 4.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Datum a čas",
            modifier = Modifier.weight(8f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Pohyb",
            modifier = Modifier.weight(3f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Změna",
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Stav",
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

@Composable
fun LastActionRow(
    slotAction: SlotAction,
    modifier: Modifier = Modifier
) {
    val action = when (slotAction.action) {
        "prijem" -> "Příjem"
        "vydej" -> "Výdej"
        else -> "Error"
    }
    val date = slotAction.timestamp?.toDate() ?: Date()
    val readableDate = formatDate(date)

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Text(readableDate, modifier = Modifier.weight(8f), style = MaterialTheme.typography.bodySmall)
        Text(action, modifier = Modifier.weight(3f), style = MaterialTheme.typography.bodySmall)
        Text(
            text = slotAction.quantityChange.toString(),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = slotAction.newQuantity.toString(),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun AddAction(
    viewModel: ShowLastActionsViewModel,
    modifier: Modifier = Modifier
) {
    val quantityFocusRequester = remember { FocusRequester() }
    val submitFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val validationFlow by viewModel.validationSharedFlowStream.collectAsStateWithLifecycle(
        initialValue = AddActionValidationState()
    )

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = "Přidat pohyb",
            style = MaterialTheme.typography.headlineSmall,
            modifier = modifier
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            RadioButton(
                selected = viewModel.radioState.value == "prijem",
                onClick = {
                    viewModel.onRadioSelected("prijem")
                    quantityFocusRequester.requestFocus()
                    keyboardController?.show()
                }
            )
            Text(
                text = stringResource(R.string.p_jem),
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable(
                        onClick = {
                            viewModel.onRadioSelected("prijem")
                            quantityFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    ))
            Spacer(modifier = Modifier.width(48.dp))
            RadioButton(
                selected = viewModel.radioState.value == "vydej",
                onClick = {
                    viewModel.onRadioSelected("vydej")
                    quantityFocusRequester.requestFocus()
                    keyboardController?.show()
                }
            )
            Text(
                text = stringResource(R.string.v_dej),
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable(
                        onClick = {
                            viewModel.onRadioSelected("vydej")
                            quantityFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    )
            )
        }
        if (validationFlow.isRadioError) {
            ErrorText(text = stringResource(R.string.vyber_akci))
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.width(300.dp),
        ) {
            OutlinedTextField(
                value = viewModel.quantityState.value,
                onValueChange = { viewModel.onQuantityChanged(it) },
                label = { Text(stringResource(R.string.zadej_mno_stv)) },
                isError = validationFlow.isQuantityError,
                keyboardActions = KeyboardActions(onDone = {
                    quantityFocusRequester.freeFocus()
                    keyboardController?.hide()
                }),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                modifier = Modifier
                    .focusRequester(quantityFocusRequester)
                    .weight(6f),
                singleLine = true,
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.addActionToTheSlot()
                },
                modifier = Modifier
                    .weight(5f)
                    .focusRequester(submitFocusRequester)
                    .height(50.dp)
                    .padding(bottom = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.odeslat),
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Send,
                    contentDescription = "Ikona odeslat",
                    modifier = Modifier
                        .padding(0.dp)
                        .size(40.dp)
                )
            }
        }

        // If there's an error, display the error message
        if (validationFlow.isQuantityError) {
            ErrorText(text = stringResource(R.string.zadej_platn_mno_stv))
        }

        if (validationFlow.isRemovedError) {
            ErrorText(text = stringResource(R.string.vydej_nemuze_byt_vetsi_nez_aktualni_stav))
        }
    }
}

@Composable
fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text, // This holds the error message
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.padding(top = 4.dp)
    )
}