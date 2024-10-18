package eu.jelinek.hranolky.ui.showlast

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.model.SlotAction
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.ui.shared.formatDate
import eu.jelinek.hranolky.ui.shared.slotActionsIndexedWithAlternatingModifier
import org.koin.androidx.compose.koinViewModel
import java.util.Date

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

@Composable
fun SlotData(
    slot: WarehouseSlot,
    modifier: Modifier = Modifier
) {
    var isShowMore by rememberSaveable {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.widthIn(max = 280.dp),
        ) {


            val quantity = slot.quantity
            val width = slot.width ?: 1.0f
            val length = slot.length ?: 1
            val thickness = slot.thickness ?: 1.0f
            val volume = ((quantity * length).toFloat() * thickness * width) / 1_000_000_000f
            val formattedVolume = String.format("%.3f", volume)

            val alternateRowModifier =
                Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)

            DataRow("Množství na skladě", quantity.toString(), alternateRowModifier)
            DataRow("Objem dřeva", "$formattedVolume m³")
            if (isShowMore) {
                DataRow("Kvalita", slot.quality.toString(), alternateRowModifier)
                DataRow("Tloušťka", "$thickness mm")
                DataRow("Šířka", "$width mm", alternateRowModifier)
                DataRow("Délka", "$length mm")
            }
        }

        IconButton(
            onClick = { isShowMore = !isShowMore },
            modifier = Modifier.size(48.dp),
        ) {
            val icon = if (isShowMore) {
                Icons.Default.KeyboardArrowUp
            } else {
                Icons.Default.KeyboardArrowDown
            }

            Icon(
                imageVector = icon,
                contentDescription = "Zobrazit více",
                modifier = Modifier.size(40.dp) // Adjust icon size
            )
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LastActions(
    lastActions: List<SlotAction>,
    modifier: Modifier = Modifier
) {

    val alternateRowModifier =
        Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)

    Text(
        text = "Poslední akce",
        style = MaterialTheme.typography.headlineSmall,
    )
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(max = 550.dp)
            .fillMaxWidth()
            .padding(
                // horizontal = 16.dp
            )
            .heightIn(min = 100.dp),
    ) {
        stickyHeader { // Makes the header sticky
            HeaderRowContent() // Your header row composable function
        }
        slotActionsIndexedWithAlternatingModifier(lastActions, alternateRowModifier) { index, slotAction, modifier ->
            LastActionRow(slotAction, modifier)
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
            .padding(
                horizontal = 8.dp,
                vertical = 2.dp,
            ),
    ) {
        Text(
            text = "$label: ",
            // style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value ?: "neznámá hodnota",
            // style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun HeaderRowContent() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(
                vertical = 4.dp,
                horizontal = 8.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Datum a čas",
            modifier = Modifier.weight(8f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Pohyb",
            modifier = Modifier.weight(3f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Změna",
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Stav",
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            //style = MaterialTheme.typography.bodySmall
        )
    }
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
            .padding(
                horizontal = 8.dp,
                vertical = 2.dp
            ),
    ) {
        Text(
            readableDate,
            modifier = Modifier.weight(8f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            action,
            modifier = Modifier.weight(3f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = slotAction.quantityChange.toString(),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = slotAction.newQuantity.toString(),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            //style = MaterialTheme.typography.bodySmall
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
        modifier = Modifier.widthIn(max = 340.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Přidat pohyb:",
                // style = MaterialTheme.typography.headlineSmall,
                modifier = modifier
            )
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(
                value = viewModel.quantityState.value,
                onValueChange = { viewModel.onQuantityChanged(it) },
                label = { Text(
                    stringResource(R.string.zadej_mno_stv),
                    //style = MaterialTheme.typography.bodySmall
                    ) },
                isError = validationFlow.isQuantityError,
                keyboardActions = KeyboardActions(onDone = {
                    quantityFocusRequester.freeFocus()
                    keyboardController?.hide()
                }),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                modifier = Modifier
                    .widthIn(max = 120.dp)
                    .focusRequester(quantityFocusRequester)
                    .weight(6f),
                singleLine = true,
            )
        }

        if (validationFlow.isQuantityError) {
            ErrorText(text = stringResource(R.string.zadej_platn_mno_stv))
        }

        if (validationFlow.isRemovedError) {
            ErrorText(text = stringResource(R.string.vydej_nemuze_byt_vetsi_nez_aktualni_stav))
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.width(280.dp),
        ) {
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.addActionToTheSlot(ActionType.ADD)
                },
                modifier = Modifier
                    .weight(5f)
                    .focusRequester(submitFocusRequester)
                    .height(50.dp)
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.prijem),
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ikona odeslat",
                    modifier = Modifier
                        .padding(0.dp)
                        .size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(32.dp))
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.addActionToTheSlot(ActionType.REMOVE)
                },
                modifier = Modifier
                    .weight(5f)
                    .focusRequester(submitFocusRequester)
                    .height(50.dp)
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.vydej),
                    fontSize = 16.sp,
                )
            }
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