package eu.jelinek.hranolky.ui.manageitem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.ui.shared.ScreenSize
import org.koin.androidx.compose.koinViewModel

@Composable
fun ManageItemScreen(
    modifier: Modifier = Modifier,
    navigateUp: () -> Unit,
    screenSize: ScreenSize,
    viewModel: ManageItemViewModel = koinViewModel()
) {
    val slotId = viewModel.slotId
    val screenState by viewModel.screenStateStream.collectAsStateWithLifecycle()
    val quantity = viewModel.quantityState.value
    val validationState by viewModel.validationSharedFlowStream.collectAsStateWithLifecycle(
        initialValue = AddActionValidationState()
    )

    val header = if (slotId?.first() == 'S')
        "Spárovka"
    else
        "Hranolek"

    if (screenState.isInventoryCheckEnabled)
        ConfirmSettingPopup(
            modifier = Modifier,
            dialogTitle = "Nastavit množství na $quantity?",
            dialogMessage = "To je o ${screenState.inventoryCheckPopupMessage.diff} " +
                    "${screenState.inventoryCheckPopupMessage.compareString} " +
                    "než je současné množství.",
            showYesNoDialog = screenState.showConfirmSettingPopup,
            onDismissYesNoDialog = viewModel::onDismissSettingPopup,
            onConfirmYesNoDialog = viewModel::onConfirmSettingPopup
        )

    Scaffold(
        topBar = {
            ShowLastActionsTopBar(
                text = "$header $slotId",
                navigateUp = navigateUp,
            )
        }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .padding(padding)
                .fillMaxWidth()
        ) {
            when (screenState.resultStatus) {
                ResultStatus.LOADING -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary, // Custom color
                        strokeWidth = 4.dp, // Custom stroke width
                    )
                }

                ResultStatus.SUCCESS -> {
                    if (screenState.slot != null) {
                        val slot = screenState.slot!!

                        if (screenSize.isPhone()) {
                            ShowOffline(screenState.isOnline)
                            SlotData(slot = slot)
                            if (screenState.isInventoryCheckEnabled) {
                                InventoryCheck(
                                    quantity = quantity,
                                    onQuantityChanged = viewModel::onQuantityChanged, // Or { newValue -> viewModel.onQuantityChanged(newValue) }
                                    onSetClicked = viewModel::showSettingPopup,
                                    validationState = validationState,
                                )
                            } else {
                                AddAction(
                                    quantity = quantity,
                                    onQuantityChanged = viewModel::onQuantityChanged, // Or { newValue -> viewModel.onQuantityChanged(newValue) }
                                    validationState = validationState,
                                    onAddActionClick = { actionType ->
                                        viewModel.addActionToTheSlot(actionType)
                                    },
                                )
                            }
                            LastActions(slot.slotActions)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                LastActions(slot.slotActions, modifier = Modifier
                                    .weight(1f)
                                    .padding(all = 16.dp))
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(all = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    ShowOffline(screenState.isOnline)
                                    SlotData(slot = slot, modifier = Modifier
                                        .weight(5f)
                                        .padding(horizontal = 64.dp), forceExpanded = true, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    AddAction(quantity = quantity,
                                        onQuantityChanged = viewModel::onQuantityChanged, // Or { newValue -> viewModel.onQuantityChanged(newValue) }
                                        validationState = validationState,
                                        onAddActionClick = { actionType ->
                                            viewModel.addActionToTheSlot(actionType)
                                        }, modifier = Modifier.weight(10f))
                                }
                            }
                        }
                    }
                }

                ResultStatus.DATA_ERROR -> {
                    Text(
                        "Nesprávný formát dat, položka musí být ve formátu\n\"S/H-AAA-A-XX-XX(XX)-XXXX\"\n(18-20 znaků, A jsou písmena, X jsou číslice)",
                        modifier = Modifier.padding(32.dp),
                        textAlign = TextAlign.Center,
                    )
                }

                ResultStatus.NETWORK_ERROR -> {
                    Text("Chyba sítě", modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center,)
                }

                ResultStatus.OTHER_ERROR -> {
                    Text(
                        "Neznámá chyba, kontaktuj Pavla Jelínka",
                        modifier = Modifier.padding(32.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun ShowOffline(isOnline: Boolean, modifier: Modifier = Modifier, ) {
    if (!isOnline) {
        Text(
            "Připojte se prosím k internetu",
            modifier = modifier.padding(8.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
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