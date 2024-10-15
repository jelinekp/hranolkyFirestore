package eu.jelinek.hranolky.ui.showlast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import eu.jelinek.hranolky.R
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

    Scaffold (
        topBar = { ShowLastActionsTopBar(
            text = "$slotId",
            navigateUp = navigateUp,
        ) }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.padding(padding).fillMaxWidth()
        ) {
            if (screenState.slot == null) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary, // Custom color
                    strokeWidth = 4.dp, // Custom stroke width
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = modifier.width(260.dp),
                ) {
                    val alternateRowModifier =
                        Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)
                    DataRow(
                        "Množství na skladě",
                        screenState.slot!!.quantity.toString(),
                        alternateRowModifier
                    )
                    DataRow("Kvalita", screenState.slot!!.quality.toString())
                    DataRow(
                        "Tloušťka",
                        screenState.slot!!.thickness.toString() + " mm",
                        alternateRowModifier
                    )
                    DataRow("Šířka", screenState.slot!!.width.toString() + " mm")
                    DataRow("Délka", screenState.slot!!.length.toString() + " mm", alternateRowModifier)
                }

                AddAction(
                    viewModel = viewModel
                )

                Text(
                    text = "Poslední akce",
                    style = MaterialTheme.typography.headlineSmall,
                )
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
            Row (
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(end = 48.dp).fillMaxWidth()
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
        )
        Text(text = value ?: "neznámá hodnota")
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
            .padding(all = 16.dp),
    ) {
        Text(
            text = "Přidat akci",
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
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(300.dp),
            horizontalArrangement = Arrangement.Center,
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
                modifier = Modifier.focusRequester(quantityFocusRequester).weight(6f),
                singleLine = true,
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.addActionToTheSlot()
                },
                modifier = Modifier.weight(4f)
                    .focusRequester(submitFocusRequester).padding(0.dp).height(40.dp),
            ) {
                Text(text = stringResource(R.string.odeslat))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Send,
                    contentDescription = "Ikona odeslat",
                )
            }
        }

        // If there's an error, display the error message
        if (validationFlow.isQuantityError) {
            ErrorText(text = stringResource(R.string.zadej_platn_mno_stv))
        }
    }
}

@Composable
fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text, // This holds the error message
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}